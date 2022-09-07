package com.clluc.stockmind.core.user

import java.util.UUID

import com.clluc.stockmind.core.auth.LoginInfo

/**
  * The user object.
  *
  * @param userID The unique ID of the user.
  * @param loginInfo The linked login info.
  * @param directoryData User information, stored in a JSON structure.
  * @param identifier User iedntifier, screenname/Twitter and email/Auth0.
  */
case class User(
    userID: UUID,
    loginInfo: LoginInfo,
    directoryData: LocalDirectoryData,
    identifier: String
)
