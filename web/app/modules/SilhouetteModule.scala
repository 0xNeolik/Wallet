package modules

import com.clluc.stockmind.controller._
import com.clluc.stockmind.silhouette.CustomCasProvider
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.actions.{SecuredErrorHandler, UnsecuredErrorHandler}
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto._
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{
  CookieSecretProvider,
  CookieSecretSettings
}
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
  * The Guice module which wires all Silhouette dependencies.
  */
class SilhouetteModule extends AbstractModule with ScalaModule {

  private val config = ConfigFactory.load

  /**
    * Configures the module.
    */
  def configure() {
    bind[CacheLayer].to[PlayCacheLayer]
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[SilhouetteUserDao].asEagerSingleton()
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())

    bind[DelegableAuthInfoDAO[OAuth1Info]].to[Oauth1InfoDao]
    bind[DelegableAuthInfoDAO[CasInfo]].to[CasInfoDao]
  }

  /**
    * Provides the HTTP layer implementation.
    *
    * @param client Play's WS client.
    * @return The HTTP layer implementation.
    */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
    * Provides the Silhouette environment.
    *
    * @param silhouetteUserDao The user service implementation.
    * @param authenticatorService The authentication service implementation.
    * @param eventBus The event bus instance.
    * @return The Silhouette environment.
    */
  @Provides
  def provideEnvironment(
      silhouetteUserDao: SilhouetteUserDao,
      authenticatorService: AuthenticatorService[JWTAuthenticator],
      eventBus: EventBus
  ): Environment[DefaultEnv] = {

    Environment[DefaultEnv](
      silhouetteUserDao,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  /**
    * Provides the social provider registry.
    *
    * @param twitterProvider The Twitter provider implementation.
    * @return The Silhouette environment.
    */
  @Provides
  def provideSocialProviderRegistry(
      twitterProvider: TwitterProvider,
      casProvider: CustomCasProvider
  ): SocialProviderRegistry = {
    SocialProviderRegistry(Seq(twitterProvider, casProvider))
  }

  /**
    * Provides the signer for the OAuth1 token secret provider.
    *
    * @param configuration The Play configuration.
    * @return The signer for the OAuth1 token secret provider.
    */
  @Provides @Named("oauth1-token-secret-signer")
  def provideOAuth1TokenSecretCookieSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying
      .as[JcaSignerSettings]("silhouette.oauth1TokenSecretProvider.cookie.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the crypter for the OAuth1 token secret provider.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the OAuth1 token secret provider.
    */
  @Provides @Named("oauth1-token-secret-crypter")
  def provideOAuth1TokenSecretCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying
      .as[JcaCrypterSettings]("silhouette.oauth1TokenSecretProvider.crypter")

    new JcaCrypter(config)
  }

  /**
    * Provides the cookie signer for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The cookie signer for the authenticator.
    */
  @Provides @Named("authenticator-cookie-signer")
  def provideAuthenticatorCookieSigner(configuration: Configuration): Signer = {
    val config =
      configuration.underlying.as[JcaSignerSettings]("silhouette.authenticator.cookie.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the crypter for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the authenticator.
    */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  /**
    * Provides the auth info repository.
    *
    * @param oauth1InfoDao The implementation of the delegable OAuth1 auth info DAO.
    * @param casInfoDao The implementation of the delegable CAS info DAO.
    * @return The auth info repository instance.
    */
  @Provides
  def provideAuthInfoRepository(oauth1InfoDao: DelegableAuthInfoDAO[OAuth1Info],
                                casInfoDao: DelegableAuthInfoDAO[CasInfo]): AuthInfoRepository = {
    new DelegableAuthInfoRepository(oauth1InfoDao, casInfoDao)
  }

  @Provides
  def provideJwtAuthenticatorService(
      @Named("authenticator-crypter") crypter: Crypter,
      idGenerator: IDGenerator,
      clock: Clock
  ): AuthenticatorService[JWTAuthenticator] = {

    val settings = JWTAuthenticatorSettings(
      authenticatorIdleTimeout =
        Try(config.getDuration("silhouette.authenticator.authenticatorIdleTimeout")).toOption
          .map(_.getSeconds)
          .map(FiniteDuration(_, SECONDS)),
      sharedSecret = config.getString("silhouette.authenticator.sharedSecret")
    )
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(settings, None, encoder, idGenerator, clock)
  }

  /**
    * Provides the avatar service.
    *
    * @param httpLayer The HTTP layer implementation.
    * @return The avatar service implementation.
    */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
    * Provides the OAuth1 token secret provider.
    *
    * @param cookieSigner The cookie signer implementation.
    * @param crypter The crypter implementation.
    * @param configuration The Play configuration.
    * @param clock The clock instance.
    * @return The OAuth1 token secret provider implementation.
    */
  @Provides
  def provideOAuth1TokenSecretProvider(
      @Named("oauth1-token-secret-signer") signer: Signer,
      @Named("oauth1-token-secret-crypter") crypter: Crypter,
      configuration: Configuration,
      clock: Clock
  ): OAuth1TokenSecretProvider = {

    val settings =
      configuration.underlying.as[CookieSecretSettings]("silhouette.oauth1TokenSecretProvider")
    new CookieSecretProvider(settings, signer, crypter, clock)
  }

  /**
    * Provides the Twitter provider.
    *
    * @param httpLayer The HTTP layer implementation.
    * @param tokenSecretProvider The token secret provider implementation.
    * @param configuration The Play configuration.
    * @return The Twitter provider.
    */
  @Provides
  def provideTwitterProvider(
      httpLayer: HTTPLayer,
      tokenSecretProvider: OAuth1TokenSecretProvider,
      configuration: Configuration
  ): TwitterProvider = {

    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.twitter")
    new TwitterProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  @Provides
  def provideCasProvider(
      httpLayer: HTTPLayer,
      configuration: Configuration
  ): CustomCasProvider = {

    // Workaround to quick & dirty solve the Protocol auto-generated parser issue
    // Parse the configuration into a case class we have control over and the create
    // the CasSettings instance from it.
    case class LocalSettings(
        casURL: String,
        redirectURL: String,
        encoding: String,
        acceptAnyProxy: Boolean,
        samlTimeTolerance: FiniteDuration
    )

    val settings: LocalSettings = configuration.underlying.as[LocalSettings]("silhouette.cas")

    implicit class LocalToCasSettings(local: LocalSettings) {
      def toCasSettings: CasSettings = CasSettings(
        local.casURL,
        local.redirectURL,
        local.encoding,
        local.acceptAnyProxy,
        local.samlTimeTolerance,
        CasProtocol.CAS30
      )
    }

    val caseSettings = settings.toCasSettings

    new CustomCasProvider(
      httpLayer,
      caseSettings,
      new CasClient(
        caseSettings
      )
    )
  }

}
