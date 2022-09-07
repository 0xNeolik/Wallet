package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.Block

import scala.concurrent.{ExecutionContext, Future}
import doobie.imports._
import com.clluc.stockmind.port.secondary.AppConfigPort

private[postgres] trait AbstractPostgresAppConfigAdapter extends AppConfigPort {
  protected[postgres] def get(key: String): Future[Option[String]]

  protected[postgres] def set(key: String, value: String): Future[String]

  override def getBlock(key: String)(implicit ec: ExecutionContext): Future[Block] = {
    get(key).map { blockO =>
      val blockNumber = blockO.getOrElse("0").toInt
      Block(blockNumber)
    }
  }

  override def setBlock(key: String, block: Block)(implicit ec: ExecutionContext): Future[Block] =
    set(key, block.blockNumber.toString).map { number =>
      Block(number.toInt)
    }
}

private[postgres] class PostgresAppConfigAdapter(val transactor: Transactor[IOLite])(
    implicit val executionContext: ExecutionContext)
    extends AbstractPostgresAppConfigAdapter
    with Dao {

  override def get(key: String): Future[Option[String]] = {
    def query(key: String) = sql"""
         SELECT value
           FROM appconfig
         WHERE
           key = $key
         """.query[String]

    selectOne(query(key))
  }

  override def set(key: String, value: String): Future[String] = {
    def query(key: String, value: String): ConnectionIO[String] =
      sql"""INSERT INTO appconfig
          (key, value)
        VALUES
          ($key, $value)
        ON CONFLICT (key) DO UPDATE
        SET
          value = $value
        WHERE
          appconfig.key = $key
      """.update.withUniqueGeneratedKeys("value")

    insertWithFeedback(query(key, value))
  }
}

object PostgresAppConfigAdapter {

  def apply(tx: Transactor[IOLite])(
      implicit executionContext: ExecutionContext): PostgresAppConfigAdapter =
    new PostgresAppConfigAdapter(tx)

}
