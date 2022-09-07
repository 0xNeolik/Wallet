package com.clluc.stockmind.core.tokens

import cats.data.EitherT
import TokenCreationOps.Error
import akka.actor.ActorRef
import com.clluc.stockmind.core.ethereum.solidity.{Address}
import com.clluc.stockmind.core.ethereum.{Block, Erc20Token, Ethtoken, LoggedEvent}

private[core] trait TokenCreationOps[P[_]] {

  def getTokenAddress(event: LoggedEvent): EitherT[P, Error, Address]

  def getTokenOwnerAddress(event: LoggedEvent): EitherT[P, Error, Address]

  def getTokenBirthBlock(event: LoggedEvent): EitherT[P, Error, Block]

  def getTokenName(tokenAddress: Address): EitherT[P, Error, String]

  def get721TokenName(tokenAddress: Address): EitherT[P, Error, String]

  def getTokenSymbol(tokenAddress: Address): EitherT[P, Error, String]

  def get721TokenSymbol(tokenAddress: Address): EitherT[P, Error, String]

  def getTokenDecimals(tokenAddress: Address): EitherT[P, Error, Int]

  def buildErc20Token(
      name: String,
      symbol: String,
      decimals: Int,
      contract: Address,
      owner: String,
      birthBlock: Int
  ): EitherT[P, Error, Erc20Token]

  def buildErc721Token(
      name: String,
      symbol: String,
      contract: Address,
      owner: String,
      birthBlock: Int
  ): EitherT[P, Error, Ethtoken]

  def writeErc20Token(token: Erc20Token): EitherT[P, Error, Erc20Token]

  def writeErc721Token(token: Ethtoken): EitherT[P, Error, Ethtoken]

  def notifyTransferWatcher(token: Ethtoken, watcher: ActorRef)

}

private[tokens] object TokenCreationOps {

  sealed trait Error
  case class CannotDecodeAddress(rawData: String)               extends Error
  case class EthereumClientError(statusCode: Int, body: String) extends Error
  case class CannotDecodeResult(string: String)                 extends Error
  case class DatabaseError(reason: String)                      extends Error

  object ErrorConstructors {
    def cannotDecodeAddress(rawData: String): Error = CannotDecodeAddress(rawData)

    def ethereumClientError(statusCode: Int, body: String): Error =
      EthereumClientError(statusCode, body)
    def cannotDecodeResult(string: String): Error = CannotDecodeResult(string)
    def databaseError(reason: String): Error      = DatabaseError(reason)
  }

  object Syntax {

    def getTokenAddress[P[_]](event: LoggedEvent)(implicit ev: TokenCreationOps[P]) =
      ev.getTokenAddress(event)

    def getTokenOwnerAddress[P[_]](event: LoggedEvent)(implicit ev: TokenCreationOps[P]) =
      ev.getTokenOwnerAddress(event)

    def getTokenBirthBlock[P[_]](event: LoggedEvent)(implicit ev: TokenCreationOps[P]) =
      ev.getTokenBirthBlock(event)

    def getTokenName[P[_]](tokenAddress: Address)(implicit ev: TokenCreationOps[P]) =
      ev.getTokenName(tokenAddress)

    def getTokenSymbol[P[_]](tokenAddress: Address)(implicit ev: TokenCreationOps[P]) =
      ev.getTokenSymbol(tokenAddress)

    def get721TokenName[P[_]](tokenAddress: Address)(implicit ev: TokenCreationOps[P]) =
      ev.get721TokenName(tokenAddress)

    def get721TokenSymbol[P[_]](tokenAddress: Address)(implicit ev: TokenCreationOps[P]) =
      ev.get721TokenSymbol(tokenAddress)

    def getTokenDecimals[P[_]](tokenAddress: Address)(implicit ev: TokenCreationOps[P]) =
      ev.getTokenDecimals(tokenAddress)

    def buildErc20Token[P[_]](name: String,
                              symbol: String,
                              decimals: Int,
                              contract: Address,
                              owner: String,
                              birthBlock: Int)(implicit ev: TokenCreationOps[P]) =
      ev.buildErc20Token(name, symbol, decimals, contract, owner, birthBlock)

    def buildErc721Token[P[_]](name: String,
                               symbol: String,
                               contract: Address,
                               owner: String,
                               birthBlock: Int)(implicit ev: TokenCreationOps[P]) =
      ev.buildErc721Token(name, symbol, contract, owner, birthBlock)

    def writeErc20Token[P[_]](token: Erc20Token)(implicit ev: TokenCreationOps[P]) =
      ev.writeErc20Token(token)

    def writeErc721Token[P[_]](token: Ethtoken)(implicit ev: TokenCreationOps[P]) =
      ev.writeErc721Token(token)

    def notifyTransferWatcher[P[_]](token: Ethtoken, watcher: ActorRef)(
        implicit ev: TokenCreationOps[P]): Unit =
      ev.notifyTransferWatcher(token, watcher)
  }

}
