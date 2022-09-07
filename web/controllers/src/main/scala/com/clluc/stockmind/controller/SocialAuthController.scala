package com.clluc.stockmind.controller

import java.util.UUID

import javax.inject.Inject
//import com.clluc.stockmind.core.SignUpConfiguration
import com.clluc.stockmind.core.auth.Auth0Config
//import com.clluc.stockmind.core.auth.{LoginInfo => CoreLoginInfo}
//import com.clluc.stockmind.core.transaction.TokenTransactionError
//import com.clluc.stockmind.core.transaction.TokenTransactionError.ValidatedTransaction
import com.clluc.stockmind.core.user.User
import com.clluc.stockmind.port.primary.{
  EthereumAccountOperationsPort,
  //SendTransactionPort,
  SocialAuthPort,
  UserPort
}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
//import com.mohiva.play.silhouette.api.{EventBus, Silhouette}
//import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
//import com.mohiva.play.silhouette.impl.providers.{OAuth1Info, SocialProviderRegistry}
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
//import BoundedContextImplicitConversions._
import play.api.cache.SyncCacheApi
import scala.util.matching.Regex

class SocialAuthController @Inject()(
    override val messagesApi: MessagesApi,
    //silhouette: Silhouette[DefaultEnv],
    val controllerComponents: ControllerComponents,
    val authInfoRepository: AuthInfoRepository,
    //val socialProviderRegistry: SocialProviderRegistry,
    accountOpsPort: EthereumAccountOperationsPort,
    //transactionsPort: SendTransactionPort,
    //configuration: SignUpConfiguration,
    socialAuthPort: SocialAuthPort,
    timestampFx: => DateTime,
    cache: SyncCacheApi,
    usersPort: UserPort,
    configuration: play.api.Configuration
)(
    implicit
    val executionContext: ExecutionContext
) extends BaseController
    with I18nSupport
    with LazyLogging
    with SocialAuthLogic {

  val UUIDPattern: Regex =
    "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}){1}".r

  /**
    * Authenticates a user against a social provider at server side. Flow description:
    * - Asks for a request token and it's corresponding secret to the Twitter API.
    * - Validates the request token, receiving an oauth token & secret.
    * - Execute the sign-up workflow with all it's actions:
    *   - Obtain the user twitter profile (using the twitter API), and create the user entity on database.
    *   - Store the twitter profile entity for that user on database.
    *   - Save oauth info for the user and relate it to the user & twitter accounts (already created).
    *   - Generates the JWT for the user.
    *   - Return it in a redirect call, that is a web intent meant to open the Stockmind app giving it the JWT
    *   for the logged user.
    *
    * @return The result to display.
    */
  def mobileAuth() = Action.async { implicit request =>
    // TODO This logic should be transactional. Either we do everything (on Twitter, Ethereum and our database) or nothing
    // Otherwise we might end up with uncomfortable inconsistencies
    // Created issue #486 for that
    authenticate(cache, configuration)
  }

  //Authenticated action to access API functions
  def AuthenticatedAction(f: (String) => Request[AnyContent] => Future[Result]) = {
    Action.async { request =>
      val idToken = request.session.get("idToken")
      idToken match {
        case Some(idTokenValue: String) =>
          val id = cache.get[UUID](idTokenValue + "id")
          id match {
            case Some(userID: UUID) =>
              f(idTokenValue)(request)
            case None =>
              Future.successful(Unauthorized("Authentication required"))
          }
        case None =>
          val api_key: Option[String] = request.getQueryString("api_key")
          api_key match {
            case Some(key: String) =>
              //check valid string for UUID
              UUIDPattern.findFirstMatchIn(key) match {
                case Some(_) =>
                  val keyUUID = UUID.fromString(key)
                  //check caché
                  cache.get[UUID](key + "id") match {
                    case Some(id) => //already cached
                      f(key)(request)
                    case None => //add to caché if exist
                      usersPort.findFromApiKey(keyUUID).flatMap {
                        //Idtoken is api_key now
                        case Some(user: User) => //found  add to caché
                          cache.set(key + "profile", user.directoryData)
                          cache.set(key + "id", user.userID)
                          f(key)(request)
                        case None => // not found
                          Future.successful(Unauthorized("Authentication required"))
                      }
                  }
                case None => Future.successful(Unauthorized("Wrong API key"))
              }
            case None =>
              Future.successful(Unauthorized("Authentication required"))
          }
      }

    }
  }

  def logout = AuthenticatedAction { idToken => request =>
    val config = Auth0Config.get(configuration)
    cache.remove(idToken + "profile")
    cache.remove(idToken + "id")

    Future(
      Redirect(
        String.format("https://%s/v2/logout?client_id=%s&returnTo=%s",
                      config.domain,
                      config.clientId,
                      config.logoutURL)).withNewSession)

  }
  //override type OurAuthenticator = JWTAuthenticator

  override def generateUserId() = UUID.randomUUID()

  override def storeNewUser(user: User) = socialAuthPort.storeNewUser(user)

  //override def retrieveTwitterInfo(twitterUserId: Long, authInfo: OAuth1Info) =
  //socialAuthPort.retrieveTwitterInfo(twitterUserId, authInfo.toCoreOauth1Info)

  //override def authenticatorService = silhouette.env.authenticatorService

  //override def handleAuthenticatorValue(authValue: String, intentSchema: String) =
  //Redirect(s"$intentSchema#token=$authValue", SEE_OTHER)

  override def createOrRetrieveEthereumAccountForUser(id: UUID) =
    accountOpsPort.createOrRetrieveAccountFor(id)

  //override def silhouetteEventBus: EventBus = silhouette.env.eventBus

}
