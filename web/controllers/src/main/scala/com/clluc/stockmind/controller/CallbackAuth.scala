package com.clluc.stockmind.controller

import java.util.UUID

import com.clluc.stockmind.core.auth.{Auth0Config, LoginInfo, OAuth1Info}
import java.util.UUID
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}
import com.clluc.stockmind.port.primary.{
  EthereumAccountOperationsPort,
  Oauth1InfoPort,
  SocialAuthPort
}
import com.clluc.stockmind.port.primary.TwitterPort
import com.clluc.stockmind.core.twitter.TwitterAccount

import scala.concurrent.{ExecutionContext, Future}
import play.api.i18n.{I18nSupport}
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.cache.SyncCacheApi
import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.json.JsValue
import com.clluc.stockmind.core.transaction.TokenTransactionError
import com.clluc.stockmind.core.transaction.TokenTransactionError.ValidatedTransaction
import com.clluc.stockmind.port.primary.SendTransactionPort
import org.joda.time.DateTime
import com.google.inject.name.Named

import scala.util.{Failure, Success}

class CallbackAuth @Inject()(cache: SyncCacheApi,
                             ws: WSClient,
                             configuration: play.api.Configuration,
                             accountOpsPort: EthereumAccountOperationsPort,
                             val controllerComponents: ControllerComponents,
                             socialAuthPort: SocialAuthPort,
                             transactionsPort: SendTransactionPort,
                             timestampFx: => DateTime,
                             oauthPort: Oauth1InfoPort,
                             @Named("auth0Method") auth0Method: String,
                             twitterAccountPort: TwitterPort)(
    implicit
    val executionContext: ExecutionContext)
    extends BaseController
    with I18nSupport
    with LazyLogging {

  def generateUserId()         = UUID.randomUUID()
  def storeNewUser(user: User) = socialAuthPort.storeNewUser(user)

  def createOrRetrieveEthereumAccountForUser(id: UUID) =
    accountOpsPort.createOrRetrieveAccountFor(id)
  var login: LoginInfo       = _
  var userScreenName: String = _

  def userFromProfile(user: JsValue): User = {
    val dirData = user match {
      case user: JsValue =>
        val jsonFromFields = io.circe.Json.fromFields(
          List(
            ("avatar_url",
             io.circe.Json
               .fromString(((user \ "picture").as[String]).replace("_normal.JPG", ".jpg"))),
            ("screenname", io.circe.Json.fromString((user \ "nickname").as[String])),
            ("fullname", io.circe.Json.fromString((user \ "name").as[String]))
          ))
        val subArray = (user \ "sub").as[String].split('|')
        userScreenName = (user \ "name").as[String]
        login = LoginInfo(subArray(0), subArray(1))
        LocalDirectoryData(jsonFromFields)
    }

    User(generateUserId(), login, dirData, userScreenName)
  }

  def callback(codeOpt: Option[String] = None, stateOpt: Option[String] = None) = Action.async {
    if (stateOpt == cache.get("state")) {
      (for {
        code <- codeOpt
      } yield {
        getToken(code)
          .flatMap {
            case (idToken, accessToken) =>
              getUser(accessToken).map { user =>
                //crear user e introducir en bbdd
                val us: User = userFromProfile(user)
                storeNewUser(us) match {
                  case usFut: Future[User] =>
                    usFut.onComplete {
                      case Success(newUser: User) =>
                        if (newUser.loginInfo.providerID == "twitter") {
                          val cursor: io.circe.HCursor = newUser.directoryData.data.hcursor
                          val avatar = cursor.downField("avatar_url").as[String] match {
                            case Left(_)  => ""
                            case Right(a) => a.toString()
                          }
                          getUserTwitterInfo(newUser.loginInfo.providerKey).map { twitterUserInfo =>
                            val identities = (twitterUserInfo \ "identities" \ 0).get
                            twitterAccountPort.saveTwitterAccount(
                              TwitterAccount(
                                newUser.userID,
                                newUser.loginInfo.providerKey.toLong,
                                (twitterUserInfo \ "screen_name").as[String],
                                false,
                                0,
                                Option(avatar)
                              ))
                            oauthPort.save(
                              OAuth1Info((identities \ "access_token").as[String],
                                         (identities \ "access_token_secret").as[String]),
                              newUser.loginInfo)

                          }
                        }
                        //add profile data to cache
                        cache.set(idToken + "profile", user)
                        cache.set(idToken + "id", newUser.userID)
                        // The following cannot be done with an already stored user
                        val maybeEthAccount = createOrRetrieveEthereumAccountForUser(newUser.userID)
                        maybeEthAccount.map {
                          case Left(error) =>
                            Future.failed(new RuntimeException(error.toString)) // This way play show the internal server error page, and the message is logged. No further action is taken and no DB inconsistencies are created
                          case _ => Future.successful(())
                        }
                        val pendingTransfers = settlePendingTransfers(newUser)
                        pendingTransfers.map { list =>
                          reportPendingTransfersSettlement(list)
                        }

                      case Failure(ex) => println("ERROR occured - " + ex.getMessage)
                    }

                }

                //Redirect with session
                Ok("ok")
                  .withSession(
                    "idToken"     -> idToken,
                    "accessToken" -> accessToken
                  )

              }

          }
          .recover {
            case ex: IllegalStateException => Unauthorized(ex.getMessage)
          }
      }).getOrElse(Future.successful(BadRequest("No parameters supplied")))
    } else {
      Future.successful(BadRequest("Invalid state parameter"))
    }
  }

  def getToken(code: String): Future[(String, String)] = {

    val config   = Auth0Config.get(configuration)
    var audience = config.audience
    if (config.audience == "") {
      audience = String.format("https://%s/userinfo", config.domain)
    }
    val tokenResponse = ws
      .url(String.format("https://%s/oauth/token", config.domain))
      .withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(
        Json.obj(
          "client_id"     -> config.clientId,
          "client_secret" -> config.secret,
          "redirect_uri"  -> config.callbackURL,
          "code"          -> code,
          "grant_type"    -> "authorization_code",
          "audience"      -> audience
        )
      )

    tokenResponse.flatMap { response =>
      (for {
        idToken     <- (response.json \ "id_token").asOpt[String]
        accessToken <- (response.json \ "access_token").asOpt[String]
      } yield {
        Future.successful((idToken, accessToken))
      }).getOrElse(Future.failed[(String, String)](new IllegalStateException("Tokens not sent")))
    }

  }

  def getUser(accessToken: String): Future[JsValue] = {
    val config = Auth0Config.get(configuration)
    val userResponse = ws
      .url(String.format("https://%s/userinfo", config.domain))
      .withQueryStringParameters("access_token" -> accessToken)
      .get()

    userResponse.flatMap(response => Future.successful(response.json))
  }

  def getUserTwitterInfo(provKey: String): Future[JsValue] = {
    val config = Auth0Config.get(configuration)

    //get Credentials
    val tokenResponse = ws
      .url(String.format("https://%s/oauth/token", config.domain))
      .withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(
        Json.obj(
          "client_id"     -> config.clientId,
          "client_secret" -> config.secret,
          "grant_type"    -> "client_credentials",
          "audience"      -> config.audience
        )
      )
    tokenResponse.flatMap { response =>
      (for {
        accessToken <- (response.json \ "access_token").asOpt[String]
      } yield {
        val userResponse = ws
          .url(String.format("https://%s/api/v2/users/twitter|%s", config.domain, provKey))
          .addHttpHeaders("authorization" -> (s"Bearer $accessToken"))
          .get()

        userResponse.flatMap { userResponse =>
          //val screenname = (userResponse.json \ "screen_name").as[String]
          // val accessToken = (userResponse.json \ "identities" \ "access_token").as[String]
          //val accessTokenSecret =(userResponse.json \ "identities" \ "access_token_secret").as[String]
          //val providerId  = (userResponse.json \ "identities" \ "provider").as[String]
          //val providerKey = (userResponse.json \ "identities" \ "user_id").as[String]
          // oauthPort.save(OAuth1Info(accessToken, accessTokenSecret), LoginInfo("twitter", provKey))
          Future.successful(userResponse.json)
        }
      }).getOrElse(Future.failed[(JsValue)](
        new IllegalStateException("Error getting credentials access token")))
    }

  }

  def settlePendingTransfers(user: User): Future[List[ValidatedTransaction[Unit]]] =
    auth0Method match {
      case "TWITTER" =>
        transactionsPort.settlePendingTransfers(user.loginInfo.providerID,
                                                user.loginInfo.providerKey,
                                                timestampFx)
      case "USER_PASS" =>
        transactionsPort.settlePendingTransfersUser(user.loginInfo.providerID,
                                                    user.identifier,
                                                    timestampFx)
    }

  def reportPendingTransfersSettlement(settlementResults: List[ValidatedTransaction[Unit]]) = {

    def logValidationErrorInSettleAction(error: TokenTransactionError): Unit =
      logger.error(error.message())

    settlementResults.map {
      case Left(error) => Future.successful(logValidationErrorInSettleAction(error))
      case _           => ()
    }
  }
}
