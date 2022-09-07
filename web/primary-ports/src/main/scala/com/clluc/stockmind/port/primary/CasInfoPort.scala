package com.clluc.stockmind.port.primary

import com.clluc.stockmind.core.auth.{CoreCasInfo, LoginInfo}

import scala.concurrent.Future

trait CasInfoPort {
  def find(loginInfo: LoginInfo): Future[Option[CoreCasInfo]]
  def save(casInfo: CoreCasInfo, loginInfo: LoginInfo): Future[CoreCasInfo]
}
