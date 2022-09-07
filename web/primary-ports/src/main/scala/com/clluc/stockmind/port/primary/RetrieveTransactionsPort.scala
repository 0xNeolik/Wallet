package com.clluc.stockmind.port.primary

import java.util.UUID

import com.clluc.stockmind.core.transaction.CancelPendingTransaction.ValidatedCancelPendingTransactionOp
import com.clluc.stockmind.core.transaction.{Stockmind721Transaction, StockmindTransaction}
import com.clluc.stockmind.core.transaction.StockmindTransaction.ValidatedTxRetrievalResult

import scala.concurrent.Future

trait RetrieveTransactionsPort {

  def findTransactionsPage(
      userId: UUID,
      offset: Int,
      numOfTxs: Int
  ): Future[ValidatedTxRetrievalResult[List[StockmindTransaction]]]

  def find721TransactionsPage(
      userId: UUID,
      offset: Int,
      numOfTxs: Int
  ): Future[ValidatedTxRetrievalResult[List[Stockmind721Transaction]]]

  def findTransactionById(userId: UUID, txId: Long): Future[Option[StockmindTransaction]]

  def find721TransactionById(userId: UUID, txId: Long): Future[Option[Stockmind721Transaction]]

  def findPendingTransactionById(txId: Long): Future[Option[StockmindTransaction]]

  def findPending721TransactionById(txId: Long): Future[Option[Stockmind721Transaction]]

  /**
    * Cancels the given pending transaction
    * @param txId The id for the transaction to be cancelled
    * @return En eventual Either with Unit (if successful) or an instance of the CancelPendingTransactionError ADT
    *         if something went wrong.
    */
  def cancelPendingTransactionById(
      txId: Long,
      transactionOwnerUserId: UUID
  ): Future[ValidatedCancelPendingTransactionOp[Unit]]
}
