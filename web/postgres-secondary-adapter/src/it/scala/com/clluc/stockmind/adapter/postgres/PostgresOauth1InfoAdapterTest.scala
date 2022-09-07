package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.adapter.postgres.Generators._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class PostgresOauth1InfoAdapterTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  override def afterAll() = TableCleaner.clean()

  val pgOauth1Info = new PostgresOauth1InfoAdapter(testTransactor)

  behavior of "PostgresOauth1InfoAdapter"

  private def validOauth1Info = {
    val user = genUser.sample.get
    val oauth1Info = genOauth1Info.sample.get
    (user, oauth1Info)
  }

  it should "create a valid Oauth1Info correctly" in {
    val (user, oauth1Info) = validOauth1Info
    pgOauth1Info.saveOauthInfo(user.loginInfo, oauth1Info).map(_ => succeed)
  }

  it should "update the token information when there's an Oauth1Info with the same id-userid pair" in {
    val (user, oauth1Info) = validOauth1Info
    val login = user.loginInfo
    val newOauthInfo = genOauth1Info.sample.get
    for {
      o1 <- pgOauth1Info.saveOauthInfo(login, oauth1Info)
      oUpdated <- pgOauth1Info.saveOauthInfo(login, newOauthInfo)
    } yield oUpdated shouldBe newOauthInfo
  }

  it should "not find an Oauth1Info that is not there" in {
    val (user, oauth1Info) = validOauth1Info
    val login = user.loginInfo
    for {
      o1 <- pgOauth1Info.saveOauthInfo(login, oauth1Info)
      search <- pgOauth1Info.findByProviderIdAndKey(login.copy(providerID = "non-existing"))
    } yield search shouldBe None
  }

  it should "find an Oauth1Info by id and userid" in {
    val (user, oauth1Info) = validOauth1Info
    val login = user.loginInfo
    pgOauth1Info.saveOauthInfo(login, oauth1Info).flatMap { storedInfo =>
      pgOauth1Info.findByProviderIdAndKey(login).map(_ shouldBe Some(storedInfo))
    }
  }

}
