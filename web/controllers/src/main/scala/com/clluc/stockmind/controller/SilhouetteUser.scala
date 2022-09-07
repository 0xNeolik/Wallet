package com.clluc.stockmind.controller

import java.util.UUID

import com.clluc.stockmind.core.auth.{LoginInfo => CoreLoginInfo}
import com.clluc.stockmind.core.user.{LocalDirectoryData, User => CoreUser}
import com.mohiva.play.silhouette.api.{Identity, LoginInfo => SilhouetteLoginInfo}

import scala.language.implicitConversions

private[controller] case class SilhouetteUser(
    userID: UUID,
    loginInfo: CoreLoginInfo,
    directoryData: LocalDirectoryData,
) extends Identity

private[controller] object SilhouetteUser {
  private[controller] object BoundedContextImplicitConversions {

    implicit def asSilhouetteLoginInfo(l: CoreLoginInfo): SilhouetteLoginInfo =
      SilhouetteLoginInfo(
        l.providerID,
        l.providerKey
      )

    implicit def asSilhouetteUser(u: CoreUser): SilhouetteUser =
      SilhouetteUser(
        u.userID,
        u.loginInfo,
        u.directoryData
      )
  }
}
