package com.clluc.stockmind.core.actor

import java.sql.SQLException

import akka.actor.ActorSystem
import com.clluc.stockmind.core.Generators
import com.clluc.stockmind.core.actor.EventProcessorOps.ErrorConstructors
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.{
  JsonRpcPlainResult,
  UnexpectedEthereumResponse
}
import com.clluc.stockmind.core.ethereum.{Erc721Token, EthereumHash, Ethtoken}
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.transaction.{InboundTransfer, OutboundTransfer}
import com.clluc.stockmind.port.secondary._
import org.joda.time.DateTime
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}

import scala.concurrent.Future

class EventProcessorOpsImplTest
    extends AsyncFlatSpec
    with Matchers
    with EitherValues
    with AsyncMockFactory {

  val errors = ErrorConstructors

  // Fixture traits don't work with async tests.

  val infoPort             = mock[Erc20InfoPort]
  val erc721InfoPort       = mock[Erc721InfoPort]
  val transferEventPort    = mock[Erc20TransferEventPort]
  val inboundTransferPort  = mock[InboundTransferPort]
  val outboundTransferPort = mock[OutboundTransferPort]
  val offchainTransferPort = mock[OffChainTransferPort]
  val ethAccountPort       = mock[EthereumAccountPort]
  val ethClientPort        = mock[EthereumClientPort]
  val master               = Address.default
  val supplier             = Address.default
  val actorSystem          = mock[ActorSystem]

  val ops = new EventProcessorOpsImpl(
    infoPort,
    erc721InfoPort,
    transferEventPort,
    inboundTransferPort,
    offchainTransferPort,
    outboundTransferPort,
    ethAccountPort,
    ethClientPort,
    master,
    supplier,
    actorSystem
  )

  val dbFailure = Future.failed(new SQLException("db fail"))

  behavior of "findEthereumERC20TokenByAddress"

  it should "return a token given its address" in {
    val address  = Address.decode(Generators.genAddress.sample.get)
    val ethtoken = Ethtoken("QWE", "ERC-20", "qwerty", address, None, None)
    val tokenF   = Future.successful(Some(ethtoken))

    (infoPort.findEthereumTokenByAddress _).expects(address).returning(tokenF)

    ops.findEthereumTokenByAddress(address).value.map { result =>
      result.right.value shouldBe ethtoken
    }
  }

  it should "fail with the appropriate error if no erc-20 token is found" in {
    val address = Address.decode(Generators.genAddress.sample.get)
    val tokenF  = Future.successful(None)
    val error   = errors.addressNotFromAnyTokenContract(address.toHex)

    (infoPort.findEthereumTokenByAddress _).expects(address).returning(tokenF)

    ops.findEthereumTokenByAddress(address).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "findEthereumERC721TokenByAddress"

  it should "return a token given its address" in {
    val address = Address.decode(Generators.genAddress.sample.get)
    val token   = Erc721Token("QWE", "ERC-721", "qwerty", "metda", BigInt(1), address, None, None)
    val tokenF  = Future.successful(Some(token))

    (erc721InfoPort.findEthereumTokenByAddress _).expects(address).returning(tokenF)

    ops.findEthereum721TokenByAddress(address).value.map { result =>
      result.right.value shouldBe token
    }
  }

  it should "fail with the appropriate error if no erc-721 token is found" in {
    val address = Address.decode(Generators.genAddress.sample.get)
    val tokenF  = Future.successful(None)
    val error   = errors.addressNotFromAnyTokenContract(address.toHex)

    (erc721InfoPort.findEthereumTokenByAddress _).expects(address).returning(tokenF)

    ops.findEthereum721TokenByAddress(address).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "createTransferEvent"

  it should "return the stored transfer event" in {
    val event  = Fixtures.sampleTransferEvent
    val eventF = Future.successful(event)

    (transferEventPort.createTransferEvent _).expects(event).returning(eventF)

    ops.storeTransferEvent(event).value.map { result =>
      result.right.value shouldBe event
    }
  }

  it should "fail with the appropriate error if the event cannot be stored" in {
    val event = Fixtures.sampleTransferEvent
    val error = errors.ioError("db fail")

    (transferEventPort.createTransferEvent _).expects(event).returning(dbFailure)

    ops.storeTransferEvent(event).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "createInboundTransfer"

  it should "return the stored inbound transfer" in {
    val txHash    = EthereumHash(Generators.genEthHash.sample.get)
    val transfer  = InboundTransfer(0L, txHash)
    val transferF = Future.successful(transfer)

    (inboundTransferPort.create _).expects(transfer).returning(transferF)

    ops.storeInboundTransfer(transfer).value.map { result =>
      result.right.value shouldBe transfer
    }
  }

  it should "fail with the appropriate error if the transfer cannot be stored" in {
    val txHash   = EthereumHash(Generators.genEthHash.sample.get)
    val transfer = InboundTransfer(0L, txHash)
    val error    = errors.ioError("db fail")

    (inboundTransferPort.create _).expects(transfer).returning(dbFailure)

    ops.storeInboundTransfer(transfer).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "findInboundTransferBySecondStep"

  it should "return the found inbound transfer" in {
    val txHash    = EthereumHash(Generators.genEthHash.sample.get)
    val transfer  = InboundTransfer(0L, txHash)
    val transferF = Future.successful(Some(transfer))

    (inboundTransferPort.findBySecondStep _).expects(txHash.hash).returning(transferF)

    ops.findInboundTransferBySecondStep(txHash.hash).value.map { result =>
      result.right.value shouldBe transfer
    }
  }

  it should "fail with the appropriate error if no inbound transfer with the specified transaction hash exists" in {
    val txHash    = EthereumHash(Generators.genEthHash.sample.get)
    val transferF = Future.successful(None)
    val error     = errors.inboundDanglingSecondStep()

    (inboundTransferPort.findBySecondStep _).expects(txHash.hash).returning(transferF)

    ops.findInboundTransferBySecondStep(txHash.hash).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "createOffchainTransfer"

  it should "return the stored offchain transfer" in {
    val transfer  = Fixtures.createOffChainTransfer(Uint(value = BigInt("323")), DateTime.now())
    val transferF = Future.successful(transfer)

    (offchainTransferPort.create _).expects(transfer).returning(transferF)

    ops.storeOffchainTransfer(transfer).value.map { result =>
      result.right.value shouldBe transfer
    }
  }

  it should "fail with the appropriate error if the transfer cannot be stored" in {
    val transfer = Fixtures.createOffChainTransfer(Uint(value = BigInt("323")), DateTime.now())
    val error    = errors.ioError("db fail")

    (offchainTransferPort.create _).expects(transfer).returning(dbFailure)

    ops.storeOffchainTransfer(transfer).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "linkOffchainTxToOnchainTxWithId"

  it should "return the linked offchain transfer" in {
    val transfer  = Fixtures.createOffChainTransfer(Uint(value = BigInt("323")), DateTime.now())
    val transferF = Future.successful(transfer)

    (offchainTransferPort.linkToOnChainTxWithId _).expects(1L, 2L).returning(transferF)

    ops.linkOffchainTxToOnchainTxWithId(1L, 2L).value.map { result =>
      result.right.value shouldBe transfer
    }
  }

  it should "fail with the appropriate error if the transfer cannot be linked" in {
    val error = errors.ioError("db fail")

    (offchainTransferPort.linkToOnChainTxWithId _).expects(1L, 2L).returning(dbFailure)

    ops.linkOffchainTxToOnchainTxWithId(1L, 2L).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "findOutboundTxByHash"

  it should "return the found outbound transfer" in {
    val txHash    = EthereumHash(Generators.genEthHash.sample.get)
    val transfer  = OutboundTransfer(txHash, 1L)
    val transferF = Future.successful(Some(transfer))

    (outboundTransferPort.findByTxHash _).expects(txHash).returning(transferF)

    ops.findOutboundTxByHash(txHash).value.map { result =>
      result.right.value shouldBe transfer
    }
  }

  it should "fail with the appropriate error if the transfer cannot be found" in {
    val txHash    = EthereumHash(Generators.genEthHash.sample.get)
    val transferF = Future.successful(None)
    val error     = errors.cannotFindOutboundTransfer(txHash.hash)

    (outboundTransferPort.findByTxHash _).expects(txHash).returning(transferF)

    ops.findOutboundTxByHash(txHash).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "findEthereumAccountByAddress"

  it should "return the found ethereum account" in {
    val account  = Fixtures.destinationEthAccount
    val address  = account.address
    val accountF = Future.successful(Some(account))

    (ethAccountPort.findAccountByAddress _).expects(address).returning(accountF)

    ops.findEthereumAccountByAddress(address).value.map { result =>
      result.right.value shouldBe account
    }
  }

  it should "fail with the appropriate error if the account cannot be found" in {
    val account  = Fixtures.destinationEthAccount
    val address  = account.address
    val accountF = Future.successful(None)
    val error    = errors.addressNotInStockmind(address.toHex)

    (ethAccountPort.findAccountByAddress _).expects(address).returning(accountF)

    ops.findEthereumAccountByAddress(address).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "sendEthereumTx"

  it should "return the transaction hash of the sent transaction" in {
    val transfer                             = Fixtures.sampleTransferEvent
    val tx                                   = Fixtures.expectedSignableTx(transfer)
    val hash                                 = Generators.genEthHash.sample.get
    val response: Future[JsonRpcPlainResult] = Future.successful(Right(s"0x$hash"))
    val txHash                               = EthereumHash(hash)

    (ethClientPort.sendTransaction _).expects(tx).returning(response)

    ops.sendEthereumTx(tx).value.map { result =>
      result.right.value shouldBe txHash
    }
  }

  it should "fail with the appropriate error if no hash was returned" in {
    val transfer = Fixtures.sampleTransferEvent
    val tx       = Fixtures.expectedSignableTx(transfer)
    val response: Future[JsonRpcPlainResult] =
      Future.successful(Left(UnexpectedEthereumResponse("fail!", 400)))
    val error = errors.emptyTxHashInOnchainOp()

    (ethClientPort.sendTransaction _).expects(tx).returning(response)

    ops.sendEthereumTx(tx).value.map { result =>
      result.left.value shouldBe error
    }
  }

  it should "fail with the appropriate error if the node response cannot be turned into a transaction hash" in {
    val transfer                             = Fixtures.sampleTransferEvent
    val tx                                   = Fixtures.expectedSignableTx(transfer)
    val response: Future[JsonRpcPlainResult] = Future.successful(Right("fail"))
    val error                                = errors.cannotParseTxHash("fail")

    (ethClientPort.sendTransaction _).expects(tx).returning(response)

    ops.sendEthereumTx(tx).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "buildTransferEvent"

  it should "return the built transfer event" in {
    val token       = Fixtures.ethtoken
    val loggedEvent = Fixtures.sampleLoggedEvent()
    val timestamp   = DateTime.now()

    ops.buildTransferEvent(loggedEvent, token, timestamp).value.map { result =>
      result.isRight shouldBe true
    }
  }

  it should "return the appropriate error if the transfer event cannot be built" in {
    val token       = Fixtures.ethtoken
    val wrongOrigin = Address.decode(Generators.genAddress.sample.get)
    val loggedEvent = Fixtures.sampleLoggedEvent().copy(origin = wrongOrigin)
    val timestamp   = DateTime.now()
    val error       = errors.cannotBuildTransferEvent(loggedEvent, token)

    ops.buildTransferEvent(loggedEvent, token, timestamp).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "getTokenMetadata"

  it should "extract the token symbol from the token contract" in {
    val address = Address(Generators.genAddress.sample.get)
    val encodedResponse = "0x" +
      "0000000000000000000000000000000000000000000000000000000000000020" +
      "0000000000000000000000000000000000000000000000000000000000000003" +
      "6162630000000000000000000000000000000000000000000000000000000000"
    val metadata = "abc"

    (ethClientPort.callMethodFrom _).expects(*).returning(Future(Right(encodedResponse)))

    ops.get721TokenMeta(address, address, Uint(value = 1)).value.map { result =>
      result.right.value shouldBe metadata
    }
  }

}
