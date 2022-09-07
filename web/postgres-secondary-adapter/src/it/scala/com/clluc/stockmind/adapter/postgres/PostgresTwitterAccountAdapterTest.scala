package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.adapter.postgres.Generators._
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresTwitterAccountAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  override def afterAll() = TableCleaner.clean()

  val pgUsers = new PostgresUsersRepositoryAdapter(testTransactor)
  val pgTwitterAccounts = new PostgresTwitterAccountAdapter(testTransactor)

  behavior of "PostgresTwitterAccountAdapter"

  it should "write a Twitter account to the DB" in {
    val user = genUser.sample.get
    val twitterAccount = genTwitterAccount(user.userID).sample.get
    for {
      _ <- pgUsers.save(user)
      savedTwitterAccount <- pgTwitterAccounts.saveTwitterAccount(twitterAccount)
    } yield savedTwitterAccount shouldBe twitterAccount
  }

  it should "not write a Twitter account whose UUID does not match any user" in {
    recoverToSucceededIf[PSQLException] {
      val twitterAccount = genTwitterAccount(UUID.randomUUID).sample.get
      pgTwitterAccounts.saveTwitterAccount(twitterAccount)
    }
  }

  it should "not find a Twitter account that is not there" in {
    val user = genUser.sample.get
    for {
      savedUser <- pgUsers.save(user)
      twitterAccount <- pgTwitterAccounts.findAccountById(savedUser.userID)
    } yield twitterAccount shouldBe None
  }

  it should "not find a Twitter account by screen name" in {
    val unknownTwitterAccount = genTwitterAccount(UUID.randomUUID).sample.get
    pgTwitterAccounts.findTwitterAccountByScreenName(unknownTwitterAccount.screenName).map(_ shouldBe None)
  }

  // TODO Review if this test makes sense or not anymore
  // Replace if appropriate with a suitable one
//  it should "find a Twitter account by screen name" in {
//    val user = genUser.sample.get
//    val twitterAccount = genTwitterAccount(user.userID).sample.get
//    for {
//      _ <- pgUsers.save(user)
//      savedTwitterAccount <- pgTwitterAccounts.saveTwitterAccount(twitterAccount)
//      foundTwitterAccount <- pgTwitterAccounts.findTwitterAccountByScreenName(savedTwitterAccount.screenName)
//    } yield foundTwitterAccount shouldBe Some(twitterAccount)
//  }

  it should "find all Twitter accounts' screen names" in {
    TableCleaner.deleteTwitterAccounts
    TableCleaner.deleteUsers

    val users = (1 to 10).map(_ => genUser.sample.get)
    val twitterAccounts = users.map(u => genTwitterAccount(u.userID).sample.get)
    Future.sequence {
      users.map(pgUsers.save)
      twitterAccounts.map(pgTwitterAccounts.saveTwitterAccount)
    }.flatMap(_ => pgTwitterAccounts.findAllScreenNames())
      .flatMap(names => names.length shouldBe 10)
  }

}
