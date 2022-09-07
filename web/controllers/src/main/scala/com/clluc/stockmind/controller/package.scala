package com.clluc.stockmind

import java.util.UUID

import com.clluc.stockmind.core.auth.{LoginInfo => CoreLoginInfo, OAuth1Info => CoreOAuth1Info}
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.impl.providers.{OAuth1Info => SilhouetteOAuth1Info}
import com.mohiva.play.silhouette.api.{LoginInfo => SilhouetteLoginInfo}
import play.api.mvc.AnyContent

package object controller {
  private[controller] type RequestParsingResult[ParsingType] =
    Option[Either[RequestParsingError, ParsingType]]

  private[controller] def fromCirceErrorToRequestParsingError[T](
      circeError: Either[io.circe.Error, T]): Either[RequestParsingError, T] =
    circeError.left.map(err => RequestParsingError(err.fillInStackTrace()))

  private[controller] def extractUserId(request: SecuredRequest[DefaultEnv, AnyContent]): UUID =
    request.identity.userID

  private[controller] object BoundedContextImplicitConversions {

    implicit class FromSilhouetteToCoreOAuth1Info(oAuthInfo: SilhouetteOAuth1Info) {

      def toCoreOauth1Info: CoreOAuth1Info =
        CoreOAuth1Info(
          oAuthInfo.token,
          oAuthInfo.secret
        )
    }

    implicit class FromSilhouetteToCoreLoginInfo(info: SilhouetteLoginInfo) {

      def toCoreLoginInfo: CoreLoginInfo =
        CoreLoginInfo(
          info.providerID,
          info.providerKey
        )
    }

    implicit class FromCoreToSilhouetteOAuth1Info(oAuth1Info: CoreOAuth1Info) {

      def toSilhouetteOauthInfo: SilhouetteOAuth1Info =
        SilhouetteOAuth1Info(
          oAuth1Info.token,
          oAuth1Info.secret
        )
    }

    implicit class FromCoreToSilhouetteLoginInfo(li: CoreLoginInfo) {

      def toSilhouetteLoginInfo: SilhouetteLoginInfo =
        SilhouetteLoginInfo(
          li.providerID,
          li.providerKey
        )
    }

  }
}
