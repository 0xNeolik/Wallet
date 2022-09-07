package com.clluc.stockmind.controller

import com.clluc.stockmind.core.ethereum.Erc721Token
import javax.inject.Inject

import scala.concurrent.Future

import com.clluc.stockmind.controller.UsersController.{toUserInfoView, toUsersInfoView}
import com.clluc.stockmind.core.ethereum.Amount
import com.clluc.stockmind.core.user._
import com.clluc.stockmind.port.primary._

import play.api.mvc._

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import scala.concurrent.{ExecutionContext}
import play.api.cache.SyncCacheApi

import java.util.UUID

class UsersController @Inject()(
    usersPort: UserPort,
    cache: SyncCacheApi,
    configuration: play.api.Configuration,
    socAuthcontroller: SocialAuthController,
    val controllerComponents: ControllerComponents)(implicit val executionContext: ExecutionContext)
    extends BaseController {

  // GET /v1/users/me
  def findUserInfo: Action[AnyContent] = socAuthcontroller.AuthenticatedAction {
    idToken => request =>
      val userID = cache.get[UUID](idToken + "id").get
      //retrieve user from database
      val fut = usersPort.findFromId(userID)
      fut.flatMap { user =>
        val coreCallResult: Future[Either[UserOperationError, UserInfo]] =
          usersPort.findUserInfo(user.get.userID, user.get.directoryData).value

        coreCallResult.map {
          case Left(error) => InternalServerError(error.message())
          case Right(userInfo) =>
            Ok(toUserInfoView(userInfo).asJson.noSpaces).as(JSON)
        }

      }

  }

  // GET /v1/users/apikey
  def addApiKey = socAuthcontroller.AuthenticatedAction { idToken => request =>
    val userID = cache.get[UUID](idToken + "id").get
    val fut2   = usersPort.setApiKey(userID, UUID.randomUUID())
    fut2.flatMap { newapikey =>
      Future.successful(Ok(play.api.libs.json.Json.obj("api_key" -> newapikey.toString)).as(JSON))
    }
  }

  // DELETE /v1/users/apikey
  def removeApiKey(apiKey: String) = socAuthcontroller.AuthenticatedAction { idToken => request =>
    val userID = cache.get[UUID](idToken + "id").get
    try {
      val keyUUID = UUID.fromString(apiKey)
      val fut2    = usersPort.deleteApiKey(userID, keyUUID)
      fut2.flatMap { apikeyold =>
        if (keyUUID == idToken) {
          cache.remove(idToken + "profile")
          cache.remove(idToken + "id")
        }
        Future.successful(
          Ok(play.api.libs.json.Json.obj("deleted_key" -> apikeyold.toString)).as(JSON))
      }
    } catch {
      case e: Exception => //wrong format
        print(e)
        Future.successful(BadRequest("Wrong API key"))

    }

  }

  //GET /v1/users/:query?page=0
  def queryUser(query: String, page: Int) = socAuthcontroller.AuthenticatedAction {
    idToken => request =>
      for {
        users <- usersPort.findUsersByName(query, page)
      } yield Ok(toUsersInfoView(users).asJson.noSpaces).as(JSON)
  }

}

private[controller] object UsersController {

  def toUserInfoView(userInfo: UserInfo): UserInfoView = {

    def toAmountView(amount: Amount): AmountView =
      AmountView(
        whole = amount.integerPart,
        decimal = amount.decimalPart
      )

    def toBalanceView(balance: Balance): BalanceView =
      BalanceView(
        balance.token.symbol,
        balance.token.name,
        balance.token.decimals,
        toAmountView(
          Amount.fromRawIntegerValue(balance.effectiveBalance.toString, balance.token.decimals))
      )

    def toErc721View(erc721: Erc721Token): Erc721View =
      Erc721View(
        erc721.symbol,
        erc721.name,
        erc721.meta,
        erc721.id.toString()
      )

    UserInfoView(
      userdata = userInfo.directoryData,
      erc20_tokens = userInfo.balances.map(toBalanceView),
      erc721_tokens = userInfo.erc_721.map(toErc721View)
    )
  }

  case class UserInfoView(
      userdata: Json,
      erc20_tokens: List[BalanceView],
      erc721_tokens: List[Erc721View]
  )

  case class BalanceView(
      symbol: String,
      fullname: String,
      decimals: Int,
      amount: AmountView
  )
  case class Erc721View(
      symbol: String,
      fullname: String,
      metadata: String,
      id: String
  )

  case class AmountView(
      whole: String,
      decimal: String
  )

  def toUsersInfoView(usersInfo: List[User]): UsersInfoView = {
    def toIdentifierView(user: User): UsersIdentifierView =
      UsersIdentifierView(user.identifier)

    UsersInfoView(
      users = usersInfo.map(toIdentifierView)
    )
  }
  case class UsersIdentifierView(email: String)
  case class UsersInfoView(users: List[UsersIdentifierView])

}
