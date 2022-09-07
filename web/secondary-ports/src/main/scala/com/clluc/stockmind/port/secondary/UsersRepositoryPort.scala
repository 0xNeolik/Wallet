package com.clluc.stockmind.port.secondary

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.User

import io.circe.Json
import scala.concurrent.Future

/**
  * Exposes functions to set or retrieve information to / from the repository
  * that keeps platform users information.
  * The current implementation at the time of this writing uses Postgres as
  * repository.
  */
trait UsersRepositoryPort {

  /**
    * Finds a user by its login info.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]]

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def find(userID: UUID): Future[Option[User]]

  /**
    * Finds a user by its user ID with auth0.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def findAuth0(userID: UUID): Future[Option[User]]

  /**
    * Finds a user by its user ID.
    *
    * @param userID The api_key of the user to find.
    * @return The found user or None if no user for the given api_key could be found.
    */
  def findByApiKey(api_key: UUID): Future[Option[User]]

  /**
    * Finds a user by its Twitter login key.
    *
    * @param loginKey A String corresponding to the Twitter login key.
    * @return The found user or None if no user for the given login key could be found.
    */
  def findByLoginKey(loginKey: String): Future[Option[User]]

  /**
    * Finds a user by its Identifier.
    *
    * @param identifier A String corresponding to the identifier.
    * @return The found user or None if no user for the given login key could be found.
    */
  def findByIdentifier(identifier: String): Future[Option[User]]

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: User): Future[User]

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param user The social profile to save.
    * @return The user for whom the profile was saved.
    */
  def store(user: User): Future[User]

  /**
    * Save a single key-value pair in the users' local directory.
    * @param userId The users' unique UUID.
    * @param jsonKey A list detailing the path inside the JSON structure. Root-level keys are
    *                one-element lists, while nesting involves multi-element lists.
    * @param value The value to insert. It can be any valid JSON value: an object, an array,
    *              a string, or a number.
    * @return The newly modified user.
    */
  def storeSingleDataValue(userId: UUID, jsonKey: List[String], value: Json): Future[User]

  /**
    * Save api key value in api_keys.
    * @param userId The users' unique UUID.
    * @param apiKey The new api key UUID value
    * @return The newly modified user.
    */
  def storeApiKey(userId: UUID, apiKey: UUID): Future[UUID]

  /**
    * delete api key value in api_keys.
    * @param userId The users' unique UUID.
    * @param apiKey The api key UUID value
    * @return The removed api key.
    */
  def removeApiKey(userId: UUID, apikey: UUID): Future[UUID]

  /**
    * Find auth users by name
    *
    * @param name A String corresponding to the auth user name.
    * @param page: Show page of results
    * @return List of users
    */
  def findByquery(name: String, page: Int): Future[List[User]]
}
