package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.transaction.OffChainTransfer
import com.clluc.stockmind.port.secondary.OffChainTransferPort
import doobie.imports._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

private[postgres] class PostgresOffChainTransferAdapter(
    val transactor: Transactor[IOLite]
)(
    override implicit val executionContext: ExecutionContext
) extends OffChainTransferPort
    with Dao {

  private case class DbOffChainTransfer(id: Long,
                                        tokenSymbol: String,
                                        erc_type: String,
                                        from: String,
                                        to: String,
                                        amount: BigInt,
                                        created: DateTime,
                                        onchainTransferId: Option[Long],
                                        token_id: Option[BigInt]) {

    def toOffChainTransfer =
      OffChainTransfer(id,
                       tokenSymbol,
                       erc_type,
                       Address(from),
                       Address(to),
                       Uint(value = amount),
                       created,
                       onchainTransferId,
                       token_id)
  }

  override def create(tx: OffChainTransfer): Future[OffChainTransfer] = {
    def query(tokenSymbol: String,
              erc_type: String,
              from: String,
              to: String,
              amount: BigInt,
              created: DateTime,
              onchainTransferId: Option[Long],
              id: Option[BigInt]): ConnectionIO[DbOffChainTransfer] =
      sql"""
      INSERT INTO offchain_transfers
        (token_symbol, erc_type, from_addr, to_addr, amount, created_date, onchain_transfer_id, token_id)
      VALUES
        ($tokenSymbol, $erc_type, $from, $to, $amount, $created, $onchainTransferId, $id)
      """.update
        .withUniqueGeneratedKeys("id",
                                 "token_symbol",
                                 "erc_type",
                                 "from_addr",
                                 "to_addr",
                                 "amount",
                                 "created_date",
                                 "onchain_transfer_id",
                                 "token_id")

    insertWithFeedback(
      query(tx.tokenSymbol,
            tx.erc_type,
            tx.from.value,
            tx.to.value,
            tx.amount.value,
            tx.created,
            tx.onchainTransferId,
            tx.token_id))
      .map(_.toOffChainTransfer)
  }

  override def find(id: Long): Future[Option[OffChainTransfer]] = {
    def query(id: Long) = sql"""
      SELECT *
      FROM offchain_transfers
      WHERE
        id = $id
      """.query[DbOffChainTransfer]

    selectOne(query(id)).map(_.map(_.toOffChainTransfer))
  }

  override def findTransfersInvolvingAddressAndType(
      address: Address,
      transaction_type: String): Future[List[OffChainTransfer]] = {
    def query(add: String, transaction_type: String) = sql"""

    SELECT *
    FROM offchain_transfers
    WHERE
    (from_addr = $add OR
    to_addr = $add)
    AND erc_type=$transaction_type
    """.query[DbOffChainTransfer]

    selectMany(query(address.value, transaction_type)).map(_.map(_.toOffChainTransfer))
  }

  override def findTransfersInvolvingAddressPage(address: Address,
                                                 limit: Int,
                                                 offset: Int): Future[List[OffChainTransfer]] = {
    def query(address: String, limit: Int, offset: Int) = sql"""
      SELECT *
      FROM offchain_transfers
      WHERE
        from_addr = $address OR
        to_addr = $address
      ORDER BY id desc
      OFFSET ${offset * limit}
      LIMIT $limit
      """.query[DbOffChainTransfer]

    selectMany(query(address.value, limit, offset)).map(_.map(_.toOffChainTransfer))
  }

  override def linkToOnChainTxWithId(offchainTxId: Long,
                                     onchainTxId: Long): Future[OffChainTransfer] = {
    def query(offchainTxId: Long, onchainTxId: Long): ConnectionIO[DbOffChainTransfer] =
      sql"""
           UPDATE offchain_transfers
           SET onchain_transfer_id = $onchainTxId
           WHERE id = $offchainTxId
         """.update
        .withUniqueGeneratedKeys("id",
                                 "token_symbol",
                                 "erc_type",
                                 "from_addr",
                                 "to_addr",
                                 "amount",
                                 "created_date",
                                 "onchain_transfer_id")

    insertWithFeedback(query(offchainTxId, onchainTxId)).map(_.toOffChainTransfer)
  }
}

object PostgresOffChainTransferAdapter {

  def apply(tx: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): PostgresOffChainTransferAdapter =
    new PostgresOffChainTransferAdapter(tx)
}
