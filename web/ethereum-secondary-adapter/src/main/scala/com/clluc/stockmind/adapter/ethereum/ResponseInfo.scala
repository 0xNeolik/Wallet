package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import io.circe.Json
import io.circe.optics.JsonPath.root
import io.circe.parser.parse
import cats.syntax.either._
import com.clluc.stockmind.core.ethereum.JsonRpcResponse._
import com.clluc.stockmind.core.ethereum._
import com.typesafe.scalalogging.LazyLogging

case class ResponseInfo(body: String, status: Int) extends LazyLogging {

  lazy val leftValue = {
    logger.error(s"Problematic response with HTTP code $status: $body")
    Left(UnexpectedEthereumResponse(body, status))
  }

  private def checkCond[T](cond: => Boolean)(
      ethResponse: => EthereumResponse[T]): EthereumResponse[T] = {
    if (cond) ethResponse else leftValue
  }

  private lazy val checkOkJson: EthereumResponse[Json] = {
    lazy val jsonResponse = parse(body).leftMap(_ => leftValue.value)
    val is2xx             = status >= 200 && status < 300
    checkCond(is2xx)(jsonResponse)
  }

  lazy val result: JsonRpcPlainResult = {

    def _getResult(json: Json): EthereumResponse[String] = {
      val results = json.findAllByKey("result")
      checkCond(results.size == 1) {
        Either.fromOption(results.head.asString, leftValue.value)
      }
    }

    checkOkJson.flatMap(_getResult)
  }

  lazy val address: JsonRpcAddress = result.map(Address.decode)

  lazy val balance: JsonRpcBalance =
    result.map(result => BigInt(result.replace("0x", ""), 16))

  lazy val transactions: EthereumResponse[Option[List[Transaction]]] = {

    def _getTransactions(json: Json): EthereumResponse[Option[List[Transaction]]] = {
      val transactionsLens = root.result.transactions.arr
      val hashLens         = root.hash.string
      val fromLens         = root.from.string
      val toLens           = root.to.string
      val blockNumberLens  = root.blockNumber.string
      val txIndexLens      = root.transactionIndex.string
      val valueLens        = root.value.string

      val transactions = transactionsLens.getOption(json).map {
        _.toList.map { transaction =>
          Transaction(
            hash =
              hashLens.getOption(transaction).map(EthereumHash.decodePrefixedHexString(_).get).get,
            from = fromLens.getOption(transaction).map(Address.decode).get,
            to = toLens.getOption(transaction).map(Address.decode),
            blockNumber = blockNumberLens.getOption(transaction).map(Block.fromHexString(_).get).get,
            txIndex =
              txIndexLens.getOption(transaction).map(i => Integer.parseInt(i.substring(2), 16)).get,
            value = valueLens.getOption(transaction).map(x => Uint.decode(256, x.substring(2))).get
          )
        }
      }

      Right(transactions)
    }

    checkOkJson.flatMap(_getTransactions)
  }

  lazy val events: JsonRpcLoggedEvents = {

    def _getEvents(json: Json): EthereumResponse[List[LoggedEvent]] = {
      val results         = root.result.each
      val topicsLens      = results.topics.arr
      val dataLens        = results.data.string
      val addressLens     = results.address.string
      val blockNumberLens = results.blockNumber.string
      val blockHashLens   = results.blockHash.string
      val txHashLens      = results.transactionHash.string
      val txIndexLens     = results.transactionIndex.string

      val topics = topicsLens
        .getAll(json)
        .map(_.map(_.noSpaces.drop(1).dropRight(1)).toList)
      val datum     = dataLens.getAll(json)
      val addresses = addressLens.getAll(json).map(Address.decode)
      val blocks    = blockNumberLens.getAll(json).map(Block.fromHexString).map(_.get)
      val txHashes  = txHashLens.getAll(json).map(EthereumHash.decodePrefixedHexString).map(_.get)
      val blockHashes =
        blockHashLens.getAll(json).map(EthereumHash.decodePrefixedHexString).map(_.get)
      val txIndexes = txIndexLens.getAll(json).map(_.drop(2)).map(Integer.parseInt(_, 16))

      val lengthCheck = List(topics, datum, addresses, blocks, txHashes, txIndexes)
        .forall(l => l.length == topics.length)

      checkCond(lengthCheck) {
        val events = topics.indices.toList.map { i =>
          LoggedEvent(addresses(i),
                      blocks(i),
                      topics(i),
                      datum(i),
                      txHashes(i),
                      txIndexes(i),
                      blockHashes(i))
        }
        Right(events)
      }
    }

    checkOkJson.flatMap(_getEvents)
  }
}
