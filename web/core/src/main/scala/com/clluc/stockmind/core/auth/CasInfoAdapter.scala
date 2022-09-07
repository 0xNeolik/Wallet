package com.clluc.stockmind.core.auth

import com.clluc.stockmind.port.primary.CasInfoPort
import com.clluc.stockmind.port.secondary.{CasInfoPort => SecondaryCasInfoPort}

import scala.concurrent.Future

private[auth] class CasInfoAdapter(port: SecondaryCasInfoPort) extends CasInfoPort {
  override def find(loginInfo: LoginInfo): Future[Option[CoreCasInfo]] = port.findBy(loginInfo)

  override def save(casInfo: CoreCasInfo, loginInfo: LoginInfo): Future[CoreCasInfo] =
    port.save(loginInfo, casInfo)
}

object CasInfoAdapter {

  def apply(port: SecondaryCasInfoPort): CasInfoAdapter =
    new CasInfoAdapter(port)

}
