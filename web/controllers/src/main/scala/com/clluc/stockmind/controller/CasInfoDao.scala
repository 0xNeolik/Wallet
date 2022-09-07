package com.clluc.stockmind.controller

import javax.inject.Inject

import com.clluc.stockmind.core.auth.CoreCasInfo
import com.clluc.stockmind.port.primary.CasInfoPort
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CasInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.{ExecutionContext, Future}

class CasInfoDao @Inject()(
    casInfoPort: CasInfoPort,
)(
    implicit
    ec: ExecutionContext,
) extends DelegableAuthInfoDAO[CasInfo] {

  import BoundedContextImplicitConversions._

  implicit class RichCoreCasInfo(info: CoreCasInfo) {
    def asSilhouette = CasInfo(info.ticket)
  }

  implicit class RichCasInfo(info: CasInfo) {
    def asCore = CoreCasInfo(info.ticket)
  }

  override def find(loginInfo: LoginInfo): Future[Option[CasInfo]] = {
    casInfoPort.find(loginInfo.toCoreLoginInfo).map(_.map(_.asSilhouette))
  }

  override def add(loginInfo: LoginInfo, authInfo: CasInfo): Future[CasInfo] = ???

  override def update(loginInfo: LoginInfo, authInfo: CasInfo): Future[CasInfo] = ???

  override def save(loginInfo: LoginInfo, authInfo: CasInfo): Future[CasInfo] = {
    casInfoPort.save(authInfo.asCore, loginInfo.toCoreLoginInfo).map(_.asSilhouette)
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = ???
}
