package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.Block
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AbstractPostgresAppConfigAdapterTest extends FlatSpec with Matchers {

  object Fixture {
    val storedBlock  = Block(42)
    val initialBlock = Block(0)

    val appconfig = new AbstractPostgresAppConfigAdapter {
      override def set(key: String, value: String) = Future.successful(value)
      override def get(key: String)                = Future.successful(Some(storedBlock.blockNumber.toString))
    }

    val emptyAppconfig = new AbstractPostgresAppConfigAdapter {
      override def set(key: String, value: String) = ???
      override def get(key: String)                = Future.successful(None)
    }
  }

  behavior of "AbstractPostgresAppConfigAdapter"

  it should "get a Block" in {
    for {
      block <- Fixture.appconfig.getBlock("something")
    } yield {
      block shouldBe Fixture.storedBlock
    }
  }

  it should "get a Block number '0' when there is no block stored" in {
    for {
      block <- Fixture.emptyAppconfig.getBlock("something")
    } yield {
      block shouldBe Fixture.initialBlock
    }
  }

  it should "set a Block" in {
    val block = Block(123)
    for {
      savedBlock <- Fixture.appconfig.setBlock("something", block)
    } yield block shouldBe savedBlock
  }

}
