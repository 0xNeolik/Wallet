package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.{Block, EthereumHash, TransferEvent}
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.port.secondary.Erc20TransferEventPort
import doobie.imports.{IOLite, Transactor}
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._

private[postgres] class PostgresErc20TransferEventAdapter(
    val transactor: Transactor[IOLite]
)(
    implicit
    val executionContext: ExecutionContext
) extends Erc20TransferEventPort
    with Dao {

  private case class DbTransferEvent(
      id: Long,
      tokenSymbol: String,
      erc_type: String,
      from: String,
      to: String,
      value: BigInt,
      block: Int,
      txHash: String,
      txIndex: Int,
      processedDate: Option[DateTime]
  ) {

    lazy val toTransferEvent =
      TransferEvent(id,
                    tokenSymbol,
                    erc_type,
                    Address(from),
                    Address(to),
                    Uint(256, value),
                    Block(block),
                    EthereumHash(txHash),
                    txIndex,
                    processedDate)

  }

  override def createTransferEvent(transferEvent: TransferEvent): Future[TransferEvent] = {
    def query(token: String,
              erc_type: String,
              from: String,
              to: String,
              value: BigInt,
              block: Int,
              txHash: String,
              txIndex: Int,
              processedDate: DateTime,
              token_id: Option[BigInt]): ConnectionIO[DbTransferEvent] =
      sql"""
      INSERT INTO erc_transfers
        (token_symbol, erc_type, param_from, param_to, param_value, block, tx_hash, tx_index, processed_date, token_id)
      VALUES
        ($token, $erc_type, $from, $to, $value, $block, $txHash, $txIndex, $processedDate, $token_id)
      """.update
        .withUniqueGeneratedKeys("id",
                                 "token_symbol",
                                 "erc_type",
                                 "param_from",
                                 "param_to",
                                 "param_value",
                                 "block",
                                 "tx_hash",
                                 "tx_index",
                                 "processed_date",
                                 "token_id")

    val now = LocalDateTime.now(DateTimeZone.UTC).toDateTime

    insertWithFeedback(
      query(
        transferEvent.tokenSymbol,
        transferEvent.erc_type,
        transferEvent.from.value,
        transferEvent.to.value,
        transferEvent.value.value,
        transferEvent.block.blockNumber,
        transferEvent.txHash.hash,
        transferEvent.txIndex,
        now,
        transferEvent.token_id
      )).map(_.toTransferEvent)
  }

  override def findTransfersInvolvingAddress(address: Address): Future[List[TransferEvent]] = {
    def query(address: String) = sql"""
      SELECT *
      FROM erc_transfers
      WHERE
        param_from = $address OR
        param_to = $address
      ORDER BY processed_date desc
      """.query[DbTransferEvent]

    selectMany(query(address.value)).map(_.map(_.toTransferEvent))
  }

  override def findTransfersInvolvingAddressPage(address: Address,
                                                 limit: Int,
                                                 offset: Int): Future[List[TransferEvent]] = {
    def query(address: String, limit: Int, offset: Int) = sql"""
      SELECT *
      FROM erc_transfers
      WHERE
        param_from = $address OR
        param_to = $address
      ORDER BY id desc
      OFFSET ${offset * limit}
      LIMIT $limit
      """.query[DbTransferEvent]

    selectMany(query(address.value, limit, offset)).map(_.map(_.toTransferEvent))
  }

  override def find(id: Long): Future[Option[TransferEvent]] = {
    def query(id: Long) = sql"""
      SELECT *
      FROM erc_transfers
      WHERE
        id = $id
      """.query[DbTransferEvent]

    selectOne(query(id)).map(_.map(_.toTransferEvent))
  }
}

object PostgresErc20TransferEventAdapter {

  def apply(tx: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): PostgresErc20TransferEventAdapter =
    new PostgresErc20TransferEventAdapter(tx)
}
