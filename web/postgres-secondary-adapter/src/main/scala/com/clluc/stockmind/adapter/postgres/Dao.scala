package com.clluc.stockmind.adapter.postgres

import cats.syntax.either._
import doobie.imports._
import doobie.util.meta.Meta
import org.joda.time.DateTime
import fs2.interop.cats._
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

import scala.concurrent.{ExecutionContext, Future}

private[postgres] trait Dao {

  def transactor: Transactor[IOLite]

  implicit val executionContext: ExecutionContext

  // org.joda.time.DateTime support. Define how to convert between SQL timestamps and DateTime.
  implicit val DateTimeMeta: Meta[DateTime] =
    Meta[java.sql.Timestamp].xmap(
      ts => new DateTime(ts.getTime()),
      dt => new java.sql.Timestamp(dt.getMillis)
    )

  // scala.math.BigInt support. Doobie understands BigDecimal, so a quick conversion works.
  implicit val BigIntMeta: Meta[BigInt] = Meta[BigDecimal].xmap(_.toBigInt(), BigDecimal(_))

  // io.circe.Json support.
//  implicit val JsonMeta: Meta[Json] = Meta[String].xmap(str => parse(str).right.get, _.noSpaces)

  implicit val JsonMeta: Meta[Json] =
    Meta
      .other[PGobject]("jsonb")
      .xmap[Json](
        a => parse(a.getValue).leftMap[Json](e => throw e).merge, // failure raises an exception
        a => {
          val o = new PGobject
          o.setType("jsonb")
          o.setValue(a.noSpaces)
          o
        }
      )

  //
  // Query helper methods.
  //

  def selectOne[T](query: Query0[T]): Future[Option[T]] =
    Future { query.option.transact(transactor).unsafePerformIO }

  def selectMany[T](query: Query0[T]): Future[List[T]] =
    Future { query.process.list.transact(transactor).unsafePerformIO }

  def update(statement: Update0): Future[Unit] =
    Future { statement.run.transact(transactor).unsafePerformIO }

  def insertWithFeedback[T](insert: ConnectionIO[T]): Future[T] =
    Future { insert.transact(transactor).unsafePerformIO }
}
