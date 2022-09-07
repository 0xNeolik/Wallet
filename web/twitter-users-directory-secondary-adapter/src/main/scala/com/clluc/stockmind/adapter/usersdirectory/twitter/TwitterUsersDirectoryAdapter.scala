package com.clluc.stockmind.adapter.usersdirectory.twitter

import cats.Applicative
import cats.syntax.applicative._
import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.usersdirectory.{
  DirectoryUserInfo,
  MessageUserError,
  UsersDirectorySearchResult
}
import com.clluc.stockmind.port.secondary.UsersDirectoryPort
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Twitter, TwitterFactory}

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * UsersDirectoryPort implementation based on Twitter.
  * Keeps the philosophy to not commit to a monad to early (see port trait Scaladoc for further
  * reference).
  * @param stockmindConsumerKey Key for the Twitter application
  * @param stockmindConsumerSecret Secret for the Twitter application
  * @param userTwitterOAuthInfo OAuth 1 info for the user on behalf we want to call a Twitter API method
  * @tparam P Applicative functor under which this computation will run
  */
class TwitterUsersDirectoryAdapter[P[_]](
    stockmindConsumerKey: String,
    stockmindConsumerSecret: String
)(
    userTwitterOAuthInfo: OAuth1Info
)(
    implicit ev: Applicative[P]
) extends UsersDirectoryPort[P] {

  override type UserId = Long

  private def twitterClient(userTwitterCredentials: OAuth1Info): Twitter = {
    val confBuilder = new ConfigurationBuilder()
    confBuilder
      .setDebugEnabled(true)
      .setOAuthConsumerKey(stockmindConsumerKey)
      .setOAuthConsumerSecret(stockmindConsumerSecret)
      .setOAuthAccessToken(userTwitterOAuthInfo.token)
      .setOAuthAccessTokenSecret(userTwitterOAuthInfo.secret)
    val config = confBuilder.build
    new TwitterFactory(config).getInstance
  }

  override def usersInfoByQuery(query: String) = {
    import cats.syntax.functor._

    val eventualUsersForQuery = twitterClient(userTwitterOAuthInfo).searchUsers(query, 1).pure[P]

    eventualUsersForQuery.map(
      _.asScala.map { user =>
        UsersDirectorySearchResult(Option(user.getName),
                                   user.getScreenName,
                                   Option(user.getProfileImageURL))
      }.toList
    )
  }

  override def userIdForScreenName(screenName: String) =
    Try(twitterClient(userTwitterOAuthInfo).showUser(screenName).getId).toOption.pure[P]

  override def messageUser(message: String, destinationUserScreenName: String) = {
    import cats.syntax.either._

    Try(twitterClient(userTwitterOAuthInfo).updateStatus(message)).toEither
      .bimap(
        MessageUserError.sendAttemptFailed,
        _ => ()
      )
      .pure[P]
  }

  override def userInfoByItsId(userId: Long) = {
    val user = Try(twitterClient(userTwitterOAuthInfo).showUser(userId)).toOption

    user
      .map { u =>
        DirectoryUserInfo(
          u.getScreenName,
          Option(u.getName),
          Option(u.getMiniProfileImageURL)
        )
      }
      .pure[P]
  }
}

object UsersDirectoryPortFactory {

  def apply[P[_]](stockmindConsumerKey: String, stockmindConsumerSecret: String)(
      implicit ev: Applicative[P]
  ): OAuth1Info => UsersDirectoryPort[P] =
    userTwitterOAuthInfo =>
      new TwitterUsersDirectoryAdapter[P](stockmindConsumerKey, stockmindConsumerSecret)(
        userTwitterOAuthInfo)
}
