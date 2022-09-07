package com.clluc.stockmind.core.auth

import com.clluc.stockmind.port.primary.Oauth1InfoPort
import com.clluc.stockmind.port.secondary.{Oauth1InfoPort => Oauth1InfoSecondaryPort}

import scala.concurrent.Future

private[auth] class Oauth1InfoAdapter(port: Oauth1InfoSecondaryPort) extends Oauth1InfoPort {

  override def find(loginInfo: LoginInfo): Future[Option[OAuth1Info]] =
    port.findByProviderIdAndKey(loginInfo)

  override def save(
      oauthInfo: OAuth1Info,
      loginInfo: LoginInfo
  ): Future[OAuth1Info] =
    port.saveOauthInfo(loginInfo, oauthInfo)
}

object Oauth1InfoAdapter {

  def apply(port: Oauth1InfoSecondaryPort): Oauth1InfoAdapter =
    new Oauth1InfoAdapter(port)
}
