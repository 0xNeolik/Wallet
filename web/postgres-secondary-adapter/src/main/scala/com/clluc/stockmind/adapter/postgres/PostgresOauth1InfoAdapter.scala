package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.port.secondary.Oauth1InfoPort
import fs2.interop.cats._

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._

private[postgres] class PostgresOauth1InfoAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext
) extends Oauth1InfoPort
    with Dao {

  override def findByProviderIdAndKey(loginInfo: LoginInfo): Future[Option[OAuth1Info]] = {
    def query(provider: String, key: String) = sql"""
      SELECT token, secret
      FROM oauth1_info
      WHERE
        token_id = $provider
      AND
        token_secret = $key
      """.query[OAuth1Info]

    Future {
      query(loginInfo.providerID, loginInfo.providerKey).option
        .transact(transactor)
        .unsafePerformIO
    }
  }

  override def saveOauthInfo(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
    def query(provider: String,
              key: String,
              token: String,
              secret: String): ConnectionIO[OAuth1Info] = sql"""
      INSERT INTO oauth1_info
        (token_id, token_secret, token, secret)
      VALUES
        ($provider, $key, $token, $secret)
      ON CONFLICT (token_id, token_secret) DO UPDATE
      SET
        token = $token, secret = $secret
      WHERE
        oauth1_info.token_id = $provider
      AND
        oauth1_info.token_secret = $key
      """.update.withUniqueGeneratedKeys("token", "secret")

    insertWithFeedback(
      query(loginInfo.providerID, loginInfo.providerKey, authInfo.token, authInfo.secret)
    )
  }
}

object PostgresOauth1InfoAdapter {

  def apply(transactor: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): PostgresOauth1InfoAdapter =
    new PostgresOauth1InfoAdapter(transactor)
}
