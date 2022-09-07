package com.clluc.stockmind.core.tokens

import java.util.UUID

import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.core.ethereum.{SignableTransaction, TokenFactoryContract}
import com.clluc.stockmind.core.ethereum.solidity.{Address, SolidityString, Uint}
import com.clluc.stockmind.port.primary.TokensPort
import com.clluc.stockmind.port.secondary.{
  Erc20InfoPort,
  Erc721InfoPort,
  EthereumAccountPort,
  EthereumClientPort
}
import com.clluc.stockmind.core.token.AllTokensInfo
import scala.concurrent.{ExecutionContext, Future}

class TokensAdapter(
    erc20InfoPort: Erc20InfoPort,
    erc721InfoPort: Erc721InfoPort,
    ethereumAccountPort: EthereumAccountPort,
    ethereumClientPort: EthereumClientPort,
    tokenFactoryContract: TokenFactoryContract,
    masterAccountPassword: String
)(
    implicit
    val executionContext: ExecutionContext
) extends TokensPort
    with TokenCreationRequestOps[Future] {

  override def supportedErc20Tokens() =
    erc20InfoPort.findAllErc20Tokens()

  override def supportedErc721Tokens() =
    erc721InfoPort.findAllErc721Tokens()

  def allTokensInfo(): Future[AllTokensInfo] = {
    for {
      tokens20  <- supportedErc20Tokens()
      tokens721 <- supportedErc721Tokens()
    } yield AllTokensInfo(tokens20, tokens721)
  }

  override def supportedValidTypes(erc_type: String) =
    erc20InfoPort.findValidTypes(erc_type)

  override def sendContractCreationTx(
      name: String,
      symbol: String,
      initialAmount: String,
      decimals: Int,
      owner: Address,
  ): Future[JsonRpcPlainResult] = {

    val transaction = tokenFactoryContract.createHumanStandardToken(
      initialAmount = Uint(256, BigInt(initialAmount)),
      name = SolidityString(name),
      decimals = Uint(8, decimals),
      symbol = SolidityString(symbol),
      tokenOwner = owner,
    )

    ethereumClientPort.sendTransaction(SignableTransaction(transaction, masterAccountPassword))
  }
  override def sendContract721CreationTx(
      name: String,
      symbol: String,
      owner: Address,
  ): Future[JsonRpcPlainResult] = {

    val transaction = tokenFactoryContract.create721StandardToken(
      name = SolidityString(name),
      symbol = SolidityString(symbol),
      tokenOwner = owner,
    )

    ethereumClientPort.sendTransaction(SignableTransaction(transaction, masterAccountPassword))
  }

  override def sendMintTokenTx(amount: String,
                               tokenAddress: Address): Future[JsonRpcPlainResult] = {

    val transaction = tokenFactoryContract.mintToken(
      amount = Uint(256, BigInt(amount)),
      tokenAddress = tokenAddress,
    )

    ethereumClientPort.sendTransaction(SignableTransaction(transaction, masterAccountPassword))
  }

  override def sendMintToken721Tx(userAddress: Address,
                                  meta: String,
                                  tokenAddress: Address): Future[JsonRpcPlainResult] = {

    val transaction = tokenFactoryContract.mint721Token(
      userAddress = userAddress,
      meta = SolidityString(meta),
      tokenAddress = tokenAddress,
    )

    ethereumClientPort.sendTransaction(SignableTransaction(transaction, masterAccountPassword))
  }

  def sendBurnToken721Tx(id: BigInt, tokenAddress: Address): Future[JsonRpcPlainResult] = {

    val transaction = tokenFactoryContract.burn721Token(
      id = Uint(256, id),
      tokenAddress = tokenAddress,
    )

    ethereumClientPort.sendTransaction(SignableTransaction(transaction, masterAccountPassword))
  }

  override def sendBurnTokenTx(amount: String,
                               tokenAddress: Address): Future[JsonRpcPlainResult] = {

    val transaction = tokenFactoryContract.burnToken(
      amount = Uint(256, BigInt(amount)),
      tokenAddress = tokenAddress,
    )

    ethereumClientPort.sendTransaction(SignableTransaction(transaction, masterAccountPassword))
  }

  override def findAccountByUserId(userId: UUID) = ethereumAccountPort.findAccountByUserId(userId)

  override def findEthereumTokenBySymbolAndType(symbol_erc_type: String) =
    erc20InfoPort.findEthereumTokenBySymbolAndType(symbol_erc_type)

  override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                               erc_type: String,
                                                               ownerAddress: Address) =
    erc20InfoPort.findEthereumTokenBySymbolAndTypeAndOwner(symbol, erc_type, ownerAddress)

  override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) =
    erc721InfoPort.findEthereum721TokenByUniqueId(id, contractOwner)

  override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String) =
    erc20InfoPort.findEthereumTokenByNameAndSymbol(name, erc_type)

  override def createToken(tokenRequest: TokensPort.PostTokenRequest, userId: UUID) = {
    import cats.instances.future._ // Bring Monad[Future] into scope

    createTokenLogic(tokenRequest, userId)
  }
  override def create721Token(tokenRequest: TokensPort.PostErc721Request, userId: UUID) = {
    import cats.instances.future._ // Bring Monad[Future] into scope

    create721TokenLogic(tokenRequest, userId)
  }

  override def mintToken(tokenRequest: TokensPort.PostMintBurnRequest, userId: UUID) = {
    import cats.instances.future._ // Bring Monad[Future] into scope

    mintTokenLogic(tokenRequest, userId)
  }

  override def mint721Token(tokenRequest: TokensPort.PostMint721Request, userId: UUID) = {
    import cats.instances.future._ // Bring Monad[Future] into scope

    mintToken721Logic(tokenRequest, userId)
  }

  override def burn721Token(tokenRequest: TokensPort.PostBurn721Request, userId: UUID) = {
    import cats.instances.future._ // Bring Monad[Future] into scope

    burnToken721Logic(tokenRequest, userId)
  }

  override def burnToken(tokenRequest: TokensPort.PostMintBurnRequest, userId: UUID) = {
    import cats.instances.future._ // Bring Monad[Future] into scope

    burnTokenLogic(tokenRequest, userId)
  }
}
