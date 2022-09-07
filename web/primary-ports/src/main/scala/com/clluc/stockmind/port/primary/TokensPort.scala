package com.clluc.stockmind.port.primary

import java.util.UUID

import com.clluc.stockmind.core.ethereum.Ethtoken
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.{
  JsonRpcPlainResult,
  UnexpectedEthereumResponse
}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.token.AllTokensInfo
import com.clluc.stockmind.port.primary.TokensPort.{
  PostBurn721Request,
  PostErc721Request,
  PostMint721Request,
  PostMintBurnRequest,
  PostTokenRequest,
  ValidatedTokenCreationTxHash,
  ValidatedTokenMintBurnTxHash
}

import scala.concurrent.Future

/**
  * Port to handle tokens resource operations.
  * As it is a primary port it will expose core business logic operations to the outer world.
  */
trait TokensPort {

  def supportedErc20Tokens(): Future[List[Ethtoken]]
  def supportedErc721Tokens(): Future[List[Ethtoken]]

  def allTokensInfo(): Future[AllTokensInfo]

  def supportedValidTypes(erc_type: String): Future[Option[String]]

  def sendContractCreationTx(
      name: String,
      symbol: String,
      initialAmount: String,
      decimals: Int,
      owner: Address,
  ): Future[JsonRpcPlainResult]

  def sendMintTokenTx(
      amount: String,
      owner: Address,
  ): Future[JsonRpcPlainResult]

  def sendBurnTokenTx(
      amount: String,
      owner: Address,
  ): Future[JsonRpcPlainResult]

  def createToken(tokenRequest: PostTokenRequest,
                  userId: UUID): Future[ValidatedTokenCreationTxHash]

  def create721Token(tokenRequest: PostErc721Request,
                     userId: UUID): Future[ValidatedTokenCreationTxHash]

  def mintToken(tokenRequest: PostMintBurnRequest,
                userId: UUID): Future[ValidatedTokenMintBurnTxHash]

  def mint721Token(tokenRequest: PostMint721Request,
                   userId: UUID): Future[ValidatedTokenMintBurnTxHash]

  def burn721Token(tokenRequest: PostBurn721Request,
                   userId: UUID): Future[ValidatedTokenMintBurnTxHash]

  def burnToken(tokenRequest: PostMintBurnRequest,
                userId: UUID): Future[ValidatedTokenMintBurnTxHash]

}

object TokensPort {
  type TxHash   = String
  type erc_type = String

  type ValidatedTokenCreationTxHash = Either[TokenCreationError, TxHash]
  type ValidatedTokenMintBurnTxHash = Either[TokenMintBurnError, TxHash]

  case class PostTokenRequest(name: String, symbol: String, initialAmount: String, decimals: Int)

  case class PostErc721Request(name: String, symbol: String)
  case class PostMintBurnRequest(erc_type: String, symbol: String, amount: String)
  case class PostMint721Request(symbol: String, metadata: String)
  case class PostBurn721Request(id: BigInt)

  sealed trait TokenCreationError
  case object NoEthAccountFoundForUser                        extends TokenCreationError
  case class TokenWithSymbolAlreadyExists(symbol: String)     extends TokenCreationError
  case class TokenWithNameAlreadyExists(name: String)         extends TokenCreationError
  case class EthereumError(error: UnexpectedEthereumResponse) extends TokenCreationError

  def noEthAccountFoundForUser: TokenCreationError = NoEthAccountFoundForUser

  def tokenWithSymbolAlreadyExists(symbol: String): TokenCreationError =
    TokenWithSymbolAlreadyExists(symbol)

  def tokenWithNameAlreadyExists(name: String): TokenCreationError =
    TokenWithNameAlreadyExists(name)

  def ethereumError(error: UnexpectedEthereumResponse): TokenCreationError = EthereumError(error)

  sealed trait TokenMintBurnError
  case object NoEthAccountFoundForUserM               extends TokenMintBurnError
  case class TokenWithSymbolNotExists(symbol: String) extends TokenMintBurnError
  case class TokenWithIdNotExists(id: BigInt)         extends TokenMintBurnError

  case class EthereumErrorMintBurn(error: UnexpectedEthereumResponse) extends TokenMintBurnError

  sealed trait TokenTypeError
  case class NoErcTypeFound(erc_type: String) extends TokenTypeError

  def noErcFound(erc_type: String): TokenTypeError =
    NoErcTypeFound(erc_type)

  def noEthAccountFoundForUserM: TokenMintBurnError = NoEthAccountFoundForUserM

  def tokenWithSymbolNotExists(symbol: String): TokenMintBurnError =
    TokenWithSymbolNotExists(symbol)

  def tokenWithIdNotExists(id: BigInt): TokenMintBurnError =
    TokenWithIdNotExists(id)

  def ethereumErrorMintBurn(error: UnexpectedEthereumResponse): TokenMintBurnError =
    EthereumErrorMintBurn(error)
}
