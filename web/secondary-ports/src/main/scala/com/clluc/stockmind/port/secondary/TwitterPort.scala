package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.twitter.{TwitterHandle, TwitterUserInfo, TwitterUserSearchResult}

import scala.concurrent.Future

trait TwitterPort {

  def findUserInfoByKey(key: Long, userCredentials: OAuth1Info): Future[Option[TwitterUserInfo]]

  def findUserIdFromScreenName(screenName: TwitterHandle,
                               userCredentials: OAuth1Info): Future[Option[Long]]

  def findUsersInfoByQuery(query: String,
                           userCredentials: OAuth1Info): Future[List[TwitterUserSearchResult]]

  /**
    * Sends a tweet using the account referred by the credentials
    * @param message The message we want to send within the tweet
    * @param userCredentials The credentials of the user whose account is going to be used to tweet
    * @return An eventual status Id that points to the just created tweet
    */
  def sendTweet(message: String)(userCredentials: OAuth1Info): Future[Long]

}
