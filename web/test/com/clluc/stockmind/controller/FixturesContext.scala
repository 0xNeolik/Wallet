package com.clluc.stockmind.controller

import java.util.UUID

import com.clluc.stockmind.core.auth.{LoginInfo => CoreLoginInfo}
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo => SilhouetteLoginInfo}
import com.mohiva.play.silhouette.test.FakeEnvironment
import modules.{BaseModule, JobModule}
import net.codingwell.scalaguice.ScalaModule
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import com.mohiva.play.silhouette.test._
import io.circe
import circe._
import circe.parser.parse
import com.clluc.stockmind.core.user.LocalDirectoryData
import com.mohiva.play.silhouette.impl.providers.{OAuth1Info => SilhouetteOAuth1Info}
import org.scalatest.{Assertion, Matchers}
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.time.{Milliseconds, Span}
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait FixturesContext extends Matchers {

  val userId = UUID.randomUUID()

  val loginInfo = SilhouetteLoginInfo("facebook", "user@facebook.com")

  val oauth1Info = SilhouetteOAuth1Info("token", "secret")

  private val printer = Printer.noSpaces.copy(dropNullKeys = true)

  def callActionOnControllerInstanceWithEmptyRequest[T: ClassTag](
      additionalModules: GuiceableModule*
  )(
      action: T => Action[AnyContent]
  ): Future[Result] =
    callActionOnControllerInstance(additionalModules: _*)(FakeRequest())(action)

  def callActionOnControllerInstance[T: ClassTag, R <: AnyContent](
      additionalModules: GuiceableModule*
  )(
      request: FakeRequest[R]
  )(
      action: T => Action[AnyContent]
  ): Future[Result] = {

    val user = SilhouetteUser(
      userID = userId,
      loginInfo = CoreLoginInfo("facebook", "user@facebook.com"),
      directoryData = LocalDirectoryData()
    )

    implicit val executionContext: ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global
    implicit val fakeEnv: Environment[DefaultEnv] =
      new FakeEnvironment[DefaultEnv](List(loginInfo -> user))

    class EnvModule extends AbstractModule with ScalaModule {
      override def configure() = {
        bind[Environment[DefaultEnv]].toInstance(fakeEnv)
      }
    }

    val controllerInstance: T = {

      val app: Application = {
        val appBuilder = new GuiceApplicationBuilder()
          .disable[JobModule]
          .disable[BaseModule]
          .overrides(new EnvModule)

        val overriddenBuilder = additionalModules.foldLeft(appBuilder) { (builder, module) =>
          builder.overrides(module)
        }

        overriddenBuilder.build()
      }

      app.injector.instanceOf[T]
    }

    action(controllerInstance).apply(request.withAuthenticator(loginInfo))
  }

  def plainResponseBody(eventualResult: Future[Result]): String = {
    contentAsBytes(eventualResult).utf8String
  }

  def assertOnEventualResult[T](result: Future[T])(assertFx: T => Assertion): Assertion = {
    val timeout = PatienceConfiguration.Timeout(Span(20000L, Milliseconds))

    whenReady(result, timeout) { response =>
      assertFx(response)
    }
  }

  def assertOnStatusCode(result: Future[Result])(expectedStatus: Int): Assertion =
    assertOnEventualResult[Result](result) {
      _.header.status shouldBe expectedStatus
    }

  def jsonFromResponseBody(eventualResult: Future[Result]): Either[ParsingFailure, Json] =
    parse(plainResponseBody(eventualResult))

  def resultAsJsonStringWithoutNulls(
      eventualResult: Future[Result]): Either[ParsingFailure, String] = {
    val json = jsonFromResponseBody(eventualResult)

    json.map(printer.pretty)
  }

  def jsonToStringWithoutNulls(json: Json): String = {
    printer.pretty(json)
  }
}
