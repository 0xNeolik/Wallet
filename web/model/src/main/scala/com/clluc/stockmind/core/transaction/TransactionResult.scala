package com.clluc.stockmind.core.transaction

// Transaction results
sealed trait TransactionResult

case object TransactionIsPending extends TransactionResult

case object TransactionIsCompleted extends TransactionResult

object TransactionResult {

  // Smart constructors for our ADT
  def transactionIsPending(): TransactionResult =
    TransactionIsPending

  def transactionIsCompleted(): TransactionResult =
    TransactionIsCompleted
}
