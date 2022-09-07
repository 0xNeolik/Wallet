package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.LocalDirectoryData
import org.joda.time.DateTime

case class StockmindTransaction(
    id: Long,
    direction: TransactionDirection,
    pending: Boolean,
    counterparty: Counterparty,
    token: String,
    erc_type: String,
    tokenDescription: String,
    decimals: Int,
    amount: TokenAmount,
    txHash: Option[String], // When it's an on-chain transaction
    date: DateTime
)

case class Stockmind721Transaction(
    id: Long,
    direction: TransactionDirection,
    pending: Boolean,
    counterparty: Counterparty,
    token: String,
    erc_type: String,
    tokenDescription: String,
    meta: String,
    token_id: BigInt,
    txHash: Option[String], // When it's an on-chain transaction
    date: DateTime
)

case class Counterparty(
    ethaddr: Option[String] = None,
    direntry: Option[LocalDirectoryData] = None
)

case class TokenAmount(whole: String, decimal: String)

// ADT to define possible directions of transactions
sealed trait TransactionDirection

case object OutgoingTx extends TransactionDirection

case object IncomingTx extends TransactionDirection

object StockmindTransaction {
  // Type alias needed for some categories algebra operations to compile in Scala
  type ValidatedTxRetrievalResult[A] = Either[TransactionsRetrievalError, A]

  // ADT With possible things that could go wrong with this logic
  sealed trait TransactionsRetrievalError

  case class TokenNotSupported(symbol: String) extends TransactionsRetrievalError

  case class CurrentUserWithoutStockmindEthereumAddress(userId: UUID)
      extends TransactionsRetrievalError

  case class NeitherFromNorToHaveScreenNames(from: Address, to: Address)
      extends TransactionsRetrievalError

  case class TwitterIdDoesNotExistAsTwitterUser(id: Long) extends TransactionsRetrievalError

  case class CurrentUserDoesNotHaveOAuth1InfoInTheSystem(userId: UUID)
      extends TransactionsRetrievalError

  case class ParamShouldNotBeNegative(paramName: String, paramValue: Int)
      extends TransactionsRetrievalError

  // Smart constructors for this ADT
  def tokenNotSupported(symbol: String): TransactionsRetrievalError =
    TokenNotSupported(symbol)

  def currentUserWithoutStockmindEthereumAddress(userId: UUID): TransactionsRetrievalError =
    CurrentUserWithoutStockmindEthereumAddress(userId)

  def neitherFromNorToHaveScreenNames(from: Address, to: Address): TransactionsRetrievalError =
    NeitherFromNorToHaveScreenNames(from, to)

  def twitterIdDoesNotExistAsTwitterUser(id: Long): TransactionsRetrievalError =
    TwitterIdDoesNotExistAsTwitterUser(id)

  def currentUserDoesNotHaveOAuth1InfoInTheSystem(userId: UUID): TransactionsRetrievalError =
    CurrentUserDoesNotHaveOAuth1InfoInTheSystem(userId)

  def paramShouldNotBeNegative(paramName: String, paramValue: Int): TransactionsRetrievalError =
    ParamShouldNotBeNegative(paramName, paramValue)
}
