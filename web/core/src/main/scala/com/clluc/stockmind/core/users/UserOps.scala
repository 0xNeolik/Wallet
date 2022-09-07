package com.clluc.stockmind.core.users

import java.util.UUID

import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.ethereum.{Erc721Token, EthereumAccount, Ethtoken}
import com.clluc.stockmind.core.user._

import cats.Monad
import cats.data.EitherT

private[users] trait UserOps[P[_]] {

  /*
   * Primitive operations
   */
  def findEthereumAccountByUserId(userId: UUID): EitherT[P, UserOperationError, EthereumAccount]

  def findBalancesForEthereumAddress(
      ethAddress: Address): EitherT[P, UserOperationError, List[Balance]]

  def findBalances271ForEthereumAddress(
      ethAddress: Address): EitherT[P, UserOperationError, List[Erc721Token]]

  def findAllErc20Tokens(): EitherT[P, UserOperationError, List[Ethtoken]]

  /*
   * Derived operations

  private def balances(userId: UUID)(
      implicit ev: Monad[P]): EitherT[P, UserOperationError, List[Balance]] =
    for {
      ethAccount <- findEthereumAccountByUserId(userId)
      balances   <- findBalancesForEthereumAddress(ethAccount.address)
    } yield balances

  private def erc721Tokens(userId: UUID)(
      implicit ev: Monad[P]): EitherT[P, UserOperationError, List[Erc721Token]] =
    for {
      ethAccount     <- findEthereumAccountByUserId(userId)
      erc721balances <- findErc271ForEthereumAddress(ethAccount.address)
    } yield erc721balances
   */
  def userInfo(
      userId: UUID,
      data: LocalDirectoryData
  )(implicit ev: Monad[P]): EitherT[P, UserOperationError, UserInfo] = {
    for {
      ethAccount     <- findEthereumAccountByUserId(userId)
      balances       <- findBalancesForEthereumAddress(ethAccount.address)
      erc721balances <- findBalances271ForEthereumAddress(ethAccount.address)
    } yield UserInfo(data.data, balances, erc721balances)
  }
}
