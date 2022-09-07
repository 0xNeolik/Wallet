package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.usersdirectory.{
  DirectoryUserInfo,
  MessageUserError,
  UsersDirectorySearchResult
}

/**
  * Represents the interface to access the users directory; that is, the complete repository of users
  * that either are already registered in the platform or can eventually become registered users.
  * Can be backed with implementations for example for Twitter, an LDAP, Active Directory, database, etc.
  *
  * To avoid committing to an specific context too early
  * (https://softwaremill.com/free-tagless-compared-how-not-to-commit-to-monad-too-early/)
  * for the first time in the project we use higher kinds in a port definition. Makes sense as we don't need
  * to know anything about the context of the port operations at this point. Probably a good design decision
  * to migrate to the rest of ports in the project.
  */
trait UsersDirectoryPort[P[_]] {

  type UserId

  def userInfoByItsId(userId: UserId): P[Option[DirectoryUserInfo]]

  /**
    * Queries the directory getting the corresponding matching users information.
    * @param query
    * @return
    */
  def usersInfoByQuery(query: String): P[List[UsersDirectorySearchResult]]

  /**
    * Given an screen name (attribute we consider mandatory in our directory), see if there's a user in
    * it that qualifies.
    * @param screenName
    * @return Some(id) with the id of the user in the directory. None if a user with the given screen name
    *         doesn't exist.
    */
  def userIdForScreenName(screenName: String): P[Option[Long]]

  /**
    * Try to send the given message to the given destination user in the directory if possible.
    * @param message
    * @param destinationUserScreenName
    * @return An eventual Unit if everything is ok; or an error if something went wrong (refer to method
    *         signature for further details).
    */
  def messageUser(message: String,
                  destinationUserScreenName: String): P[Either[MessageUserError, Unit]]
}
