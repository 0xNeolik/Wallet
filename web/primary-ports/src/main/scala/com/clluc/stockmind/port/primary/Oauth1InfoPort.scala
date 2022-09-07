package com.clluc.stockmind.port.primary

import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}

import scala.concurrent.Future

trait Oauth1InfoPort {
  def find(loginInfo: LoginInfo): Future[Option[OAuth1Info]]
  def save(oauthInfo: OAuth1Info, loginInfo: LoginInfo): Future[OAuth1Info]
}
