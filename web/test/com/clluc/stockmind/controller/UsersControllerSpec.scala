package com.clluc.stockmind.controller

import java.util.UUID

import com.clluc.stockmind.controller.UsersController._
import com.clluc.stockmind.controller.UsersControllerSpec.Context
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.ethereum.{Erc20Token, Erc721Token}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user._
import com.clluc.stockmind.port.primary.{Oauth1InfoPort, UserPort}
import com.google.inject.{AbstractModule, Provides}

import io.circe.generic.auto._
import io.circe.syntax._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.cache.SyncCacheApi
import javax.inject.Inject
import play.api.http.Status
import net.codingwell.scalaguice.ScalaModule
import cats.data.EitherT
import cats.instances.future._

class UsersControllerSpec @Inject()(cache: SyncCacheApi)
    extends FunSpec
    with Matchers
    with EitherValues
    with Context {

  //Add user data caché for tests
  val sourceUser =
    User(UUID.randomUUID(), LoginInfo("11", "aa"), LocalDirectoryData(), "identifier")
  cache.set("IDTOKEN" + "profile", sourceUser)
  cache.set("IDTOKEN" + "id", sourceUser.userID)

  describe("GET /v1/users/me") {
    describe("When there's valid user info belonging to him") { // Happy path

      val directoryData =
        Map[String, String]("aKey" -> "aValue", "anotherKey" -> "anotherValue").asJson

      val userInfoFixture = UserInfo(
        directoryData,
        List(
          Balance(
            ethAddress = Address.default,
            token = Erc20Token(
              "TKN",
              "ERC-20",
              "A sample token",
              8,
              Address.default,
              None,
              None
            ),
            totalSent = 100,
            totalReceived = 200,
            totalWithheld = 0,
            realBalance = 100,
            effectiveBalance = 100
          ),
        ),
        List(
          Erc721Token(
            "TKN",
            "ERC-721",
            "A sample token",
            "metadata",
            BigInt("71120786848863412851373030999642871879808768922518165984257232620739138279176"),
            Address.default,
            None,
            None
          )
        )
      )

      val eventualResult = findUserInfoThroughController(Right(userInfoFixture))

      it("return ok") {
        assertOnStatusCode(eventualResult)(Status.OK)
      }

      it("Give us the expected JSON response") {
        jsonFromResponseBody(eventualResult).right.value shouldBe toUserInfoView(userInfoFixture).asJson
      }
    }

    // Here, error use cases
    describe("In a not happy path use case") {

      def noHappyPathCase[T](error: T => UserOperationError, parameter: T): Unit = {
        val eventualResult = findUserInfoThroughController(Left(error(parameter)))

        it("We get a 500 error") {
          assertOnStatusCode(eventualResult)(Status.INTERNAL_SERVER_ERROR)
        }

        it("The error description matches the expected") {
          plainResponseBody(eventualResult) shouldBe error(parameter).message()
        }
      }

      describe("When the user has no ethereum account in the system") {
        noHappyPathCase[UUID](UserWithoutEthereumAccountInSystem, userId)
      }

      describe("When the user does not have a local directory entry stored") {
        noHappyPathCase[LocalDirectoryEntryId](UserDoesNotHaveLocalDirectoryEntry,
                                               LocalDirectoryEntryId("provider", "key"))
      }

      describe("The user has in balance a token not in the system") {
        noHappyPathCase[UUID](TokenFoundInBalanceDoesNotExist, userId)
      }

      describe("Something regarding IO goes south") {
        noHappyPathCase[String](IOError, "This didn't go well")
      }
    }
  }

  describe("GET /v1/users/:query?page=0") {

    val userInfoFixture =
      List(User(UUID.randomUUID(), LoginInfo("11", "aa"), LocalDirectoryData(), "identifier"))

    val eventualResult = findUsersInfoThroughController(userInfoFixture)

    it("return ok") {
      assertOnStatusCode(eventualResult)(Status.OK)
    }

    it("Give us the expected JSON response") {
      jsonFromResponseBody(eventualResult).right.value shouldBe toUsersInfoView(userInfoFixture).asJson
    }
  }
}

private[controller] object UsersControllerSpec {

  trait Context extends FixturesContext with MockFactory {

    class FakeModule(userInfoMock: Either[UserOperationError, UserInfo])
        extends AbstractModule
        with ScalaModule {
      override def configure() = {}

      @Provides
      def oauth1InfoPort(): Oauth1InfoPort = mock[Oauth1InfoPort]

      @Provides
      def userPort(): UserPort =
        new UserPort {
          override def findUserInfo(userId: UUID, data: LocalDirectoryData) =
            EitherT.fromEither[Future](userInfoMock)

          override def findFromLoginInfo(loginInfo: LoginInfo) = ???

          override def findFromId(userId: UUID): Future[Option[User]]               = ???
          override def findFromApiKey(api_key: UUID): Future[Option[User]]          = ???
          override def setApiKey(userId: UUID, apikey: UUID): Future[UUID]          = ???
          override def deleteApiKey(userId: UUID, apikey: UUID): Future[UUID]       = ???
          override def findUsersByName(name: String, page: Int): Future[List[User]] = ???
        }
    }

    class FakeModule2(userInfoMock: Future[List[User]]) extends AbstractModule with ScalaModule {
      override def configure() = {}

      @Provides
      def userPort(): UserPort =
        new UserPort {
          override def findUserInfo(userId: UUID, data: LocalDirectoryData) = ???

          override def findFromLoginInfo(loginInfo: LoginInfo) = ???

          override def findFromId(userId: UUID): Future[Option[User]]               = ???
          override def findFromApiKey(api_key: UUID): Future[Option[User]]          = ???
          override def setApiKey(userId: UUID, apikey: UUID): Future[UUID]          = ???
          override def deleteApiKey(userId: UUID, apikey: UUID): Future[UUID]       = ???
          override def findUsersByName(name: String, page: Int): Future[List[User]] = (userInfoMock)
        }
    }

    def findUserInfoThroughController(userInfoMock: Either[UserOperationError, UserInfo]) =
      callActionOnControllerInstanceWithEmptyRequest[UsersController](
        new FakeModule(userInfoMock)
      )(_.findUserInfo)

    def findUsersInfoThroughController(userInfoMock: List[User]) =
      callActionOnControllerInstanceWithEmptyRequest[UsersController](
        new FakeModule2(Future(userInfoMock))
      )(_.queryUser("identifier", 0))
  }

}
