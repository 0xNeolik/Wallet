package com.clluc.stockmind.port.secondary

import java.util.UUID

import com.clluc.stockmind.core.user.AuthToken
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.concurrent.duration._

trait AuthTokenPort {

  /**
    * Finds a token by its ID.
    *
    * @param id The unique token ID.
    * @return The found token or None if no token for the given ID could be found.
    */
  def findAuthTokenByUserId(id: UUID): Future[Option[AuthToken]]

  /**
    * Finds expired tokens.
    *
    * @param dateTime The current date time.
    */
  def findAuthTokensExpired(dateTime: DateTime): Future[Seq[AuthToken]]

  /**
    * Saves a token.
    *
    * @param token The token to save.
    * @return The saved token.
    */
  def saveAuthToken(token: AuthToken): Future[AuthToken]

  /**
    * Removes the token for the given ID.
    *
    * @param id The ID for which the token should be removed.
    * @return A future to wait for the process to be completed.
    */
  def removeAuthTokenWithId(id: UUID): Future[Unit]

  /**
    * Creates a new auth token and saves it in the backing store.
    *
    * @param userID The user ID for which the token should be created.
    * @param expiry The duration a token expires.
    * @return The saved auth token.
    */
  def createAuthTokenForUser(userID: UUID, expiry: FiniteDuration = 5.minutes): Future[AuthToken]

  /**
    * Validates a token ID.
    *
    * @param id The token ID to validate.
    * @return The token if it's valid, None otherwise.
    */
  def validateAuthTokenForUser(id: UUID): Future[Option[AuthToken]]

  /**
    * Cleans expired tokens.
    *
    * @return The list of deleted tokens.
    */
  def cleanExpiredAuthTokens(): Future[Seq[AuthToken]]
}
