package com.clluc.stockmind.core.twitter

/**
  * Represents the information we are interested in regarding twitter user search.
  * @param fullName
  * @param screenName
  * @param imageUrl
  */
case class TwitterUserSearchResult(fullName: Option[String],
                                   screenName: String,
                                   imageUrl: Option[String])
