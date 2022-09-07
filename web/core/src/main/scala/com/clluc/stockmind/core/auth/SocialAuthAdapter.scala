package com.clluc.stockmind.core.auth

import java.util.UUID

import com.clluc.stockmind.core.SignUpConfiguration
import com.clluc.stockmind.core.ethereum.CreateOrRetrieveEthAccountOutcome.{
  CreateOrRetrieveAccountResult,
  UnsuccessfulEthereumAccountAccomplished
}
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.core.ethereum.{
  AccountCreated,
  AccountRetrieved,
  HumanStandardTokenContract,
  SignableTransaction
}
import com.clluc.stockmind.core.twitter.TwitterAccount
import com.clluc.stockmind.core.user.User

import com.clluc.stockmind.port.primary.SocialAuthPort
import com.clluc.stockmind.port.secondary._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.syntax._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

private[auth] class SocialAuthAdapter(
    userPort: UsersRepositoryPort,
    twitterPort: TwitterPort,
    twitterAccountPort: TwitterAccountPort,
    ethereumClientPort: EthereumClientPort,
    configuration: SignUpConfiguration,
    erc20InfoPort: Erc20InfoPort,
)(
    implicit
    executionContext: ExecutionContext
) extends SocialAuthPort
    with LazyLogging {

  override def storeNewUser(user: User): Future[User] =
    userPort.store(user)

  def retrieveTwitterInfo(twitterUserId: Long, authInfo: OAuth1Info): Future[Json] = {
    for {
      twitterInfo <- twitterPort.findUserInfoByKey(twitterUserId, authInfo)
    } yield {
      val data: mutable.Map[String, String] = mutable.Map.empty
      twitterInfo.foreach { i =>
        data += ("tw_screenname" -> i.screenName)
        data += ("tw_fullname"   -> i.fullName)
        data += ("tw_avatar_url" -> i.avatarUrl)
        data += ("tw_followers"  -> i.followersCount.toString)
        data += ("tw_verified"   -> i.isVerified.toString)
      }
      data.toMap.asJson
    }
  }

  override def handleTwitterAccountCreation(twitterUserId: Long,
                                            authInfo: OAuth1Info,
                                            stockmindUserId: UUID): Future[TwitterAccount] = {
    val eventualTwitterInfo =
      twitterPort.findUserInfoByKey(
        twitterUserId,
        OAuth1Info(token = authInfo.token, secret = authInfo.secret)
      )

    val eventualTwitterAccount = eventualTwitterInfo.map { maybeTwitterInfo =>
      maybeTwitterInfo
        .map { twitterInfo =>
          TwitterAccount(
            stockmindUserId,
            twitterUserId,
            twitterInfo.screenName,
            twitterInfo.isVerified,
            twitterInfo.followersCount,
            Option(twitterInfo.avatarUrl)
          )
        }
        .getOrElse(
          // TODO Refactor this to use a type class (not coupled to future) and ADTs for errors instead of exceptions
          throw new RuntimeException(
            s"Cannot save user twitter account info into DB because that user with twitter id [$twitterUserId] " +
              s"(user $stockmindUserId in stockmind) does not have a twitter account"
          )
        )
    }

    eventualTwitterAccount.flatMap(twitterAccountPort.saveTwitterAccount)
  }

  override def handleInitialEtherGift(
      createOrRetrieveAccountResult: CreateOrRetrieveAccountResult
  ): Either[UnsuccessfulEthereumAccountAccomplished, Future[JsonRpcPlainResult]] = {

    createOrRetrieveAccountResult.map { accountCreatedResult =>
      accountCreatedResult.createOrRetrieveOutcome match {
        case AccountCreated(_) =>
          for {
            _ <- ethereumClientPort.sendEther(
              configuration.ethGasAddress,
              accountCreatedResult.account.address,
              configuration.ethStartingBalance,
              configuration.ethGasPassword
            )
            sldTokenO <- erc20InfoPort.findEthereumTokenBySymbolAndType("SLD|ERC-20")
            sldToken    = sldTokenO.get
            sldContract = HumanStandardTokenContract(sldToken)

            sendTxResult <- ethereumClientPort.sendTransaction(
              SignableTransaction(
                sldContract.transfer(
                  configuration.sldSupplier,
                  accountCreatedResult.account.address,
                  configuration.sldWelcomeAmount
                ),
                configuration.sldSupplierPassword
              )
            )
          } yield sendTxResult

        case AccountRetrieved(Some(callResponse)) => Future.successful(Right(callResponse))

        // TODO This is likely to be a bug; but for now we need it to compile
        // Anyway this controller is impossible to maintain. A complete rewrite is needed IMO, or at least a really disruptive refactor
        // Many concerns here have been simplified
        case AccountRetrieved(None) => Future.successful(Right(""))
      }
    }
  }
}

object SocialAuthAdapter {

  def apply(
      userPort: UsersRepositoryPort,
      twitterPort: TwitterPort,
      twitterAccountPort: TwitterAccountPort,
      ethereumClientPort: EthereumClientPort,
      configuration: SignUpConfiguration,
      erc20InfoPort: Erc20InfoPort,
  )(
      implicit
      executionContext: ExecutionContext
  ): SocialAuthAdapter =
    new SocialAuthAdapter(
      userPort,
      twitterPort,
      twitterAccountPort,
      ethereumClientPort,
      configuration,
      erc20InfoPort,
    )
}
