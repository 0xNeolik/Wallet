package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}
import io.circe.Json
import org.scalacheck.Gen
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AbstractPostgresUserAdapterTest extends FlatSpec with Matchers {

  private object Generators {

    def genUser: Gen[User] = {
      for {
        id          <- Gen.uuid
        providerId  <- Gen.alphaStr
        providerKey <- Gen.alphaStr
      } yield
        User(
          id,
          LoginInfo(providerId, providerKey),
          LocalDirectoryData(),
          "identifier"
        )
    }
  }

  import Generators._

  private trait Fixture {
    val userId = UUID.randomUUID()
    val user   = genUser.sample.get

    def pgUser(empty: Boolean = false) = new AbstractPostgresUsersRepositoryAdapter() {
      override def save(user: User): Future[User] = Future.successful(user)
      override def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
        Future.successful {
          if (empty) None else Some(user.copy(userID = userId))
        }
      override def find(userID: UUID)                   = ???
      override def findByLoginKey(loginKey: String)     = ???
      override def findByIdentifier(identifier: String) = ???
      override def findAuth0(userID: UUID)              = ???
      override def storeSingleDataValue(userId: UUID,
                                        jsonKey: List[String],
                                        value: Json): Future[User]          = ???
      override def findByApiKey(api_key: UUID): Future[Option[User]]        = ???
      override def storeApiKey(userId: UUID, apiKey: UUID): Future[UUID]    = ???
      override def removeApiKey(userId: UUID, apikey: UUID): Future[UUID]   = ???
      override def findByquery(name: String, page: Int): Future[List[User]] = ???
    }
  }

  behavior of "AbstractPostgresUserAdapter"

  it should "Retrieve an existing user" in new Fixture {
    for {
      retrievedUser <- pgUser().store(user)
    } yield {
      retrievedUser shouldBe user.copy(userID = userId)
    }
  }

  it should "Create a new user" in new Fixture {
    for {
      savedUser <- pgUser(empty = true).store(user)
    } yield {
      savedUser shouldBe user
    }
  }
}
