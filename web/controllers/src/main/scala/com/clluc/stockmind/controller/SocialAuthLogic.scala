package com.clluc.stockmind.controller

//import java.net.URLEncoder
import java.util.UUID
import java.security.SecureRandom
import java.math.BigInteger

import com.clluc.stockmind.core.auth.Auth0Config

//import cats.Apply
//import cats.instances.option._
//import com.clluc.stockmind.controller.SocialAuthLogic._
import com.clluc.stockmind.core.ethereum.CreateOrRetrieveEthAccountOutcome.CreateOrRetrieveAccountResult
//import com.clluc.stockmind.core.auth.LoginInfo
//import com.clluc.stockmind.core.transaction.TokenTransactionError.ValidatedTransaction
import com.clluc.stockmind.core.user.{User}
//import com.clluc.stockmind.silhouette.CustomSocialProfile
//import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
//import com.mohiva.play.silhouette.api.services.AuthenticatorService
//import com.mohiva.play.silhouette.api.{Authenticator, EventBus}
//import com.mohiva.play.silhouette.impl.providers._
//import com.mohiva.play.silhouette.impl.providers.oauth1.BaseTwitterProvider
//import io.circe.{Json, JsonObject}
//import io.circe.optics.JsonPath.root
//import io.circe.syntax._
//import play.api.mvc._
import play.api.cache.SyncCacheApi

import scala.concurrent.{ExecutionContext, Future}
//import scala.language.existentials

private[controller] trait SocialAuthLogic extends ErrorToResultConversions {

  //type OurSocialProvider = (sp#Self) forSome { type sp >: SocialProvider <: SocialProvider }

  //type OurAuthenticator <: Authenticator

  implicit def executionContext: ExecutionContext

  //def socialProviderRegistry: SocialProviderRegistry

  def generateUserId(): UUID

  def storeNewUser(user: User): Future[User]

  //def retrieveTwitterInfo(twitterUserId: Long, authInfo: OAuth1Info): Future[Json]

  //def authInfoRepository: AuthInfoRepository

  //def authenticatorService: AuthenticatorService[OurAuthenticator]

  //def handleAuthenticatorValue(authValue: OurAuthenticator#Value, intentSchema: String): Result

  //def silhouetteEventBus: EventBus

  def createOrRetrieveEthereumAccountForUser(id: UUID): Future[CreateOrRetrieveAccountResult]

  def authenticate(cache: SyncCacheApi, configuration: play.api.Configuration) = {

    val config = Auth0Config.get(configuration)
    // Generate random state parameter
    object RandomUtil {
      private val random = new SecureRandom()

      def alphanumeric(nrChars: Int = 24): String = {
        new BigInteger(nrChars * 5, random).toString(32)
      }
    }
    val state    = RandomUtil.alphanumeric()
    var audience = config.audience
    cache.set("state", state)
    if (config.audience == "") {
      audience = String.format("https://%s/userinfo", config.domain)
    }

    val query = String.format(
      "authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=openid profile&audience=%s&state=%s",
      config.clientId,
      config.callbackURL,
      audience,
      state
    )
    Future.successful(Redirect(String.format("https://%s/%s", config.domain, query)))
  }
  ///////

  /*def userFromProfile(p: SocialProfile): User = {
      val dirData = p match {
        case _: CommonSocialProfile =>
          LocalDirectoryData()
        case profile: CustomSocialProfile =>
          val casJson = profile.casAttributes.asJson
          val standardizedTwitterAttributes = {
            val avatarLens     = root.profile_image_url.string
            val screenNameLens = root.screen_name.string
            val fullNameLens   = root.name.string
            Apply[Option].map3(avatarLens.getOption(casJson),
                               screenNameLens.getOption(casJson),
                               fullNameLens.getOption(casJson)) { (avatar, screenName, fullName) =>
              Map("avatar_url" -> avatar.replace("_normal.jpg", ".jpg"), // Grab full-size image
                  "screenname" -> screenName,
                  "fullname"   -> fullName).asJson
            }
          }
          val json = standardizedTwitterAttributes.map(casJson.deepMerge).getOrElse(casJson)

          LocalDirectoryData(json)
        case _: SocialProfile =>
          LocalDirectoryData()
      }

      User(generateUserId(), p.loginInfo.toCoreLoginInfo, dirData)
    }*/

  /*val intentSchema = request.queryString
      .get(INTENT_SCHEMA_PARAMETER_NAME)
      .flatMap(_.headOption)
      .getOrElse("ptblockchain://ptblockchain.wallet.com") // For now we use the Bankia default value; this way we don't need to break backwards compatibility
   */
  /*val socialProvider: Option[OurSocialProvider] = {
      val _sp = socialProviderRegistry.get[SocialProvider](providerName).map {
        case tp: BaseTwitterProvider =>
          tp.withSettings { settings =>
            settings.copy(
              callbackURL = settings.callbackURL + s"?$INTENT_SCHEMA_PARAMETER_NAME=${URLEncoder
                .encode(intentSchema, "UTF-8")}"
            )
          }
        case cp: BaseCasProvider => cp.withSettings(cp => cp) // placeholder
        case other               => other
      }
      _sp.map(_.asInstanceOf[OurSocialProvider])
    }

    socialProvider match {
      case Some(p: SocialProvider) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)

          case Right(authInfo) =>
            def loginProcess(getAdditionalInfo: (User) => Future[Json]) = {
              for {
                profile <- p.retrieveProfile(authInfo)
                user = userFromProfile(profile)
                additionalInfo <- getAdditionalInfo(user)
                userWithExtraInfo = user.copy(
                  directoryData =
                    LocalDirectoryData(user.directoryData.data.deepMerge(additionalInfo)))
                newUser <- storeNewUser(userWithExtraInfo)
                // The following cannot be done with an already stored user
                _ <- {
                  val maybeEthAccount = createOrRetrieveEthereumAccountForUser(newUser.userID)
                  maybeEthAccount.map {
                    case Left(error) =>
                      Future.failed(new RuntimeException(error.toString)) // This way play show the internal server error page, and the message is logged. No further action is taken and no DB inconsistencies are created
                    case _ => Future.successful(())
                  }
                }
                loginInfo = profile.loginInfo
                _                  <- authInfoRepository.save(loginInfo, authInfo)
                authenticator      <- authenticatorService.create(loginInfo)
                authenticatorValue <- authenticatorService.init(authenticator)
                result <- authenticatorService.embed(
                  authenticatorValue,
                  handleAuthenticatorValue(authenticatorValue, intentSchema))
                pendingsSettlement <- settlePendingTransfers(loginInfo.toCoreLoginInfo)
                _ = reportPendingTransfersSettlement(pendingsSettlement)
              } yield result

            }

            // This is a hook we can use to grab additional info we cannot obtain directly from
            // the user profile. If everything goes according to plan we shouldn't need it, but
            // it doesn't hurt to leave it here just in case.
            authInfo match {
              case oauth1Info: OAuth1Info =>
                loginProcess(_ => Future.successful(Json.fromJsonObject(JsonObject.empty)))
              case casInfo: CasInfo =>
                loginProcess(_ => Future.successful(Json.fromJsonObject(JsonObject.empty)))
              case otherAuthInfo =>
                loginProcess(_ => Future.successful(Json.fromJsonObject(JsonObject.empty)))
            }
        }

      case None =>
        Future.successful(Redirect(String.format("https://%s/%s", config.domain, query)))
       Future.successful(ImATeapot(
          s"The provider $providerName does not offer a social profile and is not supported by this API"))
    }*/

}

/*private object SocialAuthLogic {
  val INTENT_SCHEMA_PARAMETER_NAME = "schema"
}*/
