package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.EthereumHash
import com.clluc.stockmind.core.transaction.OutboundTransfer
import com.clluc.stockmind.port.secondary.OutboundTransferPort
import doobie.imports._

import scala.concurrent.{ExecutionContext, Future}

private[postgres] class PostgresOutboundTransferAdapter(
    val transactor: Transactor[IOLite]
)(
    override implicit val executionContext: ExecutionContext
) extends OutboundTransferPort
    with Dao {
  override def create(ot: OutboundTransfer): Future[OutboundTransfer] = {
    def query(txHash: String, offchainId: Long): ConnectionIO[OutboundTransfer] =
      sql"""
      INSERT INTO outbound_transfers
        (tx_hash, offchain_transfer_id)
      VALUES
        ($txHash, $offchainId)
      """.update
        .withUniqueGeneratedKeys("tx_hash", "offchain_transfer_id")

    insertWithFeedback(query(ot.transactionHash.hash, ot.offchainTransferId))
  }

  override def findByTxHash(txHash: EthereumHash): Future[Option[OutboundTransfer]] = {
    def query(hash: String) = sql"""
      SELECT *
      FROM outbound_transfers
      WHERE
        tx_hash = $hash
      """.query[OutboundTransfer]

    selectOne(query(txHash.hash))
  }
}

object PostgresOutboundTransferAdapter {

  def apply(tx: Transactor[IOLite])(
      implicit ec: ExecutionContext): PostgresOutboundTransferAdapter =
    new PostgresOutboundTransferAdapter(tx)
}
