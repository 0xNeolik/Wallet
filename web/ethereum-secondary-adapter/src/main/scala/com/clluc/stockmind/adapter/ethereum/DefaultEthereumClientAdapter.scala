package com.clluc.stockmind.adapter.ethereum

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.typesafe.scalalogging.LazyLogging
import io.circe.{Json, Printer}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.shapes._
import play.api.http.ContentTypes._
import play.api.http.HeaderNames._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.{
  JsonRpcBalance,
  JsonRpcLoggedEvents,
  JsonRpcPlainResult
}
import com.clluc.stockmind.core.ethereum.{
  Block,
  EthCall,
  EthCallFrom,
  EthFilter,
  SignableTransaction
}
import com.clluc.stockmind.port.secondary.EthereumClientPort

private[ethereum] class DefaultEthereumClientAdapter(url: String)(implicit val ec: ExecutionContext)
    extends EthereumClientPort
    with LazyLogging {

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(system))

  private val ws = AhcWSClient()

  private def post(bodyAsJson: Json): Future[WSResponse] = {
    // Use a custom printer that drops empty and null fields. This allows
    // `Foo(a = Some(1), b = None)` to be encoded as `{"a"=1}`.
    val body = bodyAsJson.pretty(Printer(preserveOrder = true, dropNullKeys = true, indent = ""))
    logger.debug(s"Sending to eth node: $body")
    ws.url(url)
      .withHttpHeaders(
        ACCEPT       -> JSON,
        CONTENT_TYPE -> JSON
      )
      .post(body)
  }

  private def into[T](f: ResponseInfo => T)(resp: WSResponse): T = {
    val responseInfo = ResponseInfo(resp.body, resp.status)
    logger.debug(s"Received from eth node: $responseInfo")
    f(responseInfo)
  }

  override def createAccountWithPassword(password: String): Future[JsonRpcPlainResult] =
    post(JsonRpc.newAccount(password).asJson)
      .map(into(_.result))

  override def findBalanceForAccount(address: Address): Future[JsonRpcBalance] =
    post(JsonRpc.getBalance(address.toHex).asJson)
      .map(into(_.balance))

  override def findEthereumBlockNumber(): Future[JsonRpcPlainResult] =
    post(JsonRpc.blockNumber().asJson)
      .map(into(_.result))

  override def signMessage(account: Address, message: String): Future[JsonRpcPlainResult] =
    post(JsonRpc.signMessage(account.value, message).asJson)
      .map(into(_.result))

  override def sendTransaction(signableTx: SignableTransaction): Future[JsonRpcPlainResult] =
    post(JsonRpc.sendTransaction(signableTx).asJson)
      .map(into(_.result))

  override def callMethod(call: EthCall): Future[JsonRpcPlainResult] =
    post(JsonRpc.callMethod(call).asJson)
      .map(into(_.result))

  override def callMethodFrom(call: EthCallFrom): Future[JsonRpcPlainResult] =
    post(JsonRpc.callMethodFrom(call).asJson)
      .map(into(_.result))

  override def getLoggedEvents(filter: EthFilter): Future[JsonRpcLoggedEvents] =
    post(JsonRpc.getLogs(filter).asJson)
      .map(into(_.events))

  override def findBlockTransactionsByNumber(blockNumber: Block) =
    post(JsonRpc.getBlockByNumber(blockNumber.toHex).asJson)
      .map(into(_.transactions))

  override val defaultRightStatusCode = 200
}

object DefaultEthereumClientAdapter {

  def apply(url: String)(implicit ec: ExecutionContext): EthereumClientPort =
    new DefaultEthereumClientAdapter(url)
}
