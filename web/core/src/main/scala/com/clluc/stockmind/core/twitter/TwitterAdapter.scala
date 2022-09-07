package com.clluc.stockmind.core.twitter

import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.port.primary
import primary.{TwitterPort => PrimaryTwitterPort}
import com.clluc.stockmind.port.secondary
import secondary.{TwitterPort => SecondaryTwitterPort}
import secondary.{TwitterAccountPort => SecondaryTwitterAccountPort}

import scala.concurrent.{ExecutionContext, Future}

private[twitter] class TwitterAdapter(
    secondaryTwitterPort: SecondaryTwitterPort,
    secondaryTwitterAccountPort: SecondaryTwitterAccountPort
)(
    implicit
    executionContext: ExecutionContext
) extends PrimaryTwitterPort {

  import TwitterAdapter.BoundedContextImplicitConversions._

  override def findUsersInfoByQuery(query: String)(
      oAuthInfo: OAuth1Info): Future[List[TwitterUserSearchResult]] = {
    secondaryTwitterPort
      .findUsersInfoByQuery(query, OAuth1Info(oAuthInfo.token, oAuthInfo.secret))
      .toPrimaryPortCounterpart
  }

  override def saveTwitterAccount(twitterAccount: TwitterAccount): Future[TwitterAccount] = {
    secondaryTwitterAccountPort.saveTwitterAccount(twitterAccount)
  }

}

object TwitterAdapter {

  private[twitter] object BoundedContextImplicitConversions {

    implicit class UserSearchResultConversions(result: Future[List[TwitterUserSearchResult]]) {

      def toPrimaryPortCounterpart(
          implicit executionContext: ExecutionContext): Future[List[TwitterUserSearchResult]] =
        result.map(_.map(fromSecondaryToPrimaryUserSearchResult))
    }

    private def fromSecondaryToPrimaryUserSearchResult(
        result: TwitterUserSearchResult): TwitterUserSearchResult =
      TwitterUserSearchResult(
        result.fullName,
        result.screenName,
        result.imageUrl
      )
  }

  def apply(
      secondaryTwitterPort: SecondaryTwitterPort,
      secondaryTwitterAccountPort: SecondaryTwitterAccountPort
  )(
      implicit
      executionContext: ExecutionContext
  ): TwitterAdapter =
    new TwitterAdapter(secondaryTwitterPort, secondaryTwitterAccountPort)
}
