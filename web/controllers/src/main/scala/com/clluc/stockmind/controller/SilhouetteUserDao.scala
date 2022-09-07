package com.clluc.stockmind.controller

import javax.inject.Inject

import com.clluc.stockmind.port.primary.UserPort
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.{LoginInfo => SilhouetteLoginInfo}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

class SilhouetteUserDao @Inject()(userPort: UserPort)(implicit ec: ExecutionContext)
    extends IdentityService[SilhouetteUser]
    with LazyLogging {

  override def retrieve(loginInfo: SilhouetteLoginInfo): Future[Option[SilhouetteUser]] = {
    import SilhouetteUser.BoundedContextImplicitConversions._
    import BoundedContextImplicitConversions._

    userPort.findFromLoginInfo(loginInfo.toCoreLoginInfo).map(_.map(asSilhouetteUser))
  }
}
