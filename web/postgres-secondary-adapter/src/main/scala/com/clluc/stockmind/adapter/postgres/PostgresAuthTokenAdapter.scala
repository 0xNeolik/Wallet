package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.user.AuthToken
import com.clluc.stockmind.port.secondary.AuthTokenPort
import doobie.imports._
import doobie.postgres.pgtypes.UuidType
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

private[postgres] trait AbstractPostgresAuthTokenAdapter extends AuthTokenPort {

  val clock: () => DateTime

  implicit val executionContext: ExecutionContext

  override def createAuthTokenForUser(userID: UUID,
                                      expiry: FiniteDuration = 5.minutes): Future[AuthToken] = {
    val token = AuthToken(UUID.randomUUID(),
                          userID,
                          clock()
                            .withZone(DateTimeZone.UTC)
                            .plusSeconds(expiry.toSeconds.toInt))

    saveAuthToken(token)
  }

  override def validateAuthTokenForUser(id: UUID): Future[Option[AuthToken]] =
    findAuthTokenByUserId(id)

  override def cleanExpiredAuthTokens(): Future[Seq[AuthToken]] =
    findAuthTokensExpired(
      clock().withZone(DateTimeZone.UTC)
    ).flatMap { tokens =>
      Future.sequence(tokens.map { token =>
        removeAuthTokenWithId(token.id).map(_ => token)
      })
    }

}

private[postgres] class PostgresAuthTokenAdapter(
    override val clock: () => DateTime,
    override val transactor: Transactor[IOLite]
)(
    override implicit val executionContext: ExecutionContext
) extends AbstractPostgresAuthTokenAdapter
    with Dao {

  override def findAuthTokenByUserId(id: UUID): Future[Option[AuthToken]] = {
    def query(id: UUID) = sql"""
      SELECT *
      FROM auth_tokens
      WHERE
        token_id = $id
    """.query[AuthToken]

    selectOne(query(id))
  }

  override def findAuthTokensExpired(dateTime: DateTime): Future[Seq[AuthToken]] = {
    def query(dateTime: DateTime) = sql"""
      SELECT *
      FROM auth_tokens
      WHERE
        expiry < $dateTime
    """.query[AuthToken]

    selectMany(query(dateTime))
  }

  override def saveAuthToken(token: AuthToken): Future[AuthToken] = {
    def query(tokenId: UUID, userId: UUID, expiry: DateTime): ConnectionIO[AuthToken] = sql"""
      INSERT INTO auth_tokens
        (token_id, user_id, expiry)
      VALUES
        ($tokenId, $userId, $expiry)
    """.update.withUniqueGeneratedKeys("token_id", "user_id", "expiry")

    insertWithFeedback(query(token.id, token.userID, token.expiry))
  }

  override def removeAuthTokenWithId(id: UUID): Future[Unit] = {
    def query(id: UUID) = sql"""
      DELETE
      FROM auth_tokens
      WHERE
        token_id = $id
    """.update

    update(query(id))
  }
}

object PostgresAuthTokenAdapter {

  def apply(
      clock: () => DateTime,
      tx: Transactor[IOLite]
  )(
      implicit
      executionContext: ExecutionContext
  ): PostgresAuthTokenAdapter =
    new PostgresAuthTokenAdapter(clock, tx)

}
