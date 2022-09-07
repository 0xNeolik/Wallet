package com.clluc.stockmind.controller

import javax.inject.Inject

import com.clluc.stockmind.port.primary.TwitterPort
import com.clluc.stockmind.port.primary.Oauth1InfoPort
import com.clluc.stockmind.port.primary.UserPort

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._
import play.api.mvc.{BaseController, ControllerComponents}
import java.util.UUID
import play.api.cache.SyncCacheApi

import scala.concurrent.ExecutionContext

class TwitterController @Inject()(
    val controllerComponents: ControllerComponents,
    twitterPort: TwitterPort,
    authPort: Oauth1InfoPort,
    socAuthcontroller: SocialAuthController,
    cache: SyncCacheApi,
    usersPort: UserPort,
)(
    implicit
    executionContext: ExecutionContext
) extends BaseController
    with LazyLogging {

  // TODO If we finally decide to implement a REST API maybe we should have one controller per entity
  // As this method retrieves information for our beloved User resource,
  // what do you think if we refactor and include it as part of a UserController?
  // That UserController would serve all requests related to the User resource.
  // The URL namespace for that controller would be /user
  def queryUser(query: String) = socAuthcontroller.AuthenticatedAction { idToken => request =>
    val userID = cache.get[UUID](idToken + "id").get
    for {
      user      <- usersPort.findFromId(userID)
      authtoken <- authPort.find(user.get.loginInfo)
      usersInfo <- twitterPort.findUsersInfoByQuery(query)(authtoken.get)
    } yield Ok(usersInfo.asJson.noSpaces).as(JSON)
  }
}
