package com.clluc.stockmind.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile

case class CustomSocialProfile(
    loginInfo: LoginInfo,
    casAttributes: Map[String, String],
) extends SocialProfile
