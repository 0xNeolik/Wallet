package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.ethereum.JsonRpcResponse.UnexpectedEthereumResponse
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.twitter.TwitterHandle
import com.clluc.stockmind.core.user.EmailHandle

// Possible errors in a transaction
sealed trait TokenTransactionError {
  def message(): String
}

case class NonExistentTwitterUser(screenName: TwitterHandle) extends TokenTransactionError {
  override def message(): String = s"$screenName does not exist as a twitter user"
}

case class NonExistentUser(email: EmailHandle) extends TokenTransactionError {
  override def message(): String = s"$email does not exist as a user"
}

case class UserDoesNotHaveEthAccountInPlatform(id: UUID, twitterScreenName: String)
    extends TokenTransactionError {
  override def message(): String =
    s"User with id $id and twitter handle @$twitterScreenName does not have an ethereum account in the system. " +
      s"This is most likely a data integrity issue. Please contact the App maintainer"
}

case class UserDoesNotHaveEthAccountInPlatformIdentifier(id: UUID, email: String)
    extends TokenTransactionError {
  override def message(): String =
    s"User with id $id and identifier $email does not have an ethereum account in the system. " +
      s"This is most likely a data integrity issue. Please contact the App maintainer"
}

case class TokenForTransferNotInPlatform(tokenSymbol: String) extends TokenTransactionError {
  override def message(): String = s"Token $tokenSymbol is not part of the Stockmind platform"
}

case object SourceUserHasNoBalance extends TokenTransactionError {
  override def message(): String = "Source user has not enough balance for the intended transaction"
}

case class SpecifiedAmountAsStringNotValid(amount: String, numberOfDecimals: Int)
    extends TokenTransactionError {
  override def message(): String =
    s"[$amount] with $numberOfDecimals decimals is not a valid number for a transfer amount (expressed as String)"
}

case class SourceUserHasNotEnoughBalance(
    sourceAddress: String,
    token: String,
    availableBalance: BigInt,
    requestedBalance: BigInt
) extends TokenTransactionError {
  override def message(): String =
    s"The ethereum address $sourceAddress has not enough balance to transfer $token token. " +
      s"Requested amount: $requestedBalance; available only $availableBalance"
}

case class DestinationUserHasNoTwitterAccount(targetScreenName: TwitterHandle)
    extends TokenTransactionError {
  override def message(): String =
    s"The twitter handle $targetScreenName used as a destination has no Twitter account"
}

case class TwitterCredentialsForTransferSenderNotValid(userId: UUID) extends TokenTransactionError {
  override def message(): String =
    s"The Twitter credentials for user with id $userId are not valid"
}

case class TweetToRecipientNotSend(cause: Throwable) extends TokenTransactionError {
  override def message(): String =
    s"The tweet to make a transfer recipient aware and to invite her to open a Stockmind account has not been sent: $cause"
}

case class TransferSourceUserDoesNotExist(id: UUID) extends TokenTransactionError {
  override def message(): String =
    s"The user with id $id, intended to be the source of a tokens transfer does not exist"
}

case class NoUserWithLoginKey(oauthProvider: String, loginKey: String)
    extends TokenTransactionError {
  override def message(): String =
    s"User for login provider $oauthProvider and key $loginKey not found in DB"
}

case class NoUserWithIdentifier(oauthProvider: String, identifier: String)
    extends TokenTransactionError {
  override def message(): String =
    s"User for login provider $oauthProvider and email $identifier not found in DB"
}

case class TriedWithdrawToInvalidAccount() extends TokenTransactionError {
  override def message(): String =
    s"A Withdraw to a supplier or master account has been tried"
}

case class NoTwitterAccountForStockmindUser(userId: UUID) extends TokenTransactionError {
  override def message(): String =
    s"User with id $userId has no twitter account registered in Stockmind. This is definitely a data consistency issue." +
      s" If we have that userId in the database it should have a twitter account. All that is created in the sign up" +
      s"process and should be an atomic operation. We need to review the sign up process atomicity"
}

case class NoAccountForStockmindUser(userId: UUID) extends TokenTransactionError {
  override def message(): String =
    s"User with id $userId is not registered in Stockmind. This is definitely a data consistency issue." +
      s"All that is created in the sign up" +
      s"process and should be an atomic operation. We need to review the sign up process atomicity"
}

case class NoEthereumAccountForAddress(address: Address) extends TokenTransactionError {
  override def message(): String =
    s"There's no ethereum account in Stockmind for the address $address. That is likely to be a data consistency issue " +
      s"(the address has to have been taken from somewhere within the system"
}

case class ExceptionInProcess(th: Throwable) extends TokenTransactionError {
  override def message() = th.getMessage
}

// TODO To be removed
case class TwitterAccountNotLinkedToStockmind(twitterScreenName: String)
    extends TokenTransactionError {
  override def message(): String =
    s"The user under @$twitterScreenName screen name have an account in Stockmind, but has no Ethereum account " +
      s"associated"
}

case class EthereumIssue(unexpectedEthereumResponse: UnexpectedEthereumResponse)
    extends TokenTransactionError {
  override def message(): String =
    s"A JSON RPC call to the Ethereum node brought back an unexpected response: " +
      s"Body: ${unexpectedEthereumResponse.statusCode} Status: ${unexpectedEthereumResponse.networkResponseBody}"
}

case class ResultingEthereumTxHashNotValid(hash: String) extends TokenTransactionError {
  override def message(): String =
    s"The transaction hash received from our ethereum node after sending a transaction is not valid: $hash"
}

case object MetaInfoNotAllowedInPendingTransfers extends TokenTransactionError {
  override def message() =
    "Tokens transfer meta info is no allowed for pending transactions " +
      "those addressed to a user that doesn't have an account in the platform yet). " +
      "If you need this feature, contact the development team"
}

object TokenTransactionError {

  type ValidatedTransaction[A] = Either[TokenTransactionError, A]

  def nonExistentTwitterUser(screenName: TwitterHandle): TokenTransactionError =
    NonExistentTwitterUser(screenName)

  def nonExistentUser(email: EmailHandle): TokenTransactionError =
    NonExistentUser(email)

  def userDoesNotHaveEthAccountInPlatform(userId: UUID,
                                          twitterScreenName: String): TokenTransactionError =
    UserDoesNotHaveEthAccountInPlatform(userId, twitterScreenName)

  def userDoesNotHaveEthAccountInPlatformIdentifier(userId: UUID,
                                                    identifier: String): TokenTransactionError =
    UserDoesNotHaveEthAccountInPlatformIdentifier(userId, identifier)

  def tokenForTransferNotInPlatform(tokenSymbol: String): TokenTransactionError =
    TokenForTransferNotInPlatform(tokenSymbol)

  def sourceUserHasNoBalance(): TokenTransactionError =
    SourceUserHasNoBalance

  def specifiedAmountAsStringNotValid(amount: String,
                                      numberOfDecimals: Int): TokenTransactionError =
    SpecifiedAmountAsStringNotValid(amount, numberOfDecimals)

  def destinationUserHasNoTwitterAccount(targetScreenName: TwitterHandle): TokenTransactionError =
    DestinationUserHasNoTwitterAccount(targetScreenName)

  def twitterCredentialsForTransferSenderNotValid(userId: UUID): TokenTransactionError =
    TwitterCredentialsForTransferSenderNotValid(userId)

  def transferSourceUserDoesNotExist(id: UUID): TokenTransactionError =
    TransferSourceUserDoesNotExist(id)

  def sourceUserHasNotEnoughBalance(
      sourceAddress: String,
      token: String,
      availableBalance: BigInt,
      requestedBalance: BigInt
  ): TokenTransactionError =
    SourceUserHasNotEnoughBalance(
      sourceAddress,
      token,
      availableBalance,
      requestedBalance
    )

  def notUserWithLoginKey(oauthProvider: String, loginKey: String): TokenTransactionError =
    NoUserWithLoginKey(oauthProvider, loginKey)

  def notUserWithIdentifier(oauthProvider: String, identifier: String): TokenTransactionError =
    NoUserWithIdentifier(oauthProvider, identifier)

  def triedWithdrawToInvalidAccount(): TokenTransactionError =
    TriedWithdrawToInvalidAccount()

  def noTwitterAccountForStockmindUser(userId: UUID): TokenTransactionError =
    NoTwitterAccountForStockmindUser(userId)

  def noAccountForStockmindUser(userId: UUID): TokenTransactionError =
    NoAccountForStockmindUser(userId)

  def noEthereumAccountForAddress(address: Address): TokenTransactionError =
    NoEthereumAccountForAddress(address)

  // TODO To be removed
  def twitterAccountNotLinkedToStockMind(twitterScreenName: String): TokenTransactionError =
    TwitterAccountNotLinkedToStockmind(twitterScreenName)

  def ethereumIssue(unexpectedEthereumResponse: UnexpectedEthereumResponse): TokenTransactionError =
    EthereumIssue(unexpectedEthereumResponse)

  def tweetToRecipientNotSent(cause: Throwable): TokenTransactionError =
    TweetToRecipientNotSend(cause)

  def resultingEthereumTxHashNotValid(hash: String): TokenTransactionError =
    ResultingEthereumTxHashNotValid(hash)

  def metaInfoNotAllowedInPendingTransfers(): TokenTransactionError =
    MetaInfoNotAllowedInPendingTransfers

  def exceptionInProcess(th: Throwable): TokenTransactionError =
    ExceptionInProcess(th)
}
