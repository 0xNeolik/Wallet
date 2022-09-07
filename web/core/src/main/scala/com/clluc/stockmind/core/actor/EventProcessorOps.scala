package com.clluc.stockmind.core.actor

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.transaction.{InboundTransfer, OffChainTransfer, OutboundTransfer}
import org.joda.time.DateTime
import cats.data.EitherT

private[core] trait EventProcessorOps[P[_]] {

  import EventProcessorOps._

  def retrieveMasterAccountAddress(): Address

  def findEthereumTokenByAddress(address: Address): EitherT[P, Error, Ethtoken]

  def findEthereum721TokenByAddress(address: Address): EitherT[P, Error, Erc721Token]

  def findEthTokenByAddress(address: Address): EitherT[P, Error, Ethtoken]

  def store721Token(token: Ethtoken, metadata: String, NFTid: Uint): EitherT[P, Error, Erc721Token]

  def findEthereumTokenBySymbolAndType(symbol_erc_type: String): EitherT[P, Error, Ethtoken]

  def storeTransferEvent(event: TransferEvent): EitherT[P, Error, TransferEvent]

  def storeInboundTransfer(transfer: InboundTransfer): EitherT[P, Error, InboundTransfer]

  def findInboundTransferBySecondStep(hash: TxHash): EitherT[P, Error, InboundTransfer]

  def storeOffchainTransfer(transfer: OffChainTransfer): EitherT[P, Error, OffChainTransfer]

  def remove721Token(id: Uint): EitherT[P, Error, Unit]

  def linkOffchainTxToOnchainTxWithId(offTxId: OffchainTxId,
                                      onTxId: OnchainTxId): EitherT[P, Error, OffChainTransfer]

  def findOutboundTxByHash(txHash: EthereumHash): EitherT[P, Error, OutboundTransfer]

  def findEthereumAccountByAddress(add: Address): EitherT[P, Error, EthereumAccount]

  def sendEthereumTx(tx: SignableTransaction): EitherT[P, Error, EthereumHash]

  def buildTransferEvent(loggedEvent: LoggedEvent,
                         token: Ethtoken,
                         timestamp: DateTime): EitherT[P, Error, TransferEvent]

  def buildMintBurnEvent(loggedEvent: LoggedEvent,
                         token: Ethtoken,
                         timestamp: DateTime): EitherT[P, Error, MintBurnEvent]

  def supplierAccountAddress(): Address

  def notifyNewTransactionToAddress(add: Address): Unit

  def get721TokenMeta(from: Address, tokenAddress: Address, id: Uint): EitherT[P, Error, String]

}

private[actor] object EventProcessorOps {

  type TxHash       = String
  type OffchainTxId = Long
  type OnchainTxId  = Long

  sealed trait Error
  case class IOError(reason: String)                         extends Error
  case class AddressNotFromAnyTokenContract(address: String) extends Error
  case class TokenNotFromAnyTokenSymbol(address: String)     extends Error
  case class AddressNotInStockmind(address: String)          extends Error
  case object InboundDanglingSecondStep                      extends Error // Second step of inbound transfer is not associated with a first step
  case object EmptyTxHashInOnchainOp                         extends Error
  case class CannotParseTxHash(hash: String)                 extends Error
  case class CannotCreateOnchainTxReprFromEvent(event: LoggedEvent, token: EthereumToken)
      extends Error
  case class CannotFindOutboundTransfer(txHash: String)                          extends Error
  case class CannotBuildTransferEvent(loggedEvent: LoggedEvent, token: Ethtoken) extends Error
  case class CannotBuildMintBurnEvent(loggedEvent: LoggedEvent, token: Ethtoken) extends Error
  case class EthereumClientError(statusCode: Int, body: String)                  extends Error
  case class CannotDecodeResult(string: String)                                  extends Error

  object ErrorConstructors {

    def ethereumClientError(statusCode: Int, body: String): Error =
      EthereumClientError(statusCode, body)
    def cannotDecodeResult(string: String): Error = CannotDecodeResult(string)

    def ioError(reason: String): Error = IOError(reason)

    def addressNotFromAnyTokenContract(address: String): Error =
      AddressNotFromAnyTokenContract(address)

    def tokenNotFromAnyTokenSymbol(symbol: String): Error =
      TokenNotFromAnyTokenSymbol(symbol)

    def addressNotInStockmind(address: String): Error =
      AddressNotInStockmind(address)

    def inboundDanglingSecondStep(): Error = InboundDanglingSecondStep

    def emptyTxHashInOnchainOp(): Error = EmptyTxHashInOnchainOp

    def cannotParseTxHash(hash: String): Error = CannotParseTxHash(hash)

    def cannotCreateOnchainTxReprFromEvent(event: LoggedEvent, token: EthereumToken): Error =
      CannotCreateOnchainTxReprFromEvent(event, token)

    def cannotFindOutboundTransfer(txHash: String): Error = CannotFindOutboundTransfer(txHash)

    def cannotBuildTransferEvent(loggedEvent: LoggedEvent, token: Ethtoken): Error =
      CannotBuildTransferEvent(loggedEvent, token)

    def cannotBuildMintBurnEvent(loggedEvent: LoggedEvent, token: Ethtoken): Error =
      CannotBuildMintBurnEvent(loggedEvent, token)
  }

  object Syntax {

    def retrieveMasterAccountAddress[P[_]]()(implicit ev: EventProcessorOps[P]): Address =
      ev.retrieveMasterAccountAddress()

    def findEthereumTokenByAddress[P[_]](address: Address)(implicit ev: EventProcessorOps[P]) =
      ev.findEthereumTokenByAddress(address)

    def findEthereum721TokenByAddress[P[_]](address: Address)(implicit ev: EventProcessorOps[P]) =
      ev.findEthereum721TokenByAddress(address)

    def findEth721TokenByAddress[P[_]](address: Address)(implicit ev: EventProcessorOps[P]) =
      ev.findEthTokenByAddress(address)

    def storeTransferEvent[P[_]](event: TransferEvent)(implicit ev: EventProcessorOps[P]) =
      ev.storeTransferEvent(event)

    def store721Token[P[_]](token: Ethtoken, metadata: String, NFTid: Uint)(
        implicit ev: EventProcessorOps[P]) =
      ev.store721Token(token, metadata, NFTid)

    def get721TokenMeta[P[_]](from: Address, tokenAddress: Address, NFTid: Uint)(
        implicit ev: EventProcessorOps[P]) =
      ev.get721TokenMeta(from, tokenAddress, NFTid)

    def storeInboundTransfer[P[_]](transfer: InboundTransfer)(implicit ev: EventProcessorOps[P]) =
      ev.storeInboundTransfer(transfer)

    def findInboundTransferBySecondStep[P[_]](hash: TxHash)(implicit ev: EventProcessorOps[P]) =
      ev.findInboundTransferBySecondStep(hash)

    def storeOffchainTransfer[P[_]](transfer: OffChainTransfer)(implicit ev: EventProcessorOps[P]) =
      ev.storeOffchainTransfer(transfer)

    def remove721Token[P[_]](id: Uint)(implicit ev: EventProcessorOps[P]) =
      ev.remove721Token(id)

    def linkOffchainTxToOnchainTxWithId[P[_]](offTxId: OffchainTxId, onTxId: OnchainTxId)(
        implicit ev: EventProcessorOps[P]) =
      ev.linkOffchainTxToOnchainTxWithId(offTxId, onTxId)

    def findOutboundTxByHash[P[_]](txHash: EthereumHash)(implicit ev: EventProcessorOps[P]) =
      ev.findOutboundTxByHash(txHash)

    def findEthereumAccountByAddress[P[_]](add: Address)(implicit ev: EventProcessorOps[P]) =
      ev.findEthereumAccountByAddress(add)

    def findEthereumTokenBySymbolAndType[P[_]](add_erc_type: String)(
        implicit ev: EventProcessorOps[P]) =
      ev.findEthereumTokenBySymbolAndType(add_erc_type)

    def sendEthereumTx[P[_]](tx: SignableTransaction)(implicit ev: EventProcessorOps[P]) =
      ev.sendEthereumTx(tx)

    def buildTransferEvent[P[_]](loggedEvent: LoggedEvent, token: Ethtoken, timestamp: DateTime)(
        implicit ev: EventProcessorOps[P]) =
      ev.buildTransferEvent(loggedEvent, token, timestamp)

    def buildMintBurnEvent[P[_]](loggedEvent: LoggedEvent, token: Ethtoken, timestamp: DateTime)(
        implicit ev: EventProcessorOps[P]) =
      ev.buildMintBurnEvent(loggedEvent, token, timestamp)

    def supplierAccountAddress[P[_]]()(implicit ev: EventProcessorOps[P]) =
      ev.supplierAccountAddress()

    def notifyNewTransactionToAddress[P[_]](add: Address)(implicit ev: EventProcessorOps[P]) =
      ev.notifyNewTransactionToAddress(add)
  }
}
