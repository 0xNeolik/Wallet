package com.clluc.stockmind.adapter.postgres

import cats.syntax.cartesian._
import com.clluc.stockmind.core.transaction.OffChainTransfer
import com.clluc.stockmind.port.secondary.SettleTransferPort
import doobie.imports._
import fs2.interop.cats._

import scala.concurrent.{ExecutionContext, Future}

/**
  * TODO Add Scaladoc.
  */
private[postgres] class PostgresSettleTransferAdapter(
    val transactor: Transactor[IOLite]
)(
    implicit
    val executionContext: ExecutionContext
) extends SettleTransferPort
    with Dao {
  override def settlePendingTransfer(
      pendingTransferId: Long,
      transfer: OffChainTransfer
  ): Future[Unit] = {
    val create: Update0 = sql"""
           INSERT INTO offchain_transfers
                   (token_symbol, erc_type, from_addr, to_addr, amount, created_date, token_id)
                 VALUES
                   (${transfer.tokenSymbol},
                   ${transfer.erc_type},
                   ${transfer.from.value},
                   ${transfer.to.value},
                   ${transfer.amount.value},
                   ${transfer.created},
                   ${transfer.token_id})

         """.update

    val update: Update0 = sql"""
                      UPDATE pending_transfers
                            SET processed = ${transfer.created}
                            WHERE id = $pendingTransferId
                          """.update

    val transactionalOps = (create.run |@| update.run).tupled

    Future { transactionalOps.transact(transactor).unsafePerformIO }
  }
}

object PostgresSettleTransferAdapter {

  def apply(
      transactor: Transactor[IOLite]
  )(
      implicit
      executionContext: ExecutionContext
  ): PostgresSettleTransferAdapter = new PostgresSettleTransferAdapter(transactor)
}
