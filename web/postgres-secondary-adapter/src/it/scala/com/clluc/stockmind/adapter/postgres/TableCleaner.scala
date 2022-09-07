package com.clluc.stockmind.adapter.postgres
import doobie.imports._
import fs2.interop.cats._

object TableCleaner extends Dao {

  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  override def transactor: Transactor[IOLite] = testTransactor

  def clean() = {
    val statements: ConnectionIO[Unit] = for {
      _ <- sql"""DELETE FROM appconfig""".update.run
      _ <- sql"""DELETE FROM inbound_transfers""".update.run
      _ <- sql"""DELETE FROM outbound_transfers""".update.run
      _ <- sql"""DELETE FROM pending_transfers""".update.run
      _ <- sql"""DELETE FROM transfers_metadata""".update.run
      _ <- sql"""DELETE FROM offchain_transfers""".update.run
      _ <- sql"""DELETE FROM erc20_transfers""".update.run
      _ <- sql"""DELETE FROM erc20_tokens""".update.run
      _ <- sql"""DELETE FROM oauth1_info""".update.run
      _ <- sql"""DELETE FROM auth_tokens""".update.run
      _ <- sql"""DELETE FROM local_directory""".update.run
      _ <- sql"""DELETE FROM ethereum_accounts""".update.run
      _ <- sql"""DELETE FROM twitter_accounts""".update.run
      _ <- sql"""DELETE FROM users""".update.run
    } yield ()
    statements.transact(transactor).unsafePerformIO
  }

  def deleteUsers = update(sql"""DELETE FROM users""".update)

  def deleteAuthTokens = update(sql"""DELETE FROM auth_tokens""".update)

  def deleteErc20Tokens = update(sql"""DELETE FROM erc20_tokens""".update)

  def deleteTwitterAccounts = update(sql"""DELETE FROM twitter_accounts""".update)

}
