package com.clluc.stockmind.port.primary

import java.util.UUID

import com.clluc.stockmind.core.ethereum.CreateOrRetrieveEthAccountOutcome.{
  CreateOrRetrieveAccountResult,
  UnsuccessfulEthereumAccountAccomplished
}
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.core.auth.OAuth1Info
import com.clluc.stockmind.core.twitter.TwitterAccount
import com.clluc.stockmind.core.user.User
import io.circe.Json

import scala.concurrent.Future

trait SocialAuthPort {
  def storeNewUser(user: User): Future[User]

  def retrieveTwitterInfo(twitterUserId: Long, authInfo: OAuth1Info): Future[Json]

  def handleTwitterAccountCreation(
      twitterUserId: Long,
      authInfo: OAuth1Info,
      stockmindUserId: UUID
  ): Future[TwitterAccount]

  def handleInitialEtherGift(
      createOrRetrieveAccountResult: CreateOrRetrieveAccountResult
  ): Either[UnsuccessfulEthereumAccountAccomplished, Future[JsonRpcPlainResult]]
}
