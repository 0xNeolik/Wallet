package com.clluc.stockmind.core.usersdirectory

/**
  * Represents the information we are interested in response to a query to the users directory.
  * @param fullName
  * @param screenName
  * @param imageUrl
  */
case class UsersDirectorySearchResult(
    fullName: Option[String],
    screenName: String,
    imageUrl: Option[String]
)

case class DirectoryUserInfo(
    screenName: String,
    fullName: Option[String],
    avatarUrl: Option[String]
)

// ADT to define what can go wrong when sending a message to a user
sealed trait MessageUserError

object MessageUserError {
  // Possible instances of this ADT; just one for now
  case class SendAttemptFailed(throwable: Throwable) extends MessageUserError

  // Smart constructors for the previous ADT
  def sendAttemptFailed(th: Throwable): MessageUserError = SendAttemptFailed(th)
}
