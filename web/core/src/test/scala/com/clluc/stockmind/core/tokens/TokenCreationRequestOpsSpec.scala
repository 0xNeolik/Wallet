package com.clluc.stockmind.core.tokens

import java.util.UUID

import com.clluc.stockmind.core.Generators
import com.clluc.stockmind.core.ethereum.{Erc20Token, Erc721Token, EthereumAccount, Ethtoken}
import com.clluc.stockmind.core.ethereum.solidity.Address
import org.scalatest.{FunSpec, Matchers}

import scala.util.Random
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.port.primary.TokensPort._
import java.util.UUID
import cats.data.State
import TokenCreationRequestOpsSpec._

class TokenCreationRequestOpsSpec extends FunSpec with Matchers {

  private def doTest(
      fixture: TokenCreationRequestOps[TestState],
      request: PostTokenRequest,
      expectedState: Option[SendContractCreationTxPars],
      expectedResult: ValidatedTokenCreationTxHash
  ) = {
    val (state, result) = fixture.createTokenLogic(request, userId).run(None).value

    state shouldBe expectedState
    result shouldBe expectedResult

  }

  /*private def doTest721(
      fixture: TokenCreationRequestOps[TestState],
      request721: PostErc721Request,
      expectedState721: Option[SendContractCreation721TxPars],
      expectedResult: ValidatedTokenCreationTxHash
  ) = {
    val (state721, result721) = fixture.create721TokenLogic(request721, userId).run(None).value

    state721 shouldBe expectedState721
    result721 shouldBe expectedResult
  }*/

  describe("When everything is ok erc20 (happy path)") {
    it("the create erc20token action give us the expected result") {
      doTest(
        happyPathFixture,
        happyPathPostTokenRequest,
        Some(expectedSendContractCreationTxPars),
        expectedEthResponse
      )
    }
  }

  /* describe("When everything is ok erc721 (happy path)") {
    it("the create erc721token action give us the expected result") {
      doTest721(
        happyPathFixture,
        happyPathPostT721okenRequest,
        Some(expectedSendContract721CreationTxPars),
        expectedEthResponse
      )
    }
  }*/

  describe("When something goes wrong") {
    it(
      "If the token creation requester has no ethereum account in the system we get a noEthAccountFoundForUser result") {
      doTest(
        userNoEthAccFixture,
        happyPathPostTokenRequest,
        None,
        Left(NoEthAccountFoundForUser)
      )
    }

    it("If the token to be created has a symbol already in database") {
      doTest(
        tokenSymbolExistsFixture,
        symbolExistsPostTokenRequest,
        None,
        Left(TokenWithSymbolAlreadyExists(existentTokenSymbol))
      )
    }

    it("If the token to be created has a name already in database") {
      doTest(
        tokenNameExistsFixture,
        nameExistsPostTokenRequest,
        None,
        Left(TokenWithNameAlreadyExists(existentTokenName))
      )
    }

    it("If the token to be created has a symbol and a name already in database") {
      doTest(
        tokenNameAndSymbolExistFixture,
        nameAndSymbolExistPostTokenRequest,
        None,
        Left(TokenWithSymbolAlreadyExists(existentTokenSymbol))
      )
    }
    /* it(
      "If the token 721 creation requester has no ethereum account in the system we get a noEthAccountFoundForUser result") {
      doTest721(
        userNoEthAccFixture,
        happyPathPostT721okenRequest,
        None,
        Left(NoEthAccountFoundForUser)
      )
    }

    it("If the token 721 to be created has a symbol already in database") {
      doTest721(
        token721SymbolExistsFixture,
        happyPathPostT721okenRequest,
        None,
        Left(TokenWithSymbolAlreadyExists(existentTokenSymbol721))
      )
    }

    it("If the token 721 to be created has a name already in database") {
      doTest721(
        tokenNameExistsFixture,
        happyPathPostT721okenRequest,
        None,
        Left(TokenWithNameAlreadyExists(existentTokenName))
      )
    }

    it("If the token 721 to be created has a symbol and a name already in database") {
      doTest721(
        tokenNameAndSymbolExistFixture,
        happyPathPostT721okenRequest,
        None,
        Left(TokenWithSymbolAlreadyExists(existentTokenSymbol))
      )
    }*/
  }
}

object TokenCreationRequestOpsSpec {
  val userId         = UUID.randomUUID()
  val userEthAdd     = Address(Generators.genAddress.sample.get)
  val userEthAccount = EthereumAccount(userId, userEthAdd, Random.nextString(10))

  val existentTokenSymbol   = "STK"
  val existentTokenType     = "ERC-20"
  val existentTokenName     = "Stocks"
  val tokenContractAddress  = Address.default
  private val tokenDecimals = 8

  val existentToken =
    Erc20Token(existentTokenSymbol,
               existentTokenType,
               existentTokenName,
               tokenDecimals,
               tokenContractAddress,
               Some(userId.toString),
               Some(10))

  val existentEthToken =
    Ethtoken(existentTokenSymbol,
             existentTokenType,
             existentTokenName,
             tokenContractAddress,
             Some(userId.toString),
             Some(10))

  val existentTokenSymbol721 = "STK"
  val existentTokenType721   = "ERC-721"
  val existentMetadata721    = "metadata"

  val existentId721 =
    "71120786848863412851373030999642871879808768922518165984257232620739138279176"

  val existentToken721 =
    Erc721Token(
      existentTokenSymbol721,
      existentTokenType721,
      existentTokenName,
      existentMetadata721,
      BigInt(existentId721),
      tokenContractAddress,
      Some(userId.toString),
      Some(10)
    )

  val nonExistentTokenSymbol           = "BLD"
  val nonExistentTokenName             = "Baladis"
  val initialAmountForNonExistentToken = "1000"

  val expectedEthResponse = Right("Ok")

  case class SendContractCreationTxPars(
      name: String,
      symbol: String,
      initialAmount: String,
      decimals: Int,
      ownerAddress: Address
  )

  case class SendContractCreation721TxPars(
      name: String,
      symbol: String,
      ownerAddress: Address
  )

  case class SendMint721TxPars(userAddress: Address, meta: String, tokenAddress: Address)

  case class SendBurn721TxPars(id: BigInt, tokenAddress: Address)

  case class SendContractMintBurnTxPars(
      symbol: String,
      amount: String,
      tokenAddress: Address
  )

  private def createPostTokenRequest(name: String, symbol: String): PostTokenRequest =
    PostTokenRequest(
      name,
      symbol,
      initialAmountForNonExistentToken,
      tokenDecimals
    )

  private def createPost721TokenRequest(name: String, symbol: String): PostErc721Request =
    PostErc721Request(
      name,
      symbol
    )

  val happyPathPostTokenRequest = createPostTokenRequest(
    nonExistentTokenName,
    nonExistentTokenSymbol
  )

  val happyPathPostT721okenRequest = createPost721TokenRequest(
    nonExistentTokenName,
    nonExistentTokenSymbol
  )

  val symbolExistsPostTokenRequest = createPostTokenRequest(
    nonExistentTokenName,
    existentTokenSymbol,
  )

  val nameExistsPostTokenRequest = createPostTokenRequest(
    existentTokenName,
    nonExistentTokenSymbol
  )

  val nameAndSymbolExistPostTokenRequest = createPostTokenRequest(
    existentTokenName,
    existentTokenSymbol
  )

  val expectedSendContractCreationTxPars = SendContractCreationTxPars(
    nonExistentTokenName,
    nonExistentTokenSymbol,
    initialAmountForNonExistentToken,
    tokenDecimals,
    userEthAdd
  )

  val expectedSendContract721CreationTxPars = SendContractCreation721TxPars(
    nonExistentTokenName,
    nonExistentTokenSymbol,
    userEthAdd
  )

  // Unfortunately we cannot use mocks to create fixtures for this test
  // We are testing a derived operation in a type class
  // Abstract methods and logic to be tested are part of the same trait
  // If we use a mock, we cannot access the behaviour we want to test
  // So we have to resort to mockless, functional testing using state transitions
  type TestState[A] = State[(Option[SendContractCreationTxPars]), A]

  trait BaseOps extends TokenCreationRequestOps[TestState] {

    def sendContractCreationTx(
        name: String,
        symbol: String,
        initialAmount: String,
        decimals: Int,
        ownerAddress: Address
    ): TestState[JsonRpcPlainResult] = State { _ =>
      (
        Some(SendContractCreationTxPars(name, symbol, initialAmount, decimals, ownerAddress)),
        expectedEthResponse
      )
    }

    def sendContract721CreationTx(name: String, symbol: String, ownerAddress: Address) = State {
      _ =>
        (
          Some(SendContractCreationTxPars(name, symbol, "1", 0, ownerAddress)),
          expectedEthResponse
        )
    }

    def sendMintTokenTx(
        amount: String,
        tokenAddress: Address,
    ): TestState[JsonRpcPlainResult] = State { _ =>
      (
        Some(SendContractCreationTxPars("", "", amount, 0, tokenAddress)),
        expectedEthResponse
      )
    }

    def sendBurnTokenTx(
        amount: String,
        tokenAddress: Address,
    ): TestState[JsonRpcPlainResult] = State { _ =>
      (
        Some(SendContractCreationTxPars("", "", amount, 0, tokenAddress)),
        expectedEthResponse
      )
    }
  }

  lazy val happyPathFixture = new BaseOps {
    override def findAccountByUserId(userId: UUID)                     = State { (_, Some(userEthAccount)) }
    override def findEthereumTokenBySymbolAndType(symbol_type: String) = State { (_, None) }
    override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                                 erc_type: String,
                                                                 ownerAddress: Address) =
      State { (_, None) }
    override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String) = State {
      (_, None)
    }
    override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) = ???
    override def sendMintToken721Tx(userAddress: Address, meta: String, tokenAddress: Address) =
      State {
        (
          _,
          expectedEthResponse
        )
      }
    override def sendBurnToken721Tx(id: BigInt, tokenAddress: Address) = State {
      (
        _,
        expectedEthResponse
      )
    }
  }

  lazy val userNoEthAccFixture = new BaseOps {
    override def findAccountByUserId(userId: UUID)                         = State { (_, None) }
    override def findEthereumTokenBySymbolAndType(symbol_erc_type: String) = ???
    override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                                 erc_type: String,
                                                                 ownerAddress: Address) =
      State { (_, None) }
    override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String)   = ???
    override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) = ???
    override def sendMintToken721Tx(userAddress: Address, meta: String, tokenAddress: Address) =
      State {
        (
          _,
          expectedEthResponse
        )
      }
    override def sendBurnToken721Tx(id: BigInt, tokenAddress: Address) = State {
      (
        _,
        expectedEthResponse
      )
    }
  }

  lazy val tokenSymbolExistsFixture = new BaseOps {
    override def findAccountByUserId(userId: UUID) = State { (_, Some(userEthAccount)) }
    override def findEthereumTokenBySymbolAndType(symbol_type: String) = State {
      (_, Some(existentEthToken))
    }
    override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                                 erc_type: String,
                                                                 ownerAddress: Address) =
      State { (_, Some(existentEthToken)) }
    override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String)   = ???
    override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) = ???
    override def sendMintToken721Tx(userAddress: Address, meta: String, tokenAddress: Address) =
      State {
        (
          _,
          expectedEthResponse
        )
      }
    override def sendBurnToken721Tx(id: BigInt, tokenAddress: Address) = State {
      (
        _,
        expectedEthResponse
      )
    }
  }

  lazy val token721SymbolExistsFixture = new BaseOps {
    override def findAccountByUserId(userId: UUID) = State { (_, Some(userEthAccount)) }
    override def findEthereumTokenBySymbolAndType(symbol_erc_type: String) = State {
      (_, Some(existentEthToken))
    }
    override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                                 erc_type: String,
                                                                 ownerAddress: Address) =
      State { (_, Some(existentEthToken)) }
    override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String)   = ???
    override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) = ???
    override def sendMintToken721Tx(userAddress: Address, meta: String, tokenAddress: Address) =
      State {
        (
          _,
          expectedEthResponse
        )
      }
    override def sendBurnToken721Tx(id: BigInt, tokenAddress: Address) = State {
      (
        _,
        expectedEthResponse
      )
    }
  }

  lazy val tokenNameExistsFixture = new BaseOps {
    override def findAccountByUserId(userId: UUID)                         = State { (_, Some(userEthAccount)) }
    override def findEthereumTokenBySymbolAndType(symbol_erc_type: String) = State { (_, None) }
    override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                                 erc_type: String,
                                                                 ownerAddress: Address) =
      State { (_, None) }
    override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String) = State {
      (_, Some(existentEthToken))
    }
    override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) = ???
    override def sendMintToken721Tx(userAddress: Address, meta: String, tokenAddress: Address) =
      State {
        (
          _,
          expectedEthResponse
        )
      }
    override def sendBurnToken721Tx(id: BigInt, tokenAddress: Address) = State {
      (
        _,
        expectedEthResponse
      )
    }
  }

  lazy val tokenNameAndSymbolExistFixture = new BaseOps {
    override def findAccountByUserId(userId: UUID) = State { (_, Some(userEthAccount)) }
    override def findEthereumTokenBySymbolAndType(symbol_erc_type: String) = State {
      (_, Some(existentEthToken))
    }
    override def findEthereumTokenBySymbolandTypeandOwnerAddress(symbol: String,
                                                                 erc_type: String,
                                                                 ownerAddress: Address) =
      State { (_, Some(existentEthToken)) }
    override def findEthereumTokenByNameAndSymbol(name: String, erc_type: String) = State {
      (_, Some(existentEthToken))
    }
    override def findEthereum721TokenByUniqueId(id: BigInt, contractOwner: Address) = ???
    override def sendMintToken721Tx(userAddress: Address, meta: String, tokenAddress: Address) =
      State {
        (
          _,
          expectedEthResponse
        )
      }
    override def sendBurnToken721Tx(id: BigInt, tokenAddress: Address) = State {
      (
        _,
        expectedEthResponse
      )
    }
  }
}
