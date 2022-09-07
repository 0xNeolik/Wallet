package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.transaction.PendingTransfer
import com.clluc.stockmind.port.secondary
import secondary.PendingTransferPort
import doobie.imports._
import doobie.postgres.imports._
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}

import scala.concurrent.{ExecutionContext, Future}

private[postgres] class PostgresPendingTransferAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext
) extends PendingTransferPort
    with Dao {

  override def create(pt: PendingTransfer): Future[PendingTransfer] = {
    def query(
        fromUser: UUID,
        toLoginProvider: String,
        toLoginKey: String,
        tokenSymbol: String,
        erc_type: String,
        amount: BigInt,
        created: DateTime,
        token_id: Option[BigInt]
    ): ConnectionIO[PendingTransfer] =
      sql"""INSERT INTO pending_transfers
        (from_id, to_login_provider, to_login_key, token_symbol, erc_type, amount, created, token_id)
      VALUES
        ($fromUser, $toLoginProvider, $toLoginKey, $tokenSymbol, $erc_type, $amount, $created, $token_id)""".update
        .withUniqueGeneratedKeys("id",
                                 "from_id",
                                 "to_login_provider",
                                 "to_login_key",
                                 "token_symbol",
                                 "erc_type",
                                 "amount",
                                 "created",
                                 "processed",
                                 "token_id")

    val now = LocalDateTime.now(DateTimeZone.UTC).toDateTime
    insertWithFeedback(
      query(pt.fromUser,
            pt.toFutureUser.providerID,
            pt.toFutureUser.providerKey,
            pt.tokenSymbol,
            pt.erc_type,
            pt.amount,
            now,
            pt.token_id))
  }

  override def findById(id: Long): Future[Option[PendingTransfer]] = {
    def query(id: Long) = sql"""
      SELECT *
      FROM pending_transfers
      WHERE
        id = $id
    """.query[PendingTransfer]

    selectOne(query(id))
  }

  override def findPendingByOriginAndType(userId: UUID,
                                          erc_type: String): Future[List[PendingTransfer]] = {
    def query(id: UUID, erc_type: String) = sql"""
      SELECT *
      FROM pending_transfers
      WHERE
        from_id = $id
      AND erc_type=$erc_type
    """.query[PendingTransfer]

    selectMany(query(userId, erc_type))
  }

  override def findPendingByOrigin(userId: UUID): Future[List[PendingTransfer]] = {
    def query(id: UUID) = sql"""
      SELECT id, from_id, to_login_provider, to_login_key, token_symbol, erc_type, amount, created, processed, token_id
      FROM pending_transfers
      WHERE
        from_id = $id
      AND
        processed IS NULL
    """.query[PendingTransfer]

    selectMany(query(userId))
  }

  override def findPendingByDestination(loginInfo: LoginInfo): Future[List[PendingTransfer]] = {
    def query(toLoginProvider: String, toLoginKey: String) = sql"""
      SELECT *
      FROM pending_transfers
      WHERE
        to_login_provider = $toLoginProvider
      AND
        to_login_key = $toLoginKey
      AND
        processed IS NULL
    """.query[PendingTransfer]

    selectMany(query(loginInfo.providerID, loginInfo.providerKey))
  }

  override def markAsProcessed(id: Long, dateTime: DateTime): Future[PendingTransfer] = {
    def query(id: Long, timestamp: DateTime): ConnectionIO[PendingTransfer] =
      sql"""
      UPDATE pending_transfers
      SET processed = $timestamp
      WHERE id = $id
    """.update
        .withUniqueGeneratedKeys("id",
                                 "from_id",
                                 "to_login_provider",
                                 "to_login_key",
                                 "token_symbol",
                                 "erc_type",
                                 "amount",
                                 "created",
                                 "processed")

    insertWithFeedback(query(id, dateTime))
  }

  override def delete(id: Long): Future[Unit] = {
    def query(id: Long) = sql"""
      DELETE
      FROM pending_transfers
      WHERE
        id = $id
    """.update

    update(query(id))
  }
}

object PostgresPendingTransferAdapter {

  def apply(transactor: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): PostgresPendingTransferAdapter =
    new PostgresPendingTransferAdapter(transactor)
}
