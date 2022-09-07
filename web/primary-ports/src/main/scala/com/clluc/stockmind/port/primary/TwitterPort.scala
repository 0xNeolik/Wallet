package com.clluc.stockmind.port.primary

import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterUserSearchResult}

import scala.concurrent.Future

trait TwitterPort {

  def findUsersInfoByQuery(query: String)(
      oAuthInfo: OAuth1Info): Future[List[TwitterUserSearchResult]]

  def saveTwitterAccount(twitterAccount: TwitterAccount): Future[TwitterAccount]
}
