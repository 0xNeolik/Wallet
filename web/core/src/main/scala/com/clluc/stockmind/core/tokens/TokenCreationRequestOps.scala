package com.clluc.stockmind.core.tokens

import java.util.UUID

import cats.{Functor, Monad}
import cats.data.{EitherT, OptionT}
import com.clluc.stockmind.core.ethereum.{EthereumAccount, Ethtoken}
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.port.primary.TokensPort._
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult

private[tokens] trait TokenCreationRequestOps[P[_]] {

  private def findEthereumAccountAddressOf(userId: UUID)(
      implicit ev: Functor[P]): P[Option[Address]] =
    OptionT(findAccountByUserId(userId))
      .map(_.address)
      .value

  def findAccountByUserId(userId: UUID): P[Option[EthereumAccount]]

  def findEthereumTokenBySymbolAndType(symbol_erc_type: String): P[Option[Ethtoken]]

  def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                      erc_type: String,
                                                      ownerAddress: Address): P[Option[Ethtoken]]
  def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address): P[Option[Ethtoken]]

  def findEthereumTokenByNameAndSymbol(name: String, erc_type: String): P[Option[Ethtoken]]

  def sendContractCreationTx(name: String,
                             symbol: String,
                             initialAmount: String,
                             decimals: Int,
                             ownerAddress: Address): P[JsonRpcPlainResult]

  def sendContract721CreationTx(name: String,
                                symbol: String,
                                ownerAddress: Address): P[JsonRpcPlainResult]

  def sendMintTokenTx(amount: String, tokenAddress: Address): P[JsonRpcPlainResult]

  def sendMintToken721Tx(userAddress: Address,
                         meta: String,
                         tokenAddress: Address): P[JsonRpcPlainResult]

  def sendBurnToken721Tx(id: BigInt, tokenAddress: Address): P[JsonRpcPlainResult]

  def sendBurnTokenTx(amount: String, tokenAddress: Address): P[JsonRpcPlainResult]

  // Derived operation that is usually included in a Logic trait (convention so far for Stockmind project)
  // For the sake of conciseness I think we should change that standard, and include logic traits functions as
  // derived operations of Ops traits. That way we avoid having to write Syntax sugar boilerplate and use it
  // with implicits. Less code to write, keep things related together.
  // In the end any logic is just a composition of primitive, abstract operations. Good type classes have as few
  // abstract methods as possible, and as much derived operations as possible.
  def createTokenLogic(tokenRequest: PostTokenRequest, userId: UUID)(
      implicit ev: Monad[P]): P[ValidatedTokenCreationTxHash] = {

    import cats.syntax.either._
    import cats.syntax.functor._
    val erc_type = "ERC-20"
    def checkTokenByAttributeExistence(
        searchResult: P[Option[Ethtoken]],
        errorIfAbsent: TokenCreationError
    ): EitherT[P, TokenCreationError, Unit] =
      EitherT(
        searchResult.map(
          maybeToken =>
            if (maybeToken.isDefined) errorIfAbsent.asLeft[Unit] else ().asRight[TokenCreationError]
        )
      )

    (for {
      ownerAddress <- EitherT(
        findEthereumAccountAddressOf(userId)
          .map(
            Either.fromOption[TokenCreationError, Address](_, noEthAccountFoundForUser)
          )
      )
      PostTokenRequest(name, symbol, initialAmount, decimals) = tokenRequest
      _ <- checkTokenByAttributeExistence(
        findEthereumTokenBySymbolAndType(symbol + "|" + erc_type),
        tokenWithSymbolAlreadyExists(symbol)
      )
      _ <- checkTokenByAttributeExistence(
        findEthereumTokenByNameAndSymbol(name, erc_type),
        tokenWithNameAlreadyExists(name)
      )
      txHash <- EitherT(
        sendContractCreationTx(name, symbol, initialAmount, decimals, ownerAddress)
      ).leftMap(ethereumError)
    } yield txHash).value
  }

  def create721TokenLogic(tokenRequest: PostErc721Request, userId: UUID)(
      implicit ev: Monad[P]): P[ValidatedTokenCreationTxHash] = {

    import cats.syntax.either._
    import cats.syntax.functor._

    val erc_type = "ERC-721"
    def checkTokenByAttributeExistence(
        searchResult: P[Option[Ethtoken]],
        errorIfAbsent: TokenCreationError
    ): EitherT[P, TokenCreationError, Unit] =
      EitherT(
        searchResult.map(
          maybeToken =>
            if (maybeToken.isDefined) errorIfAbsent.asLeft[Unit] else ().asRight[TokenCreationError]
        )
      )

    (for {
      ownerAddress <- EitherT(
        findEthereumAccountAddressOf(userId)
          .map(
            Either.fromOption[TokenCreationError, Address](_, noEthAccountFoundForUser)
          )
      )
      PostErc721Request(name, symbol) = tokenRequest
      _ <- checkTokenByAttributeExistence(
        findEthereumTokenBySymbolAndType(symbol + "|" + erc_type),
        tokenWithSymbolAlreadyExists(symbol)
      )
      _ <- checkTokenByAttributeExistence(
        findEthereumTokenByNameAndSymbol(name, erc_type),
        tokenWithNameAlreadyExists(name)
      )
      txHash <- EitherT(
        sendContract721CreationTx(name, symbol, ownerAddress)
      ).leftMap(ethereumError)
    } yield txHash).value
  }

  def mintTokenLogic(tokenRequest: PostMintBurnRequest, userId: UUID)(
      implicit ev: Monad[P]): P[ValidatedTokenMintBurnTxHash] = {
    import cats.syntax.either._
    import cats.syntax.functor._

    def checkTokenByAttributeMintBurn(
        searchResult: P[Option[Ethtoken]],
        errorIfAbsent: TokenMintBurnError
    ): EitherT[P, TokenMintBurnError, Ethtoken] =
      EitherT(
        searchResult.map(
          maybeToken =>
            if (maybeToken.isDefined)
              maybeToken.get.asRight[TokenMintBurnError]
            else errorIfAbsent.asLeft[Ethtoken]
        )
      )

    (for {
      ownerAddress <- EitherT(
        findEthereumAccountAddressOf(userId)
          .map(
            Either.fromOption[TokenMintBurnError, Address](_, noEthAccountFoundForUserM)
          )
      )
      PostMintBurnRequest(erc_type, symbol, amount) = tokenRequest
      token <- checkTokenByAttributeMintBurn(
        findEthereumTokenBySymbolandTypeandOwnerAddress(symbol, erc_type, ownerAddress),
        tokenWithSymbolNotExists(symbol)
      )
      txHash <- EitherT(
        sendMintTokenTx(amount, token.contract)
      ).leftMap(ethereumErrorMintBurn)
    } yield txHash).value

  }

  def mintToken721Logic(tokenRequest: PostMint721Request, userId: UUID)(
      implicit ev: Monad[P]): P[ValidatedTokenMintBurnTxHash] = {
    import cats.syntax.either._
    import cats.syntax.functor._

    val erc_type = "ERC-721"
    def checkTokenByAttributeMintBurn(
        searchResult: P[Option[Ethtoken]],
        errorIfAbsent: TokenMintBurnError
    ): EitherT[P, TokenMintBurnError, Ethtoken] =
      EitherT(
        searchResult.map(
          maybeToken =>
            if (maybeToken.isDefined)
              maybeToken.get.asRight[TokenMintBurnError]
            else errorIfAbsent.asLeft[Ethtoken]
        )
      )

    (for {
      ownerAddress <- EitherT(
        findEthereumAccountAddressOf(userId)
          .map(
            Either.fromOption[TokenMintBurnError, Address](_, noEthAccountFoundForUserM)
          )
      )
      PostMint721Request(symbol, meta) = tokenRequest
      token <- checkTokenByAttributeMintBurn(
        findEthereumTokenBySymbolandTypeandOwnerAddress(symbol, erc_type, ownerAddress),
        tokenWithSymbolNotExists(symbol)
      )
      txHash <- EitherT(
        sendMintToken721Tx(ownerAddress, meta, token.contract)
      ).leftMap(ethereumErrorMintBurn)
    } yield txHash).value

  }

  def burnTokenLogic(tokenRequest: PostMintBurnRequest, userId: UUID)(
      implicit ev: Monad[P]): P[ValidatedTokenMintBurnTxHash] = {
    import cats.syntax.either._
    import cats.syntax.functor._

    def checkTokenByAttributeMintBurn(
        searchResult: P[Option[Ethtoken]],
        errorIfAbsent: TokenMintBurnError
    ): EitherT[P, TokenMintBurnError, Ethtoken] =
      EitherT(
        searchResult.map(
          maybeToken =>
            if (maybeToken.isDefined)
              maybeToken.get.asRight[TokenMintBurnError]
            else errorIfAbsent.asLeft[Ethtoken]
        )
      )

    (for {
      ownerAddress <- EitherT(
        findEthereumAccountAddressOf(userId)
          .map(
            Either.fromOption[TokenMintBurnError, Address](_, noEthAccountFoundForUserM)
          )
      )
      PostMintBurnRequest(erc_type, symbol, amount) = tokenRequest
      token <- checkTokenByAttributeMintBurn(
        findEthereumTokenBySymbolandTypeandOwnerAddress(symbol, erc_type, ownerAddress),
        tokenWithSymbolNotExists(symbol)
      )
      txHash <- EitherT(
        sendBurnTokenTx(amount, token.contract)
      ).leftMap(ethereumErrorMintBurn)
    } yield txHash).value

  }

  def burnToken721Logic(tokenRequest: PostBurn721Request, userId: UUID)(
      implicit ev: Monad[P]): P[ValidatedTokenMintBurnTxHash] = {
    import cats.syntax.either._
    import cats.syntax.functor._

    def checkTokenByAttributeMintBurn(
        searchResult: P[Option[Ethtoken]],
        errorIfAbsent: TokenMintBurnError
    ): EitherT[P, TokenMintBurnError, Ethtoken] =
      EitherT(
        searchResult.map(
          maybeToken =>
            if (maybeToken.isDefined)
              maybeToken.get.asRight[TokenMintBurnError]
            else errorIfAbsent.asLeft[Ethtoken]
        )
      )

    (for {
      ownerAddress <- EitherT(
        findEthereumAccountAddressOf(userId)
          .map(
            Either.fromOption[TokenMintBurnError, Address](_, noEthAccountFoundForUserM)
          )
      )
      PostBurn721Request(id) = tokenRequest
      token <- checkTokenByAttributeMintBurn(
        findEthereum721TokenByUniqueId(id, ownerAddress),
        tokenWithIdNotExists(id)
      )
      txHash <- EitherT(
        sendBurnToken721Tx(id, token.contract)
      ).leftMap(ethereumErrorMintBurn)
    } yield txHash).value

  }
}
