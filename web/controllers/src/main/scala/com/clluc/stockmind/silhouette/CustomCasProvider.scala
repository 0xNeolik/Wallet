package com.clluc.stockmind.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers._
import com.typesafe.scalalogging.LazyLogging
import org.jasig.cas.client.authentication.AttributePrincipal

import scala.collection.JavaConverters._
import scala.concurrent.Future

trait CustomSocialProfileBuilder {
  self: SocialProfileBuilder =>

  type Profile = CustomSocialProfile
}

class CustomCasProvider(
    protected val httpLayer: HTTPLayer,
    val settings: CasSettings,
    val client: CasClient,
) extends BaseCasProvider
    with CustomSocialProfileBuilder {

  override type Self = CustomCasProvider

  override val profileParser = new ExtendedCasProfileParser

  override def withSettings(f: (Settings) => Settings) = {
    new CustomCasProvider(httpLayer, f(settings), client.withSettings(f))
  }
}

class ExtendedCasProfileParser
    extends SocialProfileParser[AttributePrincipal, CustomSocialProfile, CasInfo]
    with LazyLogging
    with CasProviderConstants {

  /**
    * Parses the CAS profile.
    *
    * @param principal The principal returned from the provider.
    * @param authInfo  The auth info to query the provider again for additional data.
    * @return The CAS profile from given result.
    */
  def parse(principal: AttributePrincipal, authInfo: CasInfo) = Future.successful {

    // The attributes map holds either strings or lists.
    //
    // Check 'Cas20ServiceTicketValidator -> CustomAttributeHandler' to see how
    // they are generated from an XML response.
    val attributes = principal.getAttributes

    if (logger.underlying.isDebugEnabled) { // avoid mapping over the attributes needlessly
      logger.debug("AttributePrincipal, attributes:")
      attributes.asScala.foreach {
        case (key, value) => logger.debug("key: [%s], value: [%s]".format(key, value))
      }
    }

    val attrMap = attributes.asScala.map {
      case (key, value) =>
        value match {
          case s: String =>
            (key, s)
          case l: java.util.List[_] =>
            (key, l.asInstanceOf[List[String]].toString)
          case o =>
            (key, o.toString)
        }
    }

    CustomSocialProfile(LoginInfo(ID, principal.getName), attrMap.toMap)
  }
}
