package com.clluc.stockmind.core.auth

case class Auth0Config(secret: String,
                       clientId: String,
                       callbackURL: String,
                       logoutURL: String,
                       domain: String,
                       audience: String)

object Auth0Config {

  def get(configuration: play.api.Configuration) = {
    Auth0Config(
      configuration.get[String]("auth0.clientSecret"),
      configuration.get[String]("auth0.clientId"),
      configuration.get[String]("auth0.callbackURL"),
      configuration.get[String]("auth0.logoutURL"),
      configuration.get[String]("auth0.domain"),
      configuration.get[String]("auth0.audience")
    )
  }
}
