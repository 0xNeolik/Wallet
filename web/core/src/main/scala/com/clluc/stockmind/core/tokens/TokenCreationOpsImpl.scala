package com.clluc.stockmind.core.tokens

import java.sql.SQLException

import com.clluc.stockmind.core.actor.EventWatcherActor.WatchNewEthToken
import com.clluc.stockmind.core.ethereum.solidity.{Address, SolidityString}
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.port.secondary.{Erc20InfoPort, Erc721InfoPort, EthereumClientPort}
import akka.actor.ActorRef
import cats.data.EitherT
import cats.syntax.either._
import cats.instances.future._
import com.clluc.stockmind.core.ethereum.{
  Block,
  Erc20Token,
  HumanStandardTokenContract,
  LoggedEvent
}
import scala.util.Try

import scala.concurrent.{ExecutionContext, Future}

private[core] class TokenCreationOpsImpl(
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    ethereumClientPort: EthereumClientPort,
)(
    implicit
    executionContext: ExecutionContext
) extends TokenCreationOps[Future] {

  private val errors = TokenCreationOps.ErrorConstructors

  override def getTokenAddress(
      event: LoggedEvent): EitherT[Future, TokenCreationOps.Error, Address] =
    EitherT(
      Future.successful(Try(Address.decode(event.data)).toEither)
    ).leftMap(_ => errors.cannotDecodeAddress(event.data))

  override def getTokenOwnerAddress(
      event: LoggedEvent): EitherT[Future, TokenCreationOps.Error, Address] =
    EitherT(
      Future.successful(Try(Address.decode(event.topics(1))).toEither)
    ).leftMap(_ => errors.cannotDecodeAddress(event.topics(1)))

  override def getTokenBirthBlock(
      event: LoggedEvent): EitherT[Future, TokenCreationOps.Error, Block] =
    EitherT.pure(
      event.block
    )

  override def getTokenName(
      tokenAddress: Address): EitherT[Future, TokenCreationOps.Error, String] = {
    val contract = HumanStandardTokenContract(tokenAddress)

    EitherT(ethereumClientPort.callMethod(contract.name))
      .leftMap(fail => errors.ethereumClientError(fail.statusCode, fail.networkResponseBody))
      .subflatMap { response =>
        val encodedString = response.drop(66)
        val decodedO      = SolidityString.decodeDynamicEncoded(encodedString)
        Either.fromOption(decodedO, errors.cannotDecodeResult(encodedString))
      }
  }

  override def get721TokenName(
      tokenAddress: Address): EitherT[Future, TokenCreationOps.Error, String] = {
    val contract = NFTTokenContract(tokenAddress)

    EitherT(ethereumClientPort.callMethod(contract.name))
      .leftMap(fail => errors.ethereumClientError(fail.statusCode, fail.networkResponseBody))
      .subflatMap { response =>
        val encodedString = response.drop(66)
        val decodedO      = SolidityString.decodeDynamicEncoded(encodedString)
        Either.fromOption(decodedO, errors.cannotDecodeResult(encodedString))
      }
  }

  override def getTokenSymbol(
      tokenAddress: Address): EitherT[Future, TokenCreationOps.Error, String] = {
    val contract = HumanStandardTokenContract(tokenAddress)

    EitherT(ethereumClientPort.callMethod(contract.symbol))
      .leftMap(fail => errors.ethereumClientError(fail.statusCode, fail.networkResponseBody))
      .subflatMap { response =>
        val encodedString = response.drop(66)
        val decodedO      = SolidityString.decodeDynamicEncoded(encodedString)
        Either.fromOption(decodedO, errors.cannotDecodeResult(encodedString))
      }
  }

  override def get721TokenSymbol(
      tokenAddress: Address): EitherT[Future, TokenCreationOps.Error, String] = {
    val contract = NFTTokenContract(tokenAddress)

    EitherT(ethereumClientPort.callMethod(contract.symbol))
      .leftMap(fail => errors.ethereumClientError(fail.statusCode, fail.networkResponseBody))
      .subflatMap { response =>
        val encodedString = response.drop(66)
        val decodedO      = SolidityString.decodeDynamicEncoded(encodedString)
        Either.fromOption(decodedO, errors.cannotDecodeResult(encodedString))
      }
  }

  override def getTokenDecimals(
      tokenAddress: Address): EitherT[Future, TokenCreationOps.Error, Int] = {
    val contract = HumanStandardTokenContract(tokenAddress)

    EitherT(ethereumClientPort.callMethod(contract.decimals))
      .leftMap(fail => errors.ethereumClientError(fail.statusCode, fail.networkResponseBody))
      .subflatMap { response =>
        val encodedNumber = response.drop(2)
        Either
          .fromTry(Try(Integer.parseInt(encodedNumber, 16)))
          .leftMap(_ => errors.cannotDecodeResult(encodedNumber))
      }
  }

  override def buildErc20Token(
      name: String,
      symbol: String,
      decimals: Int,
      contract: Address,
      owner: String,
      birthBlock: Int): EitherT[Future, TokenCreationOps.Error, Erc20Token] = {

    EitherT.pure[Future, TokenCreationOps.Error, Erc20Token](
      Erc20Token(symbol, "ERC-20", name, decimals, contract, Some(owner), Some(birthBlock))
    )
  }
  override def buildErc721Token(
      name: String,
      symbol: String,
      contract: Address,
      owner: String,
      birthBlock: Int): EitherT[Future, TokenCreationOps.Error, Ethtoken] = {

    EitherT.pure[Future, TokenCreationOps.Error, Ethtoken](
      Ethtoken(symbol, "ERC-721", name, contract, Some(owner), Some(birthBlock))
    )
  }

  override def writeErc20Token(
      token: Erc20Token): EitherT[Future, TokenCreationOps.Error, Erc20Token] = {
    EitherT(
      erc20InfoPort
        .createEthereumToken(token)
        .map(Right(_))
        .recover { case e: SQLException => Left(errors.databaseError(e.getMessage)) }
    )
  }

  override def writeErc721Token(
      token: Ethtoken): EitherT[Future, TokenCreationOps.Error, Ethtoken] = {
    EitherT(
      erc721InfoPort
        .create721CollectionToken(token)
        .map(Right(_))
        .recover { case e: SQLException => Left(errors.databaseError(e.getMessage)) }
    )
  }

  override def notifyTransferWatcher(token: Ethtoken, watcher: ActorRef): Unit =
    watcher ! WatchNewEthToken(token)
}
