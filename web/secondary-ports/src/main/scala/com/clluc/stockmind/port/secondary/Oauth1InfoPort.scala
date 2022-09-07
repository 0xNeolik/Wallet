package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}

import scala.concurrent.Future

trait Oauth1InfoPort {
  def findByProviderIdAndKey(loginInfo: LoginInfo): Future[Option[OAuth1Info]]

  def saveOauthInfo(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info]
}
