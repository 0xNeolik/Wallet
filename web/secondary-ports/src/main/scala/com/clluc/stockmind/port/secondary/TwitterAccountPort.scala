package com.clluc.stockmind.port.secondary

import java.util.UUID

import com.clluc.stockmind.core.twitter.TwitterAccount

import scala.concurrent.Future

/**
  * Secondary port that defines the contract we need to retrieve information from any user Twitter account.
  * Will be implemented by the corresponding secondary adapter.
  * As a port, it is part of the public interface of the core.
  */
trait TwitterAccountPort {
  def findAccountById(userID: UUID): Future[Option[TwitterAccount]]
  def findAllScreenNames(): Future[List[String]]
  def findTwitterAccountByScreenName(screenName: String): Future[Option[TwitterAccount]]
  def saveTwitterAccount(twitterAccount: TwitterAccount): Future[TwitterAccount]
}
