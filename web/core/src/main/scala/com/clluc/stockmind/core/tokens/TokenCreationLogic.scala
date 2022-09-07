package com.clluc.stockmind.core.tokens

import cats.Monad
import cats.data.EitherT
import TokenCreationOps.Error
import akka.actor.ActorRef
import com.clluc.stockmind.core.ethereum.{Ethtoken, LoggedEvent}

private[core] trait TokenCreationLogic {

  private val ops = TokenCreationOps.Syntax

  def registerNewToken[P[_]: TokenCreationOps: Monad](
      newTokenEvent: LoggedEvent,
      watcher: ActorRef): EitherT[P, Error, Ethtoken] = {
    for {
      tokenAddress  <- ops.getTokenAddress(newTokenEvent)
      tokenName     <- ops.getTokenName(tokenAddress)
      tokenSymbol   <- ops.getTokenSymbol(tokenAddress)
      tokenDecimals <- ops.getTokenDecimals(tokenAddress)
      owner         <- ops.getTokenOwnerAddress(newTokenEvent)
      birthBlock    <- ops.getTokenBirthBlock(newTokenEvent)
      token <- ops.buildErc20Token(tokenName,
                                   tokenSymbol,
                                   tokenDecimals,
                                   tokenAddress,
                                   owner.value,
                                   birthBlock.blockNumber)
      storedToken <- ops.writeErc20Token(token)
      _ = ops.notifyTransferWatcher(Ethtoken(token.symbol,
                                             token.erc_type,
                                             token.name,
                                             token.contract,
                                             token.owner,
                                             token.birthBlock),
                                    watcher)
    } yield {
      Ethtoken(token.symbol,
               token.erc_type,
               token.name,
               token.contract,
               token.owner,
               token.birthBlock)
    }
  }

  def registerNewCollection721[P[_]: TokenCreationOps: Monad](
      newTokenEvent: LoggedEvent,
      watcher: ActorRef
  ): EitherT[P, Error, Ethtoken] = {
    for {
      tokenAddress <- ops.getTokenAddress(newTokenEvent)
      tokenName    <- ops.get721TokenName(tokenAddress)
      tokenSymbol  <- ops.get721TokenSymbol(tokenAddress)
      owner        <- ops.getTokenOwnerAddress(newTokenEvent)
      birthBlock   <- ops.getTokenBirthBlock(newTokenEvent)
      token <- ops.buildErc721Token(tokenName,
                                    tokenSymbol,
                                    tokenAddress,
                                    owner.value,
                                    birthBlock.blockNumber)
      storedToken <- ops.writeErc721Token(token)
      _ = ops.notifyTransferWatcher(Ethtoken(token.symbol,
                                             token.erc_type,
                                             token.name,
                                             token.contract,
                                             token.owner,
                                             token.birthBlock),
                                    watcher)
    } yield {
      storedToken
    }
  }

}
