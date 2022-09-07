package com.clluc.stockmind.core.transaction

import java.util.UUID

import cats.data.State
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.ethereum.{
  Erc20Token,
  Erc721Token,
  EthereumAccount,
  Ethtoken,
  SignableTransaction
}
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.transaction.Fixtures.savedOffChainTransferId
import com.clluc.stockmind.core.transaction.TransactionOpsFixture._
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterHandle}
import com.clluc.stockmind.core.user.{Balance, Balance721, EmailHandle, User}

/**
  * Configurable set of Token Transaction Operations for testing purposes.
  * Emulates an stateful repository, which state is passed to the constructor of the class.
  * @param tokens
  * @param twitterAccountByUserId
  * @param ethereumAccountForUser
  * @param balanceForAccountToken
  * @param twitterIdForHandle
  * @param emailIdForHandle
  * @param emailIdForHandleString
  * @param stockmindUserForId
  * @param oauth1InfoFromLogin
  * @param userFromOauth
  * @param pendingTransfersFromOauth
  * @param _isOmnibusAccountAddress
  */
private[core] case class TransactionOpsFixture(
    tokens: Map[TokenSymbolAnType, Ethtoken] = Map.empty,
    twitterAccountByUserId: Map[UserId, TwitterAccount] = Map.empty,
    ethereumAccountForUser: Map[UserId, EthereumAccount] = Map.empty,
    balanceForAccountToken: Map[BalanceByEthAddressTokenKey, Balance] = Map.empty,
    twitterIdForHandle: Map[TwitterHandle, Long] = Map.empty,
    emailIdForHandle: Map[EmailHandle, User] = Map.empty,
    emailIdForHandleString: Map[String, UserId] = Map.empty,
    stockmindUserForId: Map[UserId, User] = Map.empty,
    oauth1InfoFromLogin: Map[LoginInfo, OAuth1Info] = Map.empty,
    userFromOauth: Map[LoginInfo, UserId] = Map.empty,
    pendingTransfersFromOauth: Map[LoginInfo, List[PendingTransfer]] = Map.empty,
    _isOmnibusAccountAddress: Boolean,
    sendWithdrawTxResult: JsonRpcPlainResult,
    _stockmindUrl: String,
    savedOutboundTransferId: Long,
    tokens721ByIdAndOwner: Map[(BigInt, Address), Erc721Token] = Map.empty,
    findBalance721ForEthereumAddressAndTokenId: Map[(Address, BigInt), Balance721] = Map.empty,
    tokensErc20BySymbolAndType: Map[String, Erc20Token] = Map.empty,
) extends TransactionOps[TestEffectsState] {

  private def concatEffectStateMonad[A](value: A,
                                        stateToConcat: Option[Effect] = None): TestEffectsState[A] =
    State { s =>
      (stateToConcat.map(e => s :+ e).getOrElse(s), value)
    }

  override def findEthTokenBySymbolAndType(symbol_erc_type: String) =
    concatEffectStateMonad(tokens.get(symbol_erc_type))

  override def findTwitterAccountByUserId(id: UUID) =
    concatEffectStateMonad(twitterAccountByUserId.get(id))

  override def findTwitterAccountByScreenName(screenName: TwitterHandle) =
    concatEffectStateMonad(twitterAccountByUserId.values.find(_.screenName == screenName.value))

  override def findEthereumAccountForUserId(id: UUID) =
    concatEffectStateMonad(ethereumAccountForUser.get(id))

  override def findBalanceForEthereumAddressAndToken(address: Address, token: String) =
    concatEffectStateMonad(balanceForAccountToken.get(BalanceByEthAddressTokenKey(address, token)))

  override def findTwitterApiUserIdFromScreenName(screenName: TwitterHandle,
                                                  credentials: OAuth1Info) =
    concatEffectStateMonad(twitterIdForHandle.get(screenName))

  override def findUserByIdentifier(email: EmailHandle) =
    concatEffectStateMonad(emailIdForHandle.get(email))

  def findUserIdFromOAuthProviderAndIdentifier(identifier: String) =
    concatEffectStateMonad(emailIdForHandleString.get(identifier))

  override def findAccountByUserId(id: UUID) =
    concatEffectStateMonad(stockmindUserForId.get(id))

  override def findStockmindUserFromId(id: UUID) =
    concatEffectStateMonad(stockmindUserForId.get(id))

  override def findOAuth1InfoFromLoginInfo(loginInfo: LoginInfo) =
    concatEffectStateMonad(oauth1InfoFromLogin.get(loginInfo))

  override def findUserIdFromOAuthProviderAndLoginKey(loginInfo: LoginInfo) =
    concatEffectStateMonad(userFromOauth.get(loginInfo))

  override def findPendingTransfersByDestination(loginInfo: LoginInfo) =
    concatEffectStateMonad(pendingTransfersFromOauth.getOrElse(loginInfo, List.empty))

  override def findEthereumAccountByAddress(address: Address) =
    concatEffectStateMonad(ethereumAccountForUser.values.find(_.address == address))

  override def savePendingTransaction(transfer: PendingTransfer) =
    concatEffectStateMonad(TransactionIsPending, Some(SavePendingTransaction(transfer)))

  override def notifyStockmindTransferParties(transfer: OffChainTransfer) =
    concatEffectStateMonad((), Some(NotifyStockmindRecipient(transfer)))

  override def notifyPendingTransfer(recipientEthAddress: Address) =
    concatEffectStateMonad((), Some(NotifyPendingTransfer(recipientEthAddress)))

  override def isOmnibusAccountAddress(address: Address) =
    concatEffectStateMonad(_isOmnibusAccountAddress)

  override def sendWithdrawTx(signableTx: SignableTransaction) =
    concatEffectStateMonad(sendWithdrawTxResult, Some(SendWithdrawTx(signableTx)))

  override def writeOffChainTransfer(transfer: OffChainTransfer) = {
    val offChainTx = transfer.copy(id = savedOffChainTransferId)
    concatEffectStateMonad(offChainTx, Some(WriteOffChainTransfer(offChainTx)))
  }

  override def saveOutboundTransferData(outboundTransfer: OutboundTransfer) =
    concatEffectStateMonad(
      outboundTransfer,
      Some(SaveOutboundTransferData(outboundTransfer))
    )

  override def storePendingTransactionInRepository(
      pendingTransferId: Long,
      tokenTransfer: OffChainTransfer
  ) =
    concatEffectStateMonad(
      (),
      Some(SettlePendingTransactionInRepository(pendingTransferId, tokenTransfer)))

  override def stockmindUrl =
    concatEffectStateMonad(_stockmindUrl)

  override def storeTransactionMetaInf(txId: Long, metaInf: Map[String, String]) =
    concatEffectStateMonad((), Some(StoreTransactionMetaInf(metaInf)))

  override def findToken721ByIdAndOwner(id: BigInt, tokenOwner: Address) =
    concatEffectStateMonad(tokens721ByIdAndOwner.get((id, tokenOwner)))

  override def findBalance721ForEthereumAddressAndTokenId(address: Address, idtoken: BigInt) =
    concatEffectStateMonad(findBalance721ForEthereumAddressAndTokenId.get((address, idtoken)))

  override def findErc20TokenBySymbolAndType(symbol_erc_type: String) =
    concatEffectStateMonad(tokensErc20BySymbolAndType.get(symbol_erc_type))
}

private[core] object TransactionOpsFixture {
  type TokenSymbolAnType = String
  type UserId            = UUID

  type TestEffectsState[A] = State[Vector[Effect], A]

  sealed trait Effect

  case class SavePendingTransaction(transfer: PendingTransfer)            extends Effect
  case class SaveCompletedTransaction(transfer: OffChainTransfer)         extends Effect
  case class NotifyStockmindRecipient(transfer: OffChainTransfer)         extends Effect
  case class NotifyPendingTransfer(recipientEthAddress: Address)          extends Effect
  case class SendWithdrawTx(signableTx: SignableTransaction)              extends Effect
  case class WriteOffChainTransfer(transfer: OffChainTransfer)            extends Effect
  case class SaveOutboundTransferData(outboundTransfer: OutboundTransfer) extends Effect
  case class SettlePendingTransactionInRepository(
      pendingTransferId: Long,
      tokenTransfer: OffChainTransfer
  ) extends Effect
  case class StoreTransactionMetaInf(metaInf: Map[String, String])                 extends Effect
  case class BalanceByEthAddressTokenKey(ethAddress: Address, tokenSymbol: String) extends Effect
}
