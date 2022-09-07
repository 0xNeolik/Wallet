package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.port.secondary.TransactionMetaInfPort
import doobie.imports._
import fs2.interop.cats._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Secondary adapter that takes care of operations on Postgres regarding transactions meta-inf
  * @param transactor
  * @param executionContext
  */
private[postgres] class PostgresTransferMetadataAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext
) extends TransactionMetaInfPort
    with Dao {

  override def saveMetaInf(txId: Long, metaInf: Map[String, String]) = {

    case class MetaInfTuple(txId: Long, key: String, value: String)

    val metaInfTuples: List[MetaInfTuple] =
      metaInf.foldLeft(List.empty[MetaInfTuple]) { (acc, keyValueTuple) =>
        MetaInfTuple(txId, keyValueTuple._1, keyValueTuple._2) :: acc
      }

    def saveStatement(metaInf: MetaInfTuple): ConnectionIO[Int] = sql"""
        INSERT INTO transfers_metadata
          (tx_id, key, value)
        VALUES
        (${metaInf.txId}, ${metaInf.key}, ${metaInf.value})
      """.update.run

    import cats.syntax.applicative._
    // Create a program that represents the sequence of insert statements
    // One insert statement per entry in the meta-inf map
    val saveStatements: ConnectionIO[Int] =
      metaInfTuples.foldLeft(0.pure[ConnectionIO]) { (acc, metaInf) =>
        acc.flatMap(_ => saveStatement(metaInf))
      }

    // "Effectfully" interpret the previous sequence of statements in the same transaction
    Future {
      saveStatements.transact(transactor).unsafePerformIO
    }.map(_ => ()) // Map Future[Int] to Future[Unit], which is the declared return type for the corresponding port
  }

  override def readMetaInf(txId: Long) = {

    case class TransactionMetaInf(key: String, value: String)

    def query(): Query0[TransactionMetaInf] = sql"""
     SELECT key, value
     FROM transfers_metadata
     WHERE tx_id=$txId
    """.query[TransactionMetaInf]

    selectMany[TransactionMetaInf](query()).map(_.foldLeft(Map.empty[String, String]) {
      (acc, txMetaInf) =>
        acc + (txMetaInf.key -> txMetaInf.value)
    })
  }

}

object PostgresTransferMetadataAdapter {

  def apply(
      transactor: Transactor[IOLite]
  )(
      implicit executionContext: ExecutionContext
  ): PostgresTransferMetadataAdapter = new PostgresTransferMetadataAdapter(transactor)
}
