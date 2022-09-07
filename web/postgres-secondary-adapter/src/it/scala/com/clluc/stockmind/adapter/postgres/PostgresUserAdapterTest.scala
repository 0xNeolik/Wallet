package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.adapter.postgres.Generators._
import io.circe.syntax._
import io.circe.parser.parse
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

class PostgresUserAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  override def afterAll() = TableCleaner.clean()

  val pgUsers = new PostgresUsersRepositoryAdapter(testTransactor)

  behavior of "PostgresUserAdapter"

  it should "write a User to the DB" in {
    val user = genUser.sample.get
    pgUsers.save(user).map(_ shouldBe user)
  }

  it should "not find a user that is not there (by LoginInfo)" in {
    val user = genUser.sample.get
    for {
      _ <- pgUsers.save(user)
      foundUser <- pgUsers.retrieve(user.loginInfo.copy(providerID = "non-existing-provider-id"))
    } yield foundUser shouldBe None
  }

  it should "not find a user that is not there (by id)" in {
    val user = genUser.sample.get
    for {
      _ <- pgUsers.save(user)
      foundUser <- pgUsers.find(java.util.UUID.randomUUID)
    } yield foundUser shouldBe None
  }

  it should "find a User by login info" in {
    val user = genUser.sample.get
    for {
      _ <- pgUsers.save(user)
      foundUser <- pgUsers.retrieve(user.loginInfo)
      f <- pgUsers.retrieve(user.loginInfo)
    } yield foundUser shouldBe Some(user)
  }

  it should "find a User by id" in {
    val user = genUser.sample.get
    for {
      _ <- pgUsers.save(user)
      foundUser <- pgUsers.find(user.userID)
    } yield foundUser shouldBe Some(user)
  }

  it should "find a User by login key" in {
    val user = genUser.sample.get
    for {
      _ <- pgUsers.save(user)
      u <- pgUsers.save(user)
      f <- pgUsers.findByLoginKey(u.loginInfo.providerKey)
    } yield f shouldBe Some(user)
  }

  it should "add a new key to a users' data" in {
    val data = genLocalDirectoryData().sample.get
    val user = genUser.sample.get.copy(directoryData = data)
    val value = "aValue".asJson
    for {
      _ <- pgUsers.save(user)
      modified <- pgUsers.storeSingleDataValue(user.userID, List("somenewkey"), value)
      expectedJson = modified.directoryData.data.deepMerge(parse("""{"somenewkey":"aValue"}""").right.get)
    } yield modified.directoryData.data shouldBe expectedJson
  }

}
