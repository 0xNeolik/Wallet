package com.clluc.stockmind.core.twitter

import java.util.UUID

/**
  * The twitter account object. As part of the port interface parameter or return type is also part of
  * the public interface of the core.
  *
  * @param userID The unique ID of the user.
  * @param accountID The twitter id.
  * @param screenName Screen name as shown in Twitter (the name after the '@' sign)
  * @param verified Whether the Twitter account is verified.
  * @param followers Number of followers of the account.
  * @param avatarURL Maybe the avatar URL of the authenticated provider.
  */
case class TwitterAccount(
    userID: UUID,
    accountID: Long,
    screenName: String,
    verified: Boolean,
    followers: Int,
    avatarURL: Option[String]
)
