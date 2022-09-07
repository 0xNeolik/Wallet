package com.clluc.stockmind.controller

import com.clluc.stockmind.controller.TokensControllerSpec.Context
import com.clluc.stockmind.core.ethereum.{Erc20Token, Erc721Token, Ethtoken}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.port.primary.TokensPort
import com.clluc.stockmind.core.token.AllTokensInfo
import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, FunSpec, Matchers}
import io.circe.generic.auto._
import io.circe.syntax._
import play.api.http.Status
import play.api.inject.guice.GuiceableModule

import scala.concurrent.Future

import javax.inject.Inject
import play.api.cache.SyncCacheApi
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}
import java.util.UUID

class TokensControllerSpec @Inject()(cache: SyncCacheApi)
    extends FunSpec
    with Matchers
    with Context
    with EitherValues {
  describe("GET /v1/tokens") {
    //Add user data caché for tests
    val sourceUser =
      User(UUID.randomUUID(), LoginInfo("11", "aa"), LocalDirectoryData(), "identifier")
    cache.set("IDTOKEN" + "profile", sourceUser)
    cache.set("IDTOKEN" + "id", sourceUser.userID)
    describe("While in the happy path (tokens retrieval op goes well)") {
      val supportedTokens = List(
        Erc20Token("SMD", "ERC-20", "Stockmind", 8, Address("0" * 40), None, None)
          .asInstanceOf[Ethtoken],
        Erc20Token("FUT", "ERC-20", "Future", 10, Address("1" * 40), None, None)
          .asInstanceOf[Ethtoken]
      )
      val supportedTokens721 = List(
        Erc721Token(
          "SMD",
          "ERC-721",
          "Stockmind",
          "metadata",
          BigInt("71120786848863412851373030999642871879808768922518165984257232620739138279176"),
          Address("0" * 40),
          None,
          None
        ).asInstanceOf[Ethtoken],
        Erc721Token(
          "FUT",
          "ERC-721",
          "Future",
          "metadata",
          BigInt("81120786848863412851373030999642871879808768922518165984257232620739138279176"),
          Address("1" * 40),
          None,
          None).asInstanceOf[Ethtoken]
      )
      val allTokensSupport = AllTokensInfo(supportedTokens, supportedTokens721)

      val eventualResult = findSupportedTokensThroughController(allTokensSupport)

      it("return ok") {
        assertOnStatusCode(eventualResult)(Status.OK)
      }

      it("We receive a correct view of the tokens") {
        jsonFromResponseBody(eventualResult).right.value shouldBe allTokensSupport
        TokensController.toTokensView(allTokensSupport).asJson
      }
    }

    describe("When something goes south retrieving the tokens") {

      val exceptionMsg   = "Something went south here, bad smell. Did you fart?"
      val eventualResult = getErrorWhileFindingSupportedTokens(exceptionMsg)

      it("We get the exception") {
        val assertion = assertOnEventualResult(eventualResult.failed)(_)

        assertion {
          _.getMessage shouldBe exceptionMsg
        }
      }
    }
  }
}

private[controller] object TokensControllerSpec {

  trait Context extends FixturesContext with MockFactory {

    class FakeModule(supportedTokens: AllTokensInfo) extends AbstractModule with ScalaModule {
      override def configure() = {}

      @Provides
      def tokensPort(): TokensPort = {
        val _mock = mock[TokensPort]
        (_mock.allTokensInfo _)
          .expects()
          .returning(Future.successful(supportedTokens))
          .noMoreThanOnce()
        _mock
      }
    }

    class ExceptionFakeModule(exceptionMsg: String) extends AbstractModule with ScalaModule {
      override def configure() = {}

      @Provides
      def tokensPort(): TokensPort = {
        val _mock = mock[TokensPort]
        (_mock.allTokensInfo _)
          .expects()
          .returning(Future.failed(new Exception(exceptionMsg)))
          .noMoreThanOnce()
        _mock
      }
    }

    private def callAction(module: GuiceableModule) =
      callActionOnControllerInstanceWithEmptyRequest[TokensController](module)(_.supportedTokens())

    def findSupportedTokensThroughController(allTokensSupport: AllTokensInfo) =
      callAction(new FakeModule(allTokensSupport))

    def getErrorWhileFindingSupportedTokens(errorMsg: String) =
      callAction(new ExceptionFakeModule(errorMsg))

  }
}
