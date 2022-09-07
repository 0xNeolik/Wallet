package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.auth.{CoreCasInfo, LoginInfo}
import com.clluc.stockmind.port.secondary.CasInfoPort
import doobie.imports._

import scala.concurrent.{ExecutionContext, Future}

private[postgres] class PostgresCasInfoAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext)
    extends CasInfoPort
    with Dao {

  override def findBy(loginInfo: LoginInfo): Future[Option[CoreCasInfo]] = {
    def query(provider: String, key: String) = sql"""
      SELECT ticket
      FROM cas_info
      WHERE
        login_provider = $provider
      AND
        login_key = $key
      """.query[CoreCasInfo]

    selectOne(query(loginInfo.providerID, loginInfo.providerKey))
  }

  override def save(loginInfo: LoginInfo, casInfo: CoreCasInfo): Future[CoreCasInfo] = {

    def query(provider: String, key: String, ticket: String): ConnectionIO[CoreCasInfo] = sql"""
      INSERT INTO cas_info
        (login_provider, login_key, ticket)
      VALUES
        ($provider, $key, $ticket)
      ON CONFLICT (login_provider, login_key) DO UPDATE
      SET
        ticket = $ticket
      WHERE
        cas_info.login_provider = $provider
      AND
        cas_info.login_key = $key
      """.update.withUniqueGeneratedKeys("ticket")

    insertWithFeedback(query(loginInfo.providerID, loginInfo.providerKey, casInfo.ticket))
  }
}

object PostgresCasInfoAdapter {

  def apply(transactor: Transactor[IOLite])(implicit ec: ExecutionContext): PostgresCasInfoAdapter =
    new PostgresCasInfoAdapter(transactor)

}
