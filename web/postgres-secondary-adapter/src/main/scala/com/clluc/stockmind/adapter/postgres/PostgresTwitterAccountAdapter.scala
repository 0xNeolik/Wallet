package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.twitter.TwitterAccount
import com.clluc.stockmind.port.secondary.TwitterAccountPort

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._
import doobie.postgres.pgtypes.UuidType

private[postgres] class PostgresTwitterAccountAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext
) extends TwitterAccountPort
    with Dao {

  override def findAccountById(userID: UUID): Future[Option[TwitterAccount]] = {
    def query(userID: UUID) = sql"""
      SELECT *
      FROM twitter_accounts
      WHERE user_id = $userID
    """.query[TwitterAccount]

    selectOne(query(userID))
  }

  override def findAllScreenNames(): Future[List[String]] = {
    def query = sql"""
      SELECT screenname
      FROM twitter_accounts
    """.query[String]

    selectMany(query)
  }

  override def findTwitterAccountByScreenName(
      screenName: String): Future[Option[TwitterAccount]] = {
    def query = sql"""
      SELECT *
      FROM twitter_accounts
      WHERE
        screenname = $screenName
    """.query[TwitterAccount]

    selectOne(query)
  }

  override def saveTwitterAccount(twitterAccount: TwitterAccount): Future[TwitterAccount] = {
    def query(
        userID: UUID,
        accountID: Long,
        screenName: String,
        verified: Boolean,
        followers: Int,
        avatarURL: Option[String]
    ): ConnectionIO[TwitterAccount] = sql"""
        INSERT INTO twitter_accounts
          (user_id, account_id, screenname, verified, followers, avatar_url)
        VALUES
          ($userID, $accountID, $screenName, $verified, $followers, $avatarURL)
        ON CONFLICT (user_id) DO UPDATE
        SET
          account_id = $accountID,
          screenname = $screenName,
          verified = $verified,
          followers = $followers,
          avatar_url = $avatarURL
        WHERE
          twitter_accounts.user_id = $userID
      """.update.withUniqueGeneratedKeys(
      "user_id",
      "account_id",
      "screenname",
      "verified",
      "followers",
      "avatar_url"
    )

    insertWithFeedback(
      query(
        twitterAccount.userID,
        twitterAccount.accountID,
        twitterAccount.screenName,
        twitterAccount.verified,
        twitterAccount.followers,
        twitterAccount.avatarURL
      )
    )
  }
}

object PostgresTwitterAccountAdapter {

  def apply(tx: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): TwitterAccountPort =
    new PostgresTwitterAccountAdapter(tx)
}
