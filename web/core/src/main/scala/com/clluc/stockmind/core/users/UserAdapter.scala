package com.clluc.stockmind.core.users

import java.util.UUID

import cats.data.EitherT
import cats.instances.future._
import com.clluc.stockmind.core.auth.LoginInfo
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.{LocalDirectoryData, User, UserOperationError}
import com.clluc.stockmind.port.primary.UserPort
import com.clluc.stockmind.port.secondary
import secondary.{
  Erc20InfoPort,
  EthereumAccountPort,
  OffchainBalancePort,
  Erc721InfoPort,
  UsersRepositoryPort => SecondaryUserPort
}

import scala.concurrent.{ExecutionContext, Future}

private[users] class UserAdapter(
    ethereumAccountPort: EthereumAccountPort,
    balancePort: OffchainBalancePort,
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    userPort: SecondaryUserPort
)(
    implicit
    executionContext: ExecutionContext
) extends UserPort
    with UserOps[Future] {

  private def catchAllIntoIOError[T]: PartialFunction[Throwable, Either[UserOperationError, T]] = {
    case e: Exception => Left(UserOperationError.ioError(e.getMessage))
  }

  override def findEthereumAccountByUserId(userId: UUID) =
    EitherT(
      ethereumAccountPort
        .findAccountByUserId(userId)
        .map(_.toRight(UserOperationError.userWithoutEthereumAccountInSystem(userId)))
        .recover(catchAllIntoIOError)
    )

  override def findBalancesForEthereumAddress(ethAddress: Address) =
    EitherT(
      balancePort
        .findBalancesForAddress(ethAddress)
        .map(Right(_))
        .recover(catchAllIntoIOError)
    )

  override def findBalances271ForEthereumAddress(ethAddress: Address) =
    EitherT(
      balancePort
        .findBalances721tokensForAddress(ethAddress)
        .map(Right(_))
        .recover(catchAllIntoIOError)
    )

  override def findAllErc20Tokens() =
    EitherT(
      erc20InfoPort
        .findAllErc20Tokens()
        .map(Right(_))
        .recover(catchAllIntoIOError)
    )

  override def findFromLoginInfo(loginInfo: LoginInfo): Future[Option[User]] = {
    userPort.retrieve(loginInfo)
  }

  override def findFromId(userId: UUID): Future[Option[User]] = {
    userPort.find(userId)
  }

  override def findFromApiKey(api_key: UUID): Future[Option[User]] = {
    userPort.findByApiKey(api_key)
  }

  override def findUserInfo(userId: UUID, data: LocalDirectoryData) =
    userInfo(userId, data)

  override def setApiKey(userId: UUID, apiKey: UUID): Future[UUID] = {
    userPort.storeApiKey(userId, apiKey)
  }

  override def deleteApiKey(userId: UUID, apiKey: UUID): Future[UUID] = {
    userPort.removeApiKey(userId, apiKey)
  }

  override def findUsersByName(name: String, page: Int): Future[List[User]] = {
    userPort.findByquery(name, page)
  }
}

object UserAdapter {

  def apply(
      ethereumAccountPort: EthereumAccountPort,
      balancePort: OffchainBalancePort,
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      secondaryUserPort: SecondaryUserPort
  )(
      implicit
      executionContext: ExecutionContext
  ): UserAdapter =
    new UserAdapter(
      ethereumAccountPort,
      balancePort,
      erc20InfoPort,
      erc721InfoPort,
      secondaryUserPort
    )
}
