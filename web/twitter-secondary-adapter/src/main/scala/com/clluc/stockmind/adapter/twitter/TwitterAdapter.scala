package com.clluc.stockmind.adapter.twitter

import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.twitter.{TwitterHandle, TwitterUserInfo, TwitterUserSearchResult}
import com.clluc.stockmind.port.secondary.TwitterPort
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Twitter, TwitterFactory}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

private[twitter] class TwitterAdapter(
    consumerKey: String,
    consumerSecret: String
)(implicit ec: ExecutionContext)
    extends TwitterPort {

  private def twitterClient(userTwitterCredentials: OAuth1Info): Twitter = {
    val confBuilder = new ConfigurationBuilder()
    confBuilder
      .setDebugEnabled(true)
      .setOAuthConsumerKey(consumerKey)
      .setOAuthConsumerSecret(consumerSecret)
      .setOAuthAccessToken(userTwitterCredentials.token)
      .setOAuthAccessTokenSecret(userTwitterCredentials.secret)
    val config = confBuilder.build
    new TwitterFactory(config).getInstance
  }

  override def findUserInfoByKey(key: Long,
                                 userCredentials: OAuth1Info): Future[Option[TwitterUserInfo]] =
    Future {
      // Due to how the Twitter4j library works internally, when the user doesn't exist we get a twitter exception
      // wrapping a 404 HTTP response
      // TODO In order to not loose the information of the error use a try or an either instead of an option
      val user = Try(twitterClient(userCredentials).showUser(key)).toOption

      user.map { u =>
        TwitterUserInfo(
          u.getScreenName,
          u.getName,
          u.isVerified,
          u.getFollowersCount,
          u.getOriginalProfileImageURL
        )
      }
    }

  override def findUserIdFromScreenName(screenName: TwitterHandle,
                                        userCredentials: OAuth1Info): Future[Option[Long]] =
    Future(Try(twitterClient(userCredentials).showUser(screenName.value).getId).toOption)

  override def sendTweet(message: String)(userCredentials: OAuth1Info): Future[Long] = {
    Future {
      twitterClient(userCredentials).updateStatus(message).getId
    }
  }

  override def findUsersInfoByQuery(
      query: String,
      userCredentials: OAuth1Info): Future[List[TwitterUserSearchResult]] = {
    val eventualUsersForQuery = Future(twitterClient(userCredentials).searchUsers(query, 1))

    eventualUsersForQuery.map(
      _.asScala.map { user =>
        TwitterUserSearchResult(Option(user.getName),
                                user.getScreenName,
                                Option(user.getProfileImageURL))
      }.toList
    )
  }
}

object TwitterAdapter {

  def apply(
      consumerKey: String,
      consumerSecret: String
  )(implicit ec: ExecutionContext): TwitterAdapter = new TwitterAdapter(consumerKey, consumerSecret)
}
