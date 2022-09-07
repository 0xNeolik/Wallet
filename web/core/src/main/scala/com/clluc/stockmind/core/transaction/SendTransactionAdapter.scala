package com.clluc.stockmind.core.transaction

import java.util.UUID

import akka.actor.ActorSystem
import com.clluc.stockmind.core
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.ethereum.{SignableTransaction}
import com.clluc.stockmind.core.twitter.TwitterHandle
import com.clluc.stockmind.core.user.EmailHandle
import core.ethereum.solidity.Address
import com.clluc.stockmind.port.primary.SendTransactionPort
import com.clluc.stockmind.port.secondary._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

/**
  * TODO Add Scaladoc.
  */
class SendTransactionAdapter(
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    twitterAccountPort: TwitterAccountPort,
    ethereumAccountPort: EthereumAccountPort,
    offChainBalancePort: OffchainBalancePort,
    twitterPort: TwitterPort,
    userPort: UsersRepositoryPort,
    oauth1InfoPort: Oauth1InfoPort,
    pendingTransferPort: PendingTransferPort,
    offChainTransferPort: OffChainTransferPort,
    ethereumClientPort: EthereumClientPort,
    outboundTransferPort: OutboundTransferPort,
    settleTransferPort: SettleTransferPort,
    transactionMetaInfPort: TransactionMetaInfPort,
    notValidDestinationAddresses: List[Address],
    _stockmindUrl: String,
)(
    implicit
    actorSystem: ActorSystem,
    executionContext: ExecutionContext
) extends SendTransactionPort
    with TransactionOps[Future] {

  import cats.instances.future._ // Bring into scope Monad[Future]

  override def sendTransaction(
      request: TransactionRequest,
      oauthKey: String,
      oauthSecret: String,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[Either[TokenTransactionError, TransactionResult]] = {

    processTransaction(request,
                       OAuth1Info(oauthKey, oauthSecret),
                       timestampFx,
                       masterAccountPassword)
  }

  override def sendTransaction721(
      request: TransactionRequest721,
      oauthKey: String,
      oauthSecret: String,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[Either[TokenTransactionError, TransactionResult]] = {

    processTransaction721(request,
                          OAuth1Info(oauthKey, oauthSecret),
                          timestampFx,
                          masterAccountPassword)
  }

  override def sendTransactionUser(
      request: TransactionRequestUser,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[Either[TokenTransactionError, TransactionResult]] = {

    processTransactionUser(request, timestampFx, masterAccountPassword)
  }

  override def sendTransactionUser721(
      request: TransactionRequestUser721,
      timestampFx: => DateTime,
      masterAccountPassword: String
  ): Future[Either[TokenTransactionError, TransactionResult]] = {

    processTransactionUser721(request, timestampFx, masterAccountPassword)
  }

  override def savePendingTransaction(transfer: PendingTransfer) =
    pendingTransferPort.create(transfer).map(_ => TransactionIsPending)

  override def notifyStockmindTransferParties(transfer: OffChainTransfer) = {
    val message = "new_transaction"

    actorSystem.actorSelection(s"user/*/*-stream-${transfer.from.toHex}") ! message
    actorSystem.actorSelection(s"user/*/*-stream-${transfer.to.toHex}") ! message

    Future.unit
  }

  override def notifyPendingTransfer(transfer: Address) = {
    val message = "new_transaction"

    actorSystem.actorSelection(s"user/*/*-stream-${transfer.toHex}") ! message

    Future.unit
  }

  override def findErc20TokenBySymbolAndType(symbol_erc_type: String) =
    erc20InfoPort.findErc20TokenBySymbolAndType(symbol_erc_type)

  override def findEthTokenBySymbolAndType(symbol_erc_type: String) =
    erc20InfoPort.findEthereumTokenBySymbolAndType(symbol_erc_type)

  override def findToken721ByIdAndOwner(id: BigInt, tokenOwner: Address) =
    erc721InfoPort.findErc721tokenByIdAndOwner(id, tokenOwner)

  override def findTwitterAccountByUserId(id: UUID) =
    twitterAccountPort.findAccountById(id)

  override def findAccountByUserId(id: UUID) =
    userPort.findAuth0(id)

  override def findTwitterAccountByScreenName(screenName: TwitterHandle) =
    twitterAccountPort.findTwitterAccountByScreenName(screenName.value)

  override def findUserByIdentifier(screenName: EmailHandle) =
    userPort.findByIdentifier(screenName.value)

  override def findEthereumAccountForUserId(id: UUID) =
    ethereumAccountPort.findAccountByUserId(id)

  override def findEthereumAccountByAddress(address: Address) =
    ethereumAccountPort.findAccountByAddress(address)

  override def findBalanceForEthereumAddressAndToken(address: Address, token: String) =
    offChainBalancePort.findBalanceByAddressAndEthereumToken(address, token)

  override def findBalance721ForEthereumAddressAndTokenId(address: Address, idtoken: BigInt) =
    offChainBalancePort.findBalance721ByAddressAndEthereumTokenId(address, idtoken)

  override def findTwitterApiUserIdFromScreenName(screenName: TwitterHandle,
                                                  credentials: OAuth1Info) =
    twitterPort.findUserIdFromScreenName(screenName, credentials)

  override def findStockmindUserFromId(id: UUID) = userPort.find(id)

  override def findOAuth1InfoFromLoginInfo(loginInfo: LoginInfo) =
    oauth1InfoPort.findByProviderIdAndKey(loginInfo)

  override def findUserIdFromOAuthProviderAndLoginKey(loginInfo: LoginInfo) =
    userPort.findByLoginKey(loginInfo.providerKey).map(_.map(_.userID))

  override def findUserIdFromOAuthProviderAndIdentifier(identifier: String) =
    userPort.findByIdentifier(identifier).map(_.map(_.userID))

  override def findPendingTransfersByDestination(loginInfo: LoginInfo) =
    pendingTransferPort.findPendingByDestination(loginInfo)

  override def isOmnibusAccountAddress(address: Address) =
    Future.successful(notValidDestinationAddresses.contains(address))

  override def sendWithdrawTx(signableTx: SignableTransaction) =
    ethereumClientPort.sendTransaction(signableTx)

  override def writeOffChainTransfer(transfer: OffChainTransfer) =
    offChainTransferPort.create(transfer)

  override def saveOutboundTransferData(outboundTransfer: OutboundTransfer) =
    outboundTransferPort.create(outboundTransfer)

  override def storePendingTransactionInRepository(
      pendingTransferId: Long,
      tokenTransfer: OffChainTransfer
  ) = settleTransferPort.settlePendingTransfer(pendingTransferId, tokenTransfer)

  override def stockmindUrl = Future.successful(_stockmindUrl)

  override def settlePendingTransfers(oauthProvider: String,
                                      destinationUserKey: String,
                                      timestampFx: => DateTime) = {
    import WithEitherRecoverable.Instances.FutureWithEitherRecoverable

    implicit val ev: WithEitherRecoverable[Future, TokenTransactionError, Unit] =
      new FutureWithEitherRecoverable

    settleTransfersYetPending(oauthProvider, destinationUserKey, timestampFx)
  }

  override def settlePendingTransfersUser(oauthProvider: String,
                                          destinationUserEmail: String,
                                          timestampFx: => DateTime) = {
    import WithEitherRecoverable.Instances.FutureWithEitherRecoverable

    implicit val ev: WithEitherRecoverable[Future, TokenTransactionError, Unit] =
      new FutureWithEitherRecoverable

    settleTransfersYetPendingUser(oauthProvider, destinationUserEmail, timestampFx)
  }

  override def storeTransactionMetaInf(txId: Long, metaInf: Map[String, String]) =
    transactionMetaInfPort.saveMetaInf(txId, metaInf)
}
