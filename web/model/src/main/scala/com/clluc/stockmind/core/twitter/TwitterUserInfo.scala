package com.clluc.stockmind.core.twitter

case class TwitterUserInfo(
    screenName: String,
    fullName: String,
    isVerified: Boolean,
    followersCount: Int,
    avatarUrl: String
)
