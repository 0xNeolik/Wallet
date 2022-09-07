package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.User
import com.clluc.stockmind.port.secondary.UsersRepositoryPort
import doobie.imports._
import doobie.postgres.imports._
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

private abstract class AbstractPostgresUsersRepositoryAdapter(implicit ex: ExecutionContext)
    extends UsersRepositoryPort {

  // TODO Migrate this logic to the core module
  // Where it actually belongs
  def store(user: User): Future[User] = {
    retrieve(user.loginInfo)
      .map {
        case Some(existingUser) => // Update user with profile
          user.copy(
            userID = existingUser.userID,
            loginInfo = existingUser.loginInfo
          )
        case None => // Insert a new user
          user
      }
      .flatMap(save)
  }
}

private[postgres] class PostgresUsersRepositoryAdapter(val transactor: Transactor[IOLite])(
    override implicit val executionContext: ExecutionContext
) extends AbstractPostgresUsersRepositoryAdapter
    with Dao {

  /**
    * Finds a user by its login info.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    def query(provider: String, key: String) = sql"""
      SELECT *
      FROM users
      WHERE
        login_provider = $provider
      AND
        login_key = $key
      """.query[User]

    selectOne(query(loginInfo.providerID, loginInfo.providerKey))
  }

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  override def find(userID: UUID): Future[Option[User]] = {
    def query(id: UUID) = sql"""
      SELECT *
      FROM users
      WHERE
        id = $id
    """.query[User]

    selectOne(query(userID))
  }

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  override def findAuth0(userID: UUID): Future[Option[User]] = {
    def query(id: UUID) = sql"""
      SELECT *
      FROM users
      WHERE
      login_provider='auth0'
      AND
      id = $id
    """.query[User]

    selectOne(query(userID))
  }

  /**
    * Finds a user by its API KEY.
    *
    * @param api_key The api_key of the user to find.
    * @return The found user or None if no user for the given api_key could be found.
    */
  override def findByApiKey(api_key: UUID): Future[Option[User]] = {
    def query(api_key: UUID) = sql"""
      SELECT users.*
      FROM api_keys LEFT Join users ON users.id = api_keys.user_id
      WHERE
      api_keys.api_key = $api_key
    """.query[User]

    selectOne(query(api_key))
  }

  /**
    * Finds a user by its Twitter login key.
    *
    * @param loginKey A String corresponding to the Twitter login key.
    * @return The found user or None if no user for the given login key could be found.
    */
  override def findByLoginKey(loginKey: String): Future[Option[User]] = {
    def query(loginKey: String) = sql"""
      SELECT *
      FROM users
      WHERE
        login_key = $loginKey
    """.query[User]

    selectOne(query(loginKey))
  }

  /**
    * Finds a user by its Identifier.
    *
    * @param identifier A String corresponding to the identifier.
    * @return The found user or None if no user for the given login key could be found.
    */
  override def findByIdentifier(identifier: String): Future[Option[User]] = {
    def query(identifier: String) = sql"""
      SELECT *
      FROM users
      WHERE
      login_provider = 'auth0'
      AND
        identifier = $identifier
    """.query[User]

    selectOne(query(identifier))
  }

  /**
    * Find auth users by name
    *
    * @param name A String corresponding to the auth user name.
    * @return List of users
    */
  override def findByquery(name: String, page: Int): Future[List[User]] = {
    val like_identifier                     = '%' + name + '%'
    def query(identifier: String, pag: Int) = sql"""
      SELECT *
      FROM users
      WHERE
       login_provider = 'auth0'
      AND
        identifier LIKE $identifier
      OFFSET 10* $pag LIMIT 10
    """.query[User]

    selectMany(query(like_identifier, page))
  }

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  override def save(user: User): Future[User] = {
    def query(
        userId: UUID,
        providerId: String,
        providerKey: String,
        data: Json,
        identifier: String,
    ): ConnectionIO[User] = sql"""
      INSERT INTO users
        (id, login_provider, login_key, data, identifier)
      VALUES
        ($userId, $providerId, $providerKey, $data, $identifier)
      ON CONFLICT (id) DO UPDATE
      SET
        login_provider = $providerId,
        login_key = $providerKey,
        data = $data,
        identifier = $identifier
      WHERE
        users.id = $userId
    """.update.withUniqueGeneratedKeys(
      "id",
      "login_provider",
      "login_key",
      "data",
      "identifier"
    )

    insertWithFeedback(
      query(
        user.userID,
        user.loginInfo.providerID,
        user.loginInfo.providerKey,
        user.directoryData.data,
        user.identifier
      )
    )
  }

  override def storeSingleDataValue(userId: UUID,
                                    jsonKey: List[String],
                                    value: Json): Future[User] = {

    def query(
        userId: UUID,
        jsonKey: List[String],
        data: Json
    ): ConnectionIO[User] =
      sql"""
      UPDATE users
      SET
        data = jsonb_set(data, $jsonKey, $data, true)
      WHERE
        id = $userId
    """.update.withUniqueGeneratedKeys(
        "id",
        "login_provider",
        "login_key",
        "data"
      )

    insertWithFeedback(query(userId, jsonKey, value))
  }

  override def storeApiKey(userId: UUID, apiKey: UUID): Future[UUID] = {

    def query(
        userId: UUID,
        apiKey: UUID
    ): ConnectionIO[UUID] =
      sql"""
     INSERT INTO api_keys
        (user_id, api_key)
      VALUES
        ($userId,$apiKey)
    """.update.withUniqueGeneratedKeys(
        "api_key"
      )
    insertWithFeedback(query(userId, apiKey))
  }

  override def removeApiKey(userId: UUID, apiKey: UUID): Future[UUID] = {

    def query(
        userId: UUID,
        apiKey: UUID
    ): ConnectionIO[UUID] =
      sql"""
     DELETE FROM api_keys
     WHERE
        user_id = $userId and  api_key = $apiKey
    """.update.withUniqueGeneratedKeys(
        "api_key"
      )
    insertWithFeedback(query(userId, apiKey))
  }

}

object PostgresUsersRepositoryAdapter {

  def apply(tx: Transactor[IOLite])(implicit ec: ExecutionContext): PostgresUsersRepositoryAdapter =
    new PostgresUsersRepositoryAdapter(tx)

}
