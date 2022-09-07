package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.Address
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class TransferEventTest extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  private object Generators {

    def genHexString(length: Int): Gen[String] =
      for {
        low       <- Gen.choose[Char]('a', 'f')
        upper     <- Gen.choose[Char]('A', 'F')
        number    <- Gen.choose[Char]('0', '9')
        hexString <- Gen.listOfN(length, Gen.oneOf(low, upper, number)).map(_.mkString)
      } yield hexString

    def genAddress: Gen[String] = genHexString(40).map(_.toLowerCase)

    def genEncodedData: Gen[String] =
      for {
        data <- genHexString(64)
      } yield s"0x$data"

    def genEthHash: Gen[String] = genHexString(64)

    def genLoggedEvent: Gen[LoggedEvent] =
      for {
        address   <- genAddress
        block     <- Gen.choose(1, 2000000)
        topics    <- Gen.listOfN(3, genEncodedData)
        data      <- genEncodedData
        txHash    <- genEthHash
        txIndex   <- Gen.choose(0, 100)
        blockHash <- genEthHash
      } yield
        LoggedEvent(
          Address(address),
          Block(block),
          topics,
          data,
          EthereumHash(txHash),
          txIndex,
          EthereumHash(blockHash)
        )

    def genToken: Gen[Erc20Token] =
      for {
        symbol   <- Gen.listOfN(3, Gen.alphaChar).map(_.mkString)
        erc_type <- Gen.listOfN(6, Gen.alphaChar).map(_.mkString)
        name     <- Gen.alphaStr
        decimals <- Gen.choose(0, 18)
        contract <- genAddress
      } yield Erc20Token(symbol, erc_type, name, decimals, Address(contract), None, None)

  }

  import Generators._

  behavior of "TransferEvent"

  it should "not create a TransferEvent from a LoggedEvent not representing a ERC20 transfer" in {
    forAll(genLoggedEvent, genToken) { (event, token) =>
      TransferEvent.fromLoggedEvent(event,
                                    Ethtoken(token.symbol,
                                             token.erc_type,
                                             token.name,
                                             token.contract,
                                             token.owner,
                                             token.birthBlock),
                                    DateTime.now) shouldBe None
    }
  }

  it should "not create a TransferEvent when the event origin is not the token contract address" in {
    forAll(genLoggedEvent, genToken) { (event, token) =>
      val transferEvent = event.copy(topics = erc20TransferEventSignature :: event.topics.tail)
      TransferEvent.fromLoggedEvent(transferEvent,
                                    Ethtoken(token.symbol,
                                             token.erc_type,
                                             token.name,
                                             token.contract,
                                             token.owner,
                                             token.birthBlock),
                                    DateTime.now) shouldBe None
    }
  }

  it should "create a TransferEvents that fulfills all restrictions" in {
    forAll(genLoggedEvent, genToken) { (event, token) =>
      val transferEvent = event.copy(topics = erc20TransferEventSignature :: event.topics.tail,
                                     origin = token.contract)
      TransferEvent
        .fromLoggedEvent(transferEvent,
                         Ethtoken(token.symbol,
                                  token.erc_type,
                                  token.name,
                                  token.contract,
                                  token.owner,
                                  token.birthBlock),
                         DateTime.now)
        .get
        .txHash shouldBe event.txHash
    }
  }
}
