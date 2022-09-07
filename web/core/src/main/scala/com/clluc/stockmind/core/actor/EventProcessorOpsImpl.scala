package com.clluc.stockmind.core.actor

import akka.actor.ActorSystem
import cats.data.EitherT
import cats.syntax.either._
import cats.instances.future._
import com.clluc.stockmind.core.actor.EventProcessorOps._
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, SolidityString, Uint}
import com.clluc.stockmind.core.transaction.{InboundTransfer, OffChainTransfer}
import com.clluc.stockmind.port.secondary._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

private[core] class EventProcessorOpsImpl(
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    erc20TransferEventPort: Erc20TransferEventPort,
    inboundTransferPort: InboundTransferPort,
    offchainTransferPort: OffChainTransferPort,
    outboundTransferPort: OutboundTransferPort,
    ethereumAccountPort: EthereumAccountPort,
    ethereumClientPort: EthereumClientPort,
    masterAccountAddress: Address,
    override val supplierAccountAddress: Address,
    actorSystem: ActorSystem
)(
    implicit
    executionContext: ExecutionContext
) extends EventProcessorOps[Future]
    with LazyLogging {

  override lazy val retrieveMasterAccountAddress: Address = masterAccountAddress

  private val error = ErrorConstructors

  // Local abstractions to reduce code duplication. Not meant to be reusable by other interpreters.
  // In fact, we think is not worth abstracting more, as we would loose granularity.
  // Doing this on a per-method basis allow us to decide what to do exactly in each case.
  private def recoverFx[T]: PartialFunction[Throwable, Either[Error, T]] = {
    // We capture a generic exception because we don't recover only from database access errors;
    // also from API call errors (JSON RPC, Twitter, etc.).
    case e: Exception => Left(error.ioError(e.getMessage))
  }

  // To generalise the following two functions and write them in terms of a more general one
  // (getting rid of structure duplication) implies using a type class or similar approach
  // TODO Team decision: is it worth to do it?
  // It wouldn't affect readability of the overridden methods that use these two.
  private def recoverFuture[T](future: Future[T]): EitherT[Future, Error, T] =
    EitherT(
      future
        .map(Right(_))
        .recover(recoverFx)
    )

  private def recoverFutureOption[T](future: Future[Option[T]],
                                     noneToLeft: => Error): EitherT[Future, Error, T] =
    EitherT(
      future
        .map(_.toRight(noneToLeft))
        .recover(recoverFx)
    )

  override def findEthereumTokenByAddress(address: Address) =
    recoverFutureOption(
      erc20InfoPort.findEthereumTokenByAddress(address),
      error.addressNotFromAnyTokenContract(address.toHex)
    )

  override def findEthereum721TokenByAddress(address: Address) =
    recoverFutureOption(
      erc721InfoPort.findEthereumTokenByAddress(address),
      error.addressNotFromAnyTokenContract(address.toHex)
    )

  override def findEthTokenByAddress(address: Address) =
    recoverFutureOption(
      erc721InfoPort.findEthTokenByAddress(address),
      error.addressNotFromAnyTokenContract(address.toHex)
    )

  override def store721Token(token: Ethtoken, metadata: String, NFTid: Uint) =
    recoverFuture(erc721InfoPort.create721Token(token, metadata, NFTid))

  override def findEthereumTokenBySymbolAndType(symbol_erc_type: String) =
    recoverFutureOption(
      erc20InfoPort.findEthereumTokenBySymbolAndType(symbol_erc_type),
      error.tokenNotFromAnyTokenSymbol(symbol_erc_type)
    )

  override def storeTransferEvent(event: TransferEvent) =
    recoverFuture(erc20TransferEventPort.createTransferEvent(event))

  override def storeInboundTransfer(transfer: InboundTransfer) =
    recoverFuture(inboundTransferPort.create(transfer))

  override def findInboundTransferBySecondStep(hash: TxHash) =
    recoverFutureOption(
      inboundTransferPort.findBySecondStep(hash),
      error.inboundDanglingSecondStep()
    )

  override def storeOffchainTransfer(transfer: OffChainTransfer) =
    recoverFuture(offchainTransferPort.create(transfer))

  override def remove721Token(id: Uint) =
    recoverFuture(erc721InfoPort.deleteTokenFromId(id))

  override def linkOffchainTxToOnchainTxWithId(offTxId: OffchainTxId, onTxId: OnchainTxId) =
    recoverFuture(offchainTransferPort.linkToOnChainTxWithId(offTxId, onTxId))

  override def findOutboundTxByHash(txHash: EthereumHash) =
    recoverFutureOption(
      outboundTransferPort.findByTxHash(txHash),
      error.cannotFindOutboundTransfer(txHash.hash)
    )

  override def findEthereumAccountByAddress(add: Address): EitherT[Future, Error, EthereumAccount] =
    recoverFutureOption(
      ethereumAccountPort.findAccountByAddress(add),
      error.addressNotInStockmind(add.toHex)
    )

  override def sendEthereumTx(tx: SignableTransaction) =
    EitherT(
      ethereumClientPort.sendTransaction(tx).recover(recoverFx)
    ).leftMap {
        case e: IOError => e
        case _          => error.emptyTxHashInOnchainOp()
      }
      .flatMap { hash =>
        EitherT.fromOption(EthereumHash.decodePrefixedHexString(hash),
                           error.cannotParseTxHash(hash))
      }

  override def buildTransferEvent(loggedEvent: LoggedEvent,
                                  token: Ethtoken,
                                  timestamp: DateTime) = {
    EitherT.fromOption(
      TransferEvent.fromLoggedEvent(loggedEvent, token, timestamp),
      error.cannotBuildTransferEvent(loggedEvent, token)
    )
  }

  override def buildMintBurnEvent(loggedEvent: LoggedEvent,
                                  token: Ethtoken,
                                  timestamp: DateTime) = {
    EitherT.fromOption(
      MintBurnEvent.fromLoggedEvent(loggedEvent, token, timestamp),
      error.cannotBuildMintBurnEvent(loggedEvent, token)
    )
  }

  override def notifyNewTransactionToAddress(add: Address) = {
    logger.info(s"Notifying $add via stream")
    actorSystem.actorSelection(s"user/*/*-stream-${add.toHex}") ! "new_transaction"
  }

  override def get721TokenMeta(from: Address,
                               tokenAddress: Address,
                               id: Uint): EitherT[Future, EventProcessorOps.Error, String] = {
    val contract = NFTTokenContract(tokenAddress)

    EitherT(ethereumClientPort.callMethodFrom(contract.metadata(from, id)))
      .leftMap(fail => error.ethereumClientError(fail.statusCode, fail.networkResponseBody))
      .subflatMap { response =>
        val encodedString = response.drop(66)
        val decodedO      = SolidityString.decodeDynamicEncoded(encodedString)
        Either.fromOption(decodedO, error.cannotDecodeResult(encodedString))
      }
  }

}
