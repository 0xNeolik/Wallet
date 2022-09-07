package com.clluc.stockmind.core.transaction

import java.util.UUID

object CancelPendingTransaction {
  sealed trait CancelPendingTransactionError

  case class TxNotFromTheCancelActionRequester(requesterUserId: UUID, txId: Long)
      extends CancelPendingTransactionError

  case class PendingTxNotFound(txId: Long) extends CancelPendingTransactionError

  case class TxIdNaN(id: String) extends CancelPendingTransactionError

  case class TxIdTooBigToBeALong(id: String) extends CancelPendingTransactionError

  // Smart constructors right here
  def txNotFromTheCancelActionRequester(requesterUserId: UUID,
                                        txId: Long): CancelPendingTransactionError =
    TxNotFromTheCancelActionRequester(requesterUserId, txId)

  def pendingTxNotFound(txId: Long): CancelPendingTransactionError =
    PendingTxNotFound(txId)

  def txIdNaN(id: String): CancelPendingTransactionError =
    TxIdNaN(id)

  type ValidatedCancelPendingTransactionOp[+A] = Either[CancelPendingTransactionError, A]
}
