package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.auth.{CoreCasInfo, LoginInfo}

import scala.concurrent.Future

trait CasInfoPort {
  def findBy(loginInfo: LoginInfo): Future[Option[CoreCasInfo]]
  def save(loginInfo: LoginInfo, casInfo: CoreCasInfo): Future[CoreCasInfo]
}
