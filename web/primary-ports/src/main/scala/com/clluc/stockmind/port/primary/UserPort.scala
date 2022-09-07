package com.clluc.stockmind.port.primary

import java.util.UUID

import cats.data.EitherT
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.user.{LocalDirectoryData, User, UserInfo, UserOperationError}

import scala.concurrent.Future

trait UserPort {

  def findUserInfo(userId: UUID,
                   data: LocalDirectoryData): EitherT[Future, UserOperationError, UserInfo]

  // Used by Silhouette user dao
  def findFromLoginInfo(loginInfo: LoginInfo): Future[Option[User]]

  def findFromId(userId: UUID): Future[Option[User]]

  def findFromApiKey(api_key: UUID): Future[Option[User]]

  def setApiKey(userId: UUID, apikey: UUID): Future[UUID]

  def deleteApiKey(userId: UUID, apikey: UUID): Future[UUID]

  def findUsersByName(name: String, page: Int): Future[List[User]]

}
