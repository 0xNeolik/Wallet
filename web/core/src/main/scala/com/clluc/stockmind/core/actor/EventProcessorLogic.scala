package com.clluc.stockmind.core.actor

import cats.Monad
import cats.syntax.applicative._
import com.clluc.stockmind.core.actor.EventProcessorLogic._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import cats.data.EitherT
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.transaction.{InboundTransfer, OffChainTransfer}
import com.typesafe.config.{Config, ConfigFactory}

private[actor] trait EventProcessorLogic extends LazyLogging {

  private val ops     = EventProcessorOps.Syntax
  private val results = ResultConstructors
  type Error = EventProcessorOps.Error
  private val conf: Config = ConfigFactory.load()

  /**
    * This is supposed to be the second step in a transaction from an external account to a Stockmind user
    * Ethereum account. After receiving the ack of the transaction from the user account to master we create
    * the corresponding offchain transaction here, and notify the client about that transaction.
    * @param transferEvent
    * @tparam P
    * @return
    */
  private def inboundStep2[P[_]: EventProcessorOps: Monad](
      transferEvent: TransferEvent,
      timestamp: DateTime,
      token_id: Option[BigInt]): EitherT[P, Error, EthereumEventProcessingResult] = {
    // Todo this could also be External -> Master: think about the implications
    logger.info(s"Transfer of funds to master account complete: $transferEvent")

    for {
      event           <- ops.storeTransferEvent(transferEvent)
      inboundTransfer <- ops.findInboundTransferBySecondStep(event.txHash.hash)
      firstStepId = inboundTransfer.firstStepId
      offchainTransfer = OffChainTransfer(
        tokenSymbol = event.tokenSymbol,
        erc_type = event.erc_type,
        from = event.to, // We register the master account address as the source of this off-chain transfer
        to = event.from, // And the destination is the eth account of the user (to calculate balances properly)
        amount = event.value,
        created = timestamp,
        onchainTransferId = Some(firstStepId),
        token_id = token_id
      )
      storedOffchainTransfer <- ops.storeOffchainTransfer(offchainTransfer)
      _ = ops.notifyNewTransactionToAddress(event.from)
    } yield {
      results.incomingTxToMaster(firstStepId, storedOffchainTransfer)
    }
  }

  /**
    * This is the withdrawal case; after sending a Tx from the master to an external account, once we receive the ack
    * of such transaction, we associate that hash with the off-chain transaction id.
    * @param transferEvent
    * @tparam P
    * @return
    */
  private def outbound[P[_]: EventProcessorOps: Monad](
      transferEvent: TransferEvent): EitherT[P, Error, EthereumEventProcessingResult] = {
    logger.info(s"Transfer of funds to external account complete: $transferEvent")

    for {
      storedTransferEvent <- ops.storeTransferEvent(transferEvent)
      outboundTransfer    <- ops.findOutboundTxByHash(storedTransferEvent.txHash)
      linkedOffchainTransfer <- ops.linkOffchainTxToOnchainTxWithId(
        outboundTransfer.offchainTransferId,
        storedTransferEvent.id)
    } yield {
      results.outgoingMasterToExternal(linkedOffchainTransfer)
    }
  }

  /**
    * Function that handles transactions that originate in external addresses (not Stockmind ones).
    * It handles the case of transactions from an external account to the master account, to a Stockmind account
    * and also between two external addresses. It's the first step of any incoming transaction.
    * In the rest of use cases we return either some detected error or the result of the operation, which include
    * an Ethereum transaction hash and an offchain transaction id.
    * When we detect that a transaction has arrived to any Stockmind user's Ethereum account we save that transaction hash
    * as an inbound transaction. This is the first step in any inbound transaction.
    * @param transferEvent
    * @param token
    * @tparam P The higher kinded context in which all these computations are run.
    * @return EitherT.right(FromExternalToStockmind(_) in case everything goes right and the whole flow is run.
    *         EitherT.left(Error) if anything goes wrong. Error is one of the possible instances of the
    *         EventProcessOps.Error ADT.
    */
  private def inboundStep1[P[_]: EventProcessorOps: Monad](
      transferEvent: TransferEvent,
      token: EthereumToken,
  ): EitherT[P, Error, EthereumEventProcessingResult] = {

    def _signable(password: String) = {
      val master = ops.retrieveMasterAccountAddress()
      val tx = token match {
        case Ether =>
          EthTransaction(transferEvent.to, master, transferEvent.value.value)
        case token: Ethtoken =>
          val contract = HumanStandardTokenContract(token)
          contract.transfer(transferEvent.to, master, transferEvent.value)
      }

      SignableTransaction(tx, password)
    }

    for {
      account             <- ops.findEthereumAccountByAddress(transferEvent.to)
      storedTransferEvent <- ops.storeTransferEvent(transferEvent)
      tx = _signable(account.password)
      txHash <- ops.sendEthereumTx(tx)
      inboundTransfer = InboundTransfer(storedTransferEvent.id, txHash)
      storedInboundTransfer <- ops.storeInboundTransfer(inboundTransfer)
    } yield {
      results.fromExternalToStockmind(tx.tx, storedInboundTransfer)
    }
  }

  /**
    * Function that handles transactions that originate in external addresses (not Stockmind ones).
    * It handles the case of transactions from an external account to the master account, to a Stockmind account
    * and also between two external addresses. It's the first step of any incoming transaction.
    * In the rest of use cases we return either some detected error or the result of the operation, which include
    * an Ethereum transaction hash and an offchain transaction id.
    * When we detect that a transaction has arrived to any Stockmind user's Ethereum account we save that transaction hash
    * as an inbound transaction. This is the first step in any inbound transaction.
    * @param transferEvent
    * @param token
    * @tparam P The higher kinded context in which all these computations are run.
    * @return EitherT.right(FromExternalToStockmind(_) in case everything goes right and the whole flow is run.
    *         EitherT.left(Error) if anything goes wrong. Error is one of the possible instances of the
    *         EventProcessOps.Error ADT.
    */
  private def inboundStep1_ERC721[P[_]: EventProcessorOps: Monad](
      transferEvent: TransferEvent,
      token: Ethtoken,
      NFTid: Uint
  ): EitherT[P, Error, EthereumEventProcessingResult] = {

    def _signable(password: String) = {
      val master = ops.retrieveMasterAccountAddress()
      val tx = token match {
        case token: Ethtoken =>
          val contract = NFTTokenContract(token)
          contract.transfer(transferEvent.to, master, NFTid)
      }

      SignableTransaction(tx, password)
    }
    for {
      account             <- ops.findEthereumAccountByAddress(transferEvent.to)
      storedTransferEvent <- ops.storeTransferEvent(transferEvent)
      tx = _signable(account.password)
      txHash <- ops.sendEthereumTx(tx)
      inboundTransfer = InboundTransfer(storedTransferEvent.id, txHash)
      storedInboundTransfer <- ops.storeInboundTransfer(inboundTransfer)
    } yield {
      results.fromExternalToStockmind(tx.tx, storedInboundTransfer)
    }
  }

  private def stepMint[P[_]: EventProcessorOps: Monad](
      mintEvent: MintBurnEvent,
      token: Ethtoken,
      timestamp: DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {

    for {
      storedTransferEvent <- ops.storeTransferEvent(
        TransferEvent(
          mintEvent.id,
          mintEvent.tokenSymbol,
          mintEvent.erc_type,
          Address.decode(conf.getString("eth.factory.address")), //master account creates own tokens
          mintEvent.owner, //master account creates own tokens
          mintEvent.value,
          mintEvent.block,
          mintEvent.txHash,
          mintEvent.txIndex,
          mintEvent.processedDate
        ))
      inboundTransfer = InboundTransfer(storedTransferEvent.id, storedTransferEvent.txHash)
      storedInboundTransfer <- ops.storeInboundTransfer(inboundTransfer)
      firstStepId = inboundTransfer.firstStepId
      offchainTransfer = OffChainTransfer(
        tokenSymbol = storedTransferEvent.tokenSymbol,
        erc_type = storedTransferEvent.erc_type,
        from = storedTransferEvent.to, // master address
        to = Address(token.owner.get), // add balances to final user in offchain
        amount = storedTransferEvent.value,
        created = timestamp,
        onchainTransferId = Some(firstStepId)
      )
      storedOffchainTransfer <- ops.storeOffchainTransfer(offchainTransfer)
    } yield {
      results.incomingTxToMaster(firstStepId, storedOffchainTransfer)
    }

  }

  private def stepBurn[P[_]: EventProcessorOps: Monad](
      mintEvent: MintBurnEvent,
      token: Ethtoken,
      timestamp: DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {

    for {
      storedTransferEvent <- ops.storeTransferEvent(
        TransferEvent(
          mintEvent.id,
          mintEvent.tokenSymbol,
          mintEvent.erc_type,
          mintEvent.owner, //master account delete own tokens
          Address.default, //0x000.. address to burn tokens
          mintEvent.value,
          mintEvent.block,
          mintEvent.txHash,
          mintEvent.txIndex,
          mintEvent.processedDate
        ))
      inboundTransfer = InboundTransfer(storedTransferEvent.id, storedTransferEvent.txHash)
      storedInboundTransfer <- ops.storeInboundTransfer(inboundTransfer)
      firstStepId = inboundTransfer.firstStepId
      offchainTransfer = OffChainTransfer(
        tokenSymbol = storedTransferEvent.tokenSymbol,
        erc_type = storedTransferEvent.erc_type,
        from = Address(token.owner.get), //owner address
        to = Address.default, // send to 0x000.. Address
        amount = storedTransferEvent.value,
        created = timestamp,
        onchainTransferId = Some(firstStepId)
      )
      storedOffchainTransfer <- ops.storeOffchainTransfer(offchainTransfer)
    } yield {
      results.incomingTxToMaster(firstStepId, storedOffchainTransfer)
    }

  }

  private def stepBurn_ERC721[P[_]: EventProcessorOps: Monad](
      mintEvent: MintBurnEvent,
      token: Ethtoken,
      id: Uint,
      timestamp: DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {

    for {
      storedTransferEvent <- ops.storeTransferEvent(
        TransferEvent(
          mintEvent.id,
          mintEvent.tokenSymbol,
          mintEvent.erc_type,
          mintEvent.owner, //master account delete own tokens
          Address.default, //0x000.. address to burn tokens
          mintEvent.value,
          mintEvent.block,
          mintEvent.txHash,
          mintEvent.txIndex,
          mintEvent.processedDate,
          Some(id.value)
        ))
      inboundTransfer = InboundTransfer(storedTransferEvent.id, storedTransferEvent.txHash)
      storedInboundTransfer <- ops.storeInboundTransfer(inboundTransfer)
      firstStepId = inboundTransfer.firstStepId
      offchainTransfer = OffChainTransfer(
        tokenSymbol = storedTransferEvent.tokenSymbol,
        erc_type = storedTransferEvent.erc_type,
        from = Address(token.owner.get), //owner address
        to = Address.default, // send to 0x000.. Address
        amount = storedTransferEvent.value,
        created = timestamp,
        onchainTransferId = Some(firstStepId),
        token_id = Some(id.value)
      )
      storedOffchainTransfer <- ops.storeOffchainTransfer(offchainTransfer)
      //_                      <- ops.remove721Token(id)
    } yield {
      results.incomingTxToMaster(firstStepId, storedOffchainTransfer)
    }

  }

  def handleErc20Event[P[_]: EventProcessorOps: Monad](
      event: LoggedEvent,
      timestamp: => DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {
    logger.info(s"Event received: $event")

    val origin      = Address.decode(event.topics(1))
    val destination = Address.decode(event.topics(2))

    ops.retrieveMasterAccountAddress() match {
      case `origin` =>
        for {
          token         <- ops.findEthereumTokenByAddress(event.origin)
          transferEvent <- ops.buildTransferEvent(event, token, timestamp)
          result        <- outbound(transferEvent)
        } yield result
      case `destination` =>
        for {
          token         <- ops.findEthereumTokenByAddress(event.origin)
          transferEvent <- ops.buildTransferEvent(event, token, timestamp)
          result        <- inboundStep2(transferEvent, timestamp, None)
        } yield result
      case _ =>
        for {
          token         <- ops.findEthereumTokenByAddress(event.origin)
          transferEvent <- ops.buildTransferEvent(event, token, timestamp)
          result        <- inboundStep1(transferEvent, token)
        } yield result
    }
  }

  def handleErc20MintEvent[P[_]: EventProcessorOps: Monad](
      event: LoggedEvent,
      timestamp: => DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {
    logger.info(s"Event received: $event")

    // val origin = Address.decode(event.topics(1))

    for {
      token         <- ops.findEthereumTokenByAddress(event.origin)
      transferEvent <- ops.buildMintBurnEvent(event, token, timestamp)
      result        <- stepMint(transferEvent, token, timestamp)
    } yield result

  }

  //Token 721-Event
  def handleErc721MintEvent[P[_]: EventProcessorOps: Monad](
      event: LoggedEvent,
      timestamp: => DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {
    logger.info(s"Event received: $event")

    val origin      = Address.decode(event.topics(1))
    val destination = Address.decode(event.topics(2))
    val id          = Uint.decode(256, event.topics(3).drop(2))
    ops.retrieveMasterAccountAddress() match {
      case `origin` =>
        for {
          token         <- ops.findEth721TokenByAddress(event.origin)
          transferEvent <- ops.buildTransferEvent(event, token, timestamp)
          result        <- outbound(transferEvent)
        } yield result
      case `destination` => //user->master
        for {
          token         <- ops.findEth721TokenByAddress(event.origin)
          metadata      <- ops.get721TokenMeta(destination, token.contract, id) //master retrieve metadata
          transferEvent <- ops.buildTransferEvent(event, token, timestamp)
          _             <- ops.store721Token(token, metadata, id) //store new token 721
          result        <- inboundStep2(transferEvent, timestamp, Some(id.value))
        } yield result
      case _ => //factory->user
        for {
          token         <- ops.findEth721TokenByAddress(event.origin)
          transferEvent <- ops.buildTransferEvent(event, token, timestamp)
          result        <- inboundStep1_ERC721(transferEvent, token, id)
        } yield result
    }

  }

  def handleErc20BurnEvent[P[_]: EventProcessorOps: Monad](
      event: LoggedEvent,
      timestamp: => DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {
    logger.info(s"Event received: $event")

    // val origin = Address.decode(event.topics(1))

    for {
      token         <- ops.findEthereumTokenByAddress(event.origin)
      transferEvent <- ops.buildMintBurnEvent(event, token, timestamp)
      result        <- stepBurn(transferEvent, token, timestamp)
    } yield result

  }

  def handleErc721BurnEvent[P[_]: EventProcessorOps: Monad](
      event: LoggedEvent,
      timestamp: => DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {
    logger.info(s"Event received: $event")

    // val origin = Address.decode(event.topics(1))
    val id = Uint.decode(256, event.topics(3).drop(2))

    for {
      token         <- ops.findEth721TokenByAddress(event.origin)
      transferEvent <- ops.buildMintBurnEvent(event, token, timestamp)
      result        <- stepBurn_ERC721(transferEvent, token, id, timestamp)
    } yield result

  }

  def handleEtherTransaction[P[_]: EventProcessorOps: Monad](
      transaction: Transaction,
      timestamp: => DateTime
  ): EitherT[P, Error, EthereumEventProcessingResult] = {

    val masterAccount   = ops.retrieveMasterAccountAddress()
    val supplierAccount = ops.supplierAccountAddress()
    val noop: EitherT[P, Error, EthereumEventProcessingResult] =
      EitherT.right(results.noOp().pure[P])

    val transfer = transaction.toEthTransferEvent(Some(timestamp))

    if (transaction.from == supplierAccount) { // Supplier -> whatever
      // Do nothing. These don't count
      noop
    } else if (transaction.to.get == masterAccount) { // Stockmind -> Master
      // TODO Unknown -> Master
      inboundStep2(transfer, timestamp, None)
    } else if (transaction.from == masterAccount) { // Master -> External
      outbound(transfer)

    } else { // External -> Stockmind
      inboundStep1(transfer, Ether)
    }

  }

}

private[actor] object EventProcessorLogic {

  // ADT that represents the different possible final results of this logic
  sealed trait EthereumEventProcessingResult
  case class IncomingTxToMaster(onchainTransferId: Long, offchainTransfer: OffChainTransfer)
      extends EthereumEventProcessingResult
  case class FromExternalToStockmind(ethTransaction: EthTransaction,
                                     inboundTransfer: InboundTransfer)
      extends EthereumEventProcessingResult
  case class OutgoingMasterToExternal(transfer: OffChainTransfer)
      extends EthereumEventProcessingResult
  case object NoOp extends EthereumEventProcessingResult

  object ResultConstructors {

    def incomingTxToMaster(onchainTransferId: Long,
                           offchainTransfer: OffChainTransfer): EthereumEventProcessingResult =
      IncomingTxToMaster(onchainTransferId, offchainTransfer)

    def fromExternalToStockmind(ethTransaction: EthTransaction,
                                inboundTransfer: InboundTransfer): EthereumEventProcessingResult =
      FromExternalToStockmind(ethTransaction, inboundTransfer)

    def outgoingMasterToExternal(
        offchainTransfer: OffChainTransfer): EthereumEventProcessingResult =
      OutgoingMasterToExternal(offchainTransfer)

    def noOp(): EthereumEventProcessingResult =
      NoOp
  }
}
