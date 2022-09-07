package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.transaction.InboundTransfer
import com.clluc.stockmind.port.secondary.InboundTransferPort
import doobie.imports._

import scala.concurrent.{ExecutionContext, Future}

private[postgres] class PostgresInboundTransferAdapter(
    val transactor: Transactor[IOLite]
)(
    override implicit val executionContext: ExecutionContext
) extends InboundTransferPort
    with Dao {

  override def create(it: InboundTransfer): Future[InboundTransfer] = {
    def query(s1: Long, s2: String): ConnectionIO[InboundTransfer] =
      sql"""
      INSERT INTO inbound_transfers
        (first_step, second_step)
      VALUES
        ($s1, $s2)
      """.update
        .withUniqueGeneratedKeys("first_step", "second_step")

    insertWithFeedback(query(it.firstStepId, it.secondStepHash.hash))
  }

  override def findByFirstStep(stepId: Long): Future[Option[InboundTransfer]] = ???

  override def findBySecondStep(stepTxHash: String): Future[Option[InboundTransfer]] = {
    def query(s2: String) = sql"""
      SELECT *
      FROM inbound_transfers
      WHERE
        second_step = $s2
      """.query[InboundTransfer]

    selectOne(query(stepTxHash))
  }
}

object PostgresInboundTransferAdapter {

  def apply(tx: Transactor[IOLite])(implicit ec: ExecutionContext): PostgresInboundTransferAdapter =
    new PostgresInboundTransferAdapter(tx)
}
