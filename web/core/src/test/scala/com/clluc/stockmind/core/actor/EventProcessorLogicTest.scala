package com.clluc.stockmind.core.actor

import cats._
import cats.data.EitherT
import com.clluc.stockmind.core.Generators._
import com.clluc.stockmind.core.actor.{EventProcessorOps => Error}
import com.clluc.stockmind.core.actor.EventProcessorLogic._
import com.clluc.stockmind.core.actor.Fixtures._
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Function, Uint}
import com.clluc.stockmind.core.transaction.{InboundTransfer, OffChainTransfer, OutboundTransfer}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class EventProcessorLogicTest extends FlatSpec with Matchers with EitherValues with MockFactory {

  // Helpers
  private def right[T](x: T) = EitherT.right[Id, EventProcessorOps.Error, T](x)

  private def left[T](error: EventProcessorOps.Error) =
    EitherT.left[Id, EventProcessorOps.Error, T](error)

  private val errors = EventProcessorOps.ErrorConstructors

  // Logic under test
  private val logic: EventProcessorLogic = new EventProcessorLogic {}

  // Common fixtures
  private val dateTime         = DateTime.now
  private val transferId: Long = 434L
  private val txHash           = genEthHash.sample.get
  private val inboundTransfer  = InboundTransfer(transferId, EthereumHash(txHash))

  // Tests

  behavior of "Inbound ERC20 token transfers - step 1 (external to controlled account)"

  trait InboundStep1 {

    val loggedEvent = {
      val topics = List(erc20TransferEventSignature,
                        sourceAccountAddress.value,
                        destinationAccountAddress.value)
      genLoggedEvent.sample.get.copy(origin = tokenContractAdd, topics = topics)
    }

    val transferEvent =
      TransferEvent.fromLoggedEvent(loggedEvent, ethtoken, dateTime).get

    val ethTransaction = EthTransaction(
      destinationAccountAddress,
      token.contract,
      Function("transfer", List(masterAccountAddress, transferEvent.value)))
    val ethereumTx = expectedSignableTx(transferEvent)
  }

  it should "register an inbound transfer when sending tokens to a controlled eth address" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(right(destinationEthAccount))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.sendEthereumTx _).expects(ethereumTx).returning(right(EthereumHash(txHash)))
      (ops.storeInboundTransfer _).expects(inboundTransfer).returning(right(inboundTransfer))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.right.value shouldBe FromExternalToStockmind(ethTransaction, inboundTransfer)
  }

  it should "halt when given events originating from not tracked tokens" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _)
        .expects(loggedEvent.origin)
        .returning(left(errors.addressNotFromAnyTokenContract(loggedEvent.origin.toHex)))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.AddressNotFromAnyTokenContract(loggedEvent.origin.toHex)
  }

  it should "halt when a TransferEvent cannot be built from the event and the token" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(left(errors.cannotBuildTransferEvent(loggedEvent, ethtoken)))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.CannotBuildTransferEvent(loggedEvent, ethtoken)
  }

  it should "halt when the event destination is not a controlled address" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(left(errors.addressNotInStockmind(destinationAccountAddress.toHex)))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.AddressNotInStockmind(destinationAccountAddress.toHex)
  }

  it should "halt if the transfer event cannot be stored" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(right(destinationEthAccount))
      (ops.storeTransferEvent _).expects(transferEvent).returning(left(errors.ioError("")))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.IOError("")
  }

  it should "halt when receiving no hash after sending the received funds to the master account" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(right(destinationEthAccount))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.sendEthereumTx _).expects(ethereumTx).returning(left(errors.emptyTxHashInOnchainOp()))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.EmptyTxHashInOnchainOp
  }

  it should "halt when the txHash received after sending the received funds to the master account cannot be parsed" in new InboundStep1 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(right(destinationEthAccount))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.sendEthereumTx _).expects(ethereumTx).returning(left(errors.cannotParseTxHash(txHash)))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.CannotParseTxHash(txHash)
  }

  it should "halt when the inbound transfer cannot be stored" in new InboundStep1 {

    val expectedInboundTransfer = InboundTransfer(transferId, EthereumHash(txHash))

    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(right(destinationEthAccount))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.sendEthereumTx _).expects(ethereumTx).returning(right(EthereumHash(txHash)))
      (ops.storeInboundTransfer _)
        .expects(expectedInboundTransfer)
        .returning(left(errors.ioError("")))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.IOError("")
  }

  behavior of "Inbound ERC20 token transfers - step 2 (controlled account to master)"

  trait InboundStep2 {

    val loggedEvent: LoggedEvent = {
      val topics = List(erc20TransferEventSignature,
                        destinationAccountAddress.value,
                        masterAccountAddress.value)
      genLoggedEvent.sample.get.copy(origin = tokenContractAdd, topics = topics)
    }

    val transferEvent =
      TransferEvent.fromLoggedEvent(loggedEvent, ethtoken, dateTime).get

    val offchainTransfer = OffChainTransfer(
      tokenSymbol = token.symbol,
      erc_type = token.erc_type,
      from = masterAccountAddress,
      to = destinationAccountAddress,
      amount = transferEvent.value,
      created = transferEvent.processedDate.get,
      onchainTransferId = Some(transferId)
    )

  }

  it should "link both steps of the inbound transfer and create an offchain transfer" in new InboundStep2 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.findInboundTransferBySecondStep _)
        .expects(transferEvent.txHash.hash)
        .returning(right(inboundTransfer))
      (ops.storeOffchainTransfer _).expects(offchainTransfer).returning(right(offchainTransfer))
      (ops.notifyNewTransactionToAddress _).expects(destinationAccountAddress)
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.right.value shouldBe IncomingTxToMaster(transferId, offchainTransfer)
  }

  it should "halt if the transfer event cannot be stored" in new InboundStep2 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _).expects(transferEvent).returning(left(errors.ioError("")))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.IOError("")
  }

  it should "halt if the txHash of the second step is not registered in any inbound transfer" in new InboundStep2 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.findInboundTransferBySecondStep _)
        .expects(transferEvent.txHash.hash)
        .returning(left(errors.inboundDanglingSecondStep()))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.InboundDanglingSecondStep
  }

  it should "halt if the offchain transfer cannot be created" in new InboundStep2 {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.findInboundTransferBySecondStep _)
        .expects(transferEvent.txHash.hash)
        .returning(right(inboundTransfer))
      (ops.storeOffchainTransfer _).expects(offchainTransfer).returning(left(errors.ioError("")))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.IOError("")
  }

  behavior of "Outbound ERC20 token transfers"

  trait Outbound {

    val loggedEvent = {
      val topics = List(erc20TransferEventSignature,
                        masterAccountAddress.value,
                        destinationAccountAddress.value)
      genLoggedEvent.sample.get.copy(origin = tokenContractAdd, topics = topics)
    }

    val transferEvent =
      TransferEvent.fromLoggedEvent(loggedEvent, ethtoken, dateTime).get
    val outboundTransfer = OutboundTransfer(transferEvent.txHash, transferId)

    val offchainTransfer = OffChainTransfer(
      tokenSymbol = token.symbol,
      erc_type = token.erc_type,
      from = masterAccountAddress,
      to = destinationAccountAddress,
      amount = transferEvent.value,
      created = transferEvent.processedDate.get
    )
  }

  it should "store an outbound transfer event and link it with the associated offchain transaction" in new Outbound {

    val storedOffchainTransfer = offchainTransfer.copy(id = outboundTransfer.offchainTransferId,
                                                       onchainTransferId = Some(transferId))

    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.findOutboundTxByHash _).expects(transferEvent.txHash).returning(right(outboundTransfer))
      (ops.linkOffchainTxToOnchainTxWithId _)
        .expects(outboundTransfer.offchainTransferId, transferId)
        .returning(right(storedOffchainTransfer))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.right.value shouldBe OutgoingMasterToExternal(storedOffchainTransfer)
  }

  it should "halt if the transfer event cannot be stored" in new Outbound {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _).expects(transferEvent).returning(left(errors.ioError("")))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.IOError("")
  }

  it should "halt if an outbound transfer is not found" in new Outbound {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.findOutboundTxByHash _)
        .expects(transferEvent.txHash)
        .returning(
          left(errors.cannotFindOutboundTransfer(transferEvent.txHash.toPrefixedHexString)))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.CannotFindOutboundTransfer(
      transferEvent.txHash.toPrefixedHexString)
  }

  it should "halt if the offchain and onchain transfers cannot be linked" in new Outbound {
    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.findEthereumTokenByAddress _).expects(loggedEvent.origin).returning(right(ethtoken))
      (ops.buildTransferEvent _)
        .expects(loggedEvent, ethtoken, dateTime)
        .returning(right(transferEvent))
      (ops.storeTransferEvent _)
        .expects(transferEvent)
        .returning(right(transferEvent.copy(id = transferId)))
      (ops.findOutboundTxByHash _).expects(transferEvent.txHash).returning(right(outboundTransfer))
      (ops.linkOffchainTxToOnchainTxWithId _)
        .expects(outboundTransfer.offchainTransferId, transferId)
        .returning(left(errors.ioError("")))
    }

    val result = logic.handleErc20Event(loggedEvent, dateTime)
    result.value.left.value shouldBe Error.IOError("")
  }

  behavior of "handleEthTransaction - from supplier to anywhere else"

  it should "do nothing" in {
    val sampleTxHash = genEthHash.sample.get
    val transaction = Transaction(EthereumHash(sampleTxHash),
                                  supplierAccountAddress,
                                  Some(genAddressSample),
                                  Block(0),
                                  0,
                                  Uint.apply(value = 10L))

    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.supplierAccountAddress _).expects().returning(supplierAccountAddress)
    }

    val result = logic.handleEtherTransaction(transaction, dateTime)

    result shouldBe EitherT.right[Id, EventProcessorOps.Error, EthereumEventProcessingResult](NoOp)
  }

  behavior of "Inbound ETH transactions - step 2 (stockmind -> master)"

  it should "link both inbound steps and create a new offchain transaction" in {
    val transaction = Transaction(EthereumHash(txHash),
                                  destinationAccountAddress,
                                  Some(masterAccountAddress),
                                  Block(0),
                                  0,
                                  Uint(value = 10L))

    val transferEvent = transaction.toEthTransferEvent(Some(dateTime))

    val offchainTransfer = OffChainTransfer(
      tokenSymbol = etherAsToken.symbol,
      erc_type = token.erc_type,
      from = masterAccountAddress,
      to = destinationAccountAddress,
      amount = Uint(256, 10L),
      created = dateTime,
      onchainTransferId = Some(transferId)
    )

    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.supplierAccountAddress _).expects().returning(supplierAccountAddress)
      (ops.storeTransferEvent _).expects(transferEvent).returning(right(transferEvent))
      (ops.findInboundTransferBySecondStep _)
        .expects(transferEvent.txHash.hash)
        .returning(right(inboundTransfer))
      (ops.storeOffchainTransfer _).expects(offchainTransfer).returning(right(offchainTransfer))
      (ops.notifyNewTransactionToAddress _).expects(destinationAccountAddress)
    }

    val result = logic.handleEtherTransaction(transaction, dateTime)
    result.value.right.value shouldBe IncomingTxToMaster(transferId, offchainTransfer)
  }

  behavior of "Inbound ETH transactions - step 1 (external -> stockmind)"

  it should "register an inbound transfer when sending ether to a controlled eth address" in {
    val transaction = Transaction(EthereumHash(txHash),
                                  sourceAccountAddress,
                                  Some(destinationAccountAddress),
                                  Block(0),
                                  0,
                                  Uint(value = 10L))

    val transferEvent = transaction.toEthTransferEvent(Some(dateTime))

    val ethTransaction = EthTransaction(destinationAccountAddress, masterAccountAddress, 10L)

    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.supplierAccountAddress _).expects().returning(supplierAccountAddress)
      (ops.findEthereumAccountByAddress _)
        .expects(destinationAccountAddress)
        .returning(right(destinationEthAccount))
      (ops.storeTransferEvent _).expects(transferEvent).returning(right(transferEvent))
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.sendEthereumTx _)
        .expects(SignableTransaction(ethTransaction, destinationEthAccount.password))
        .returning(right(EthereumHash(txHash)))
      (ops.storeInboundTransfer _)
        .expects(inboundTransfer.copy(firstStepId = 0L))
        .returning(right(inboundTransfer))
    }

    val result = logic.handleEtherTransaction(transaction, dateTime)
    result.value.right.value shouldBe FromExternalToStockmind(ethTransaction, inboundTransfer)
  }

  behavior of "Outbound ETH transactions (master -> external)"

  it should "store an outbound transfer and link it with the associated offchain transaction" in {
    val transaction = Transaction(EthereumHash(txHash),
                                  masterAccountAddress,
                                  Some(destinationAccountAddress),
                                  Block(0),
                                  0,
                                  Uint(value = 10L))

    val transferEvent = transaction.toEthTransferEvent(Some(dateTime))

    val outboundTransfer = OutboundTransfer(EthereumHash(txHash), 3L)

    val offchainTransfer = OffChainTransfer(
      tokenSymbol = etherAsToken.symbol,
      erc_type = token.erc_type,
      from = sourceAccountAddress,
      to = masterAccountAddress,
      amount = Uint(256, 10L),
      created = dateTime,
      onchainTransferId = Some(transferId)
    )

    implicit val ops = mock[EventProcessorOps[Id]]
    inSequence {
      (ops.retrieveMasterAccountAddress _).expects().returning(masterAccountAddress)
      (ops.supplierAccountAddress _).expects().returning(supplierAccountAddress)
      (ops.storeTransferEvent _).expects(transferEvent).returning(right(transferEvent))
      (ops.findOutboundTxByHash _).expects(EthereumHash(txHash)).returning(right(outboundTransfer))
      (ops.linkOffchainTxToOnchainTxWithId _)
        .expects(outboundTransfer.offchainTransferId, transferEvent.id)
        .returning(right(offchainTransfer))
    }

    val result = logic.handleEtherTransaction(transaction, dateTime)
    result.value.right.value shouldBe OutgoingMasterToExternal(offchainTransfer)
  }
}
