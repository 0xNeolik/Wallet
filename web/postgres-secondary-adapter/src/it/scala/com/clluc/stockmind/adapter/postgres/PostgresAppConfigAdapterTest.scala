package com.clluc.stockmind.adapter.postgres

import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class PostgresAppConfigAdapterTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  private object Generators {
    def genConfigKey: Gen[(String, String)] =
      for {
        key <- Gen.alphaStr.retryUntil(_.nonEmpty)
        value <- Gen.alphaStr
      } yield key -> value
  }

  override def afterAll() = TableCleaner.clean()

  val pgAppConfig = new PostgresAppConfigAdapter(testTransactor)

  behavior of "PostgresAppConfigAdapter"

  import Generators._

  it should "write a key" in {
    val (key, value) = genConfigKey.sample.get
    pgAppConfig.set(key, value).map(_ => succeed)
  }

  it should "read a previously written key" in {
    val (key, value) = genConfigKey.sample.get
    for {
      _ <- pgAppConfig.set(key, value)
      readValue <- pgAppConfig.get(key)
    } yield readValue shouldBe Some(value)
  }

  it should "overwrite a key" in {
    val (key, value) = genConfigKey.sample.get
    val (_, anotherValue) = genConfigKey.sample.get
    for {
      _ <- pgAppConfig.set(key, value)
      _ <- pgAppConfig.set(key, anotherValue)
      readValue <- pgAppConfig.get(key)
    } yield readValue shouldBe Some(anotherValue)
  }

}
