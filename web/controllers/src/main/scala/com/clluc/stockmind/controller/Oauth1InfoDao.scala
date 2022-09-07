package com.clluc.stockmind.controller

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LoginInfo => SilhoutteLoginInfo}
import com.mohiva.play.silhouette.impl.providers.{OAuth1Info => SilhouetteOauth1Info}
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.clluc.stockmind.port.primary.Oauth1InfoPort

import scala.concurrent.{ExecutionContext, Future}

class Oauth1InfoDao @Inject()(oauth1InfoPort: Oauth1InfoPort)(implicit ec: ExecutionContext)
    extends DelegableAuthInfoDAO[SilhouetteOauth1Info] {

  import BoundedContextImplicitConversions._

  override def find(loginInfo: SilhoutteLoginInfo): Future[Option[SilhouetteOauth1Info]] = {
    oauth1InfoPort.find(loginInfo.toCoreLoginInfo).map(_.map(_.toSilhouetteOauthInfo))
  }

  override def add(
      loginInfo: SilhoutteLoginInfo,
      authInfo: SilhouetteOauth1Info
  ): Future[SilhouetteOauth1Info] =
    save(loginInfo, authInfo)

  override def update(
      loginInfo: SilhoutteLoginInfo,
      authInfo: SilhouetteOauth1Info
  ): Future[SilhouetteOauth1Info] =
    save(loginInfo, authInfo)

  override def save(loginInfo: SilhoutteLoginInfo,
                    authInfo: SilhouetteOauth1Info): Future[SilhouetteOauth1Info] =
    oauth1InfoPort
      .save(authInfo.toCoreOauth1Info, loginInfo.toCoreLoginInfo)
      .map(_.toSilhouetteOauthInfo)

  override def remove(loginInfo: SilhoutteLoginInfo): Future[Unit] = ???

}
