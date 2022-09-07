package com.clluc.stockmind.controller

import java.util.UUID

import javax.inject.Inject
import com.clluc.stockmind.core.ethereum.{Ethtoken}
import com.clluc.stockmind.core.token.AllTokensInfo
import com.clluc.stockmind.port.primary.TokensPort
import TokensPort._
import play.api.mvc.{BaseController, ControllerComponents, Result}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import play.api.cache.SyncCacheApi

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._

class TokensController @Inject()(
    val controllerComponents: ControllerComponents,
    tokensPort: TokensPort,
    socAuthcontroller: SocialAuthController,
    cache: SyncCacheApi,
)(implicit executionContext: ExecutionContext)
    extends BaseController {

  import TokensController._

  def supportedTokens() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    tokensPort.allTokensInfo().map { allTokens =>
      Ok(toTokensView(allTokens).asJson.noSpaces).as(JSON)
    }

  }

  //POST /v1/tokens/20
  def createErc20Token() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    val tokenRequest: Either[TokenControllerError, PostTokenRequest] = for {
      requestContent <- Either.fromOption(request.body.asJson, noJsonInRequest())
      requestJson = requestContent.toString()
      tokenRequest <- decode[PostTokenRequest](requestJson).leftMap(cannotParseJson)
    } yield tokenRequest

    val response: Future[Either[TokenControllerError, Result]] = {

      val eitherTResult = for {
        requestContent <- EitherT.fromEither[Future](tokenRequest)
        txHash <- EitherT(tokensPort.createToken(requestContent, userID))
          .leftMap(cannotCreateTokenError)
      } yield {
        Ok(TxHashView(txHash).asJson.noSpaces).as(JSON)
      }

      eitherTResult.value
    }

    def calculateResult(response: Future[Either[TokenControllerError, Result]]): Future[Result] = {
      response.map { resp =>
        resp.getOrElse(fromErrorToResult(resp.left.get))
      }
    }

    calculateResult(response)

  }

  //POST /v1/tokens/721
  def createErc721Token() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    val tokenRequest: Either[TokenControllerError, PostErc721Request] = for {
      requestContent <- Either.fromOption(request.body.asJson, noJsonInRequest())
      requestJson = requestContent.toString()
      tokenRequest <- decode[PostErc721Request](requestJson).leftMap(cannotParseJson)
    } yield tokenRequest

    val response: Future[Either[TokenControllerError, Result]] = {

      val eitherTResult = for {
        requestContent <- EitherT.fromEither[Future](tokenRequest)
        txHash <- EitherT(tokensPort.create721Token(requestContent, userID))
          .leftMap(cannotCreateTokenError)
      } yield {
        Ok(TxHashView(txHash).asJson.noSpaces).as(JSON)
      }

      eitherTResult.value
    }

    def calculateResult(response: Future[Either[TokenControllerError, Result]]): Future[Result] = {
      response.map { resp =>
        resp.getOrElse(fromErrorToResult(resp.left.get))
      }
    }

    calculateResult(response)

  }

  //POST /v1/tokens/mint/20
  def mintERC20Token() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    val tokenRequest: Either[TokenControllerError, PostMintBurnRequest] = for {
      requestContent <- Either.fromOption(request.body.asJson, noJsonInRequest())
      requestJson = requestContent.toString()
      tokenRequest <- decode[PostMintBurnRequest](requestJson).leftMap(cannotParseJson)
    } yield tokenRequest

    val response: Future[Either[TokenControllerError, Result]] = {

      val eitherTResult = for {
        requestContent <- EitherT.fromEither[Future](tokenRequest)
        txHash <- EitherT(tokensPort.mintToken(requestContent, userID))
          .leftMap(cannotMintBurnTokenError)
      } yield {
        Ok(TxHashView(txHash).asJson.noSpaces).as(JSON)
      }

      eitherTResult.value
    }
    def calculateResult(response: Future[Either[TokenControllerError, Result]]): Future[Result] = {
      response.map { resp =>
        resp.getOrElse(fromErrorToResult(resp.left.get))
      }
    }

    calculateResult(response)

  }

  //POST /v1/tokens/mint/721
  def mintERC721Token() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    val tokenRequest: Either[TokenControllerError, PostMint721Request] = for {
      requestContent <- Either.fromOption(request.body.asJson, noJsonInRequest())
      requestJson = requestContent.toString()
      tokenRequest <- decode[PostMint721Request](requestJson).leftMap(cannotParseJson)
    } yield tokenRequest

    val response: Future[Either[TokenControllerError, Result]] = {

      val eitherTResult = for {
        requestContent <- EitherT.fromEither[Future](tokenRequest)
        txHash <- EitherT(tokensPort.mint721Token(requestContent, userID))
          .leftMap(cannotMintBurnTokenError)
      } yield {
        Ok(TxHashView(txHash).asJson.noSpaces).as(JSON)
      }

      eitherTResult.value
    }
    def calculateResult(response: Future[Either[TokenControllerError, Result]]): Future[Result] = {
      response.map { resp =>
        resp.getOrElse(fromErrorToResult(resp.left.get))
      }
    }

    calculateResult(response)

  }

  //POST /v1/tokens/burn/20
  def burnERC20Token() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    val tokenRequest: Either[TokenControllerError, PostMintBurnRequest] = for {
      requestContent <- Either.fromOption(request.body.asJson, noJsonInRequest())
      requestJson = requestContent.toString()
      tokenRequest <- decode[PostMintBurnRequest](requestJson).leftMap(cannotParseJson)
    } yield tokenRequest

    val response: Future[Either[TokenControllerError, Result]] = {

      val eitherTResult = for {
        requestContent <- EitherT.fromEither[Future](tokenRequest)
        txHash <- EitherT(tokensPort.burnToken(requestContent, userID))
          .leftMap(cannotMintBurnTokenError)
      } yield {
        Ok(TxHashView(txHash).asJson.noSpaces).as(JSON)
      }

      eitherTResult.value
    }
    def calculateResult(response: Future[Either[TokenControllerError, Result]]): Future[Result] = {
      response.map { resp =>
        resp.getOrElse(fromErrorToResult(resp.left.get))
      }
    }

    calculateResult(response)

  }

  //POST /v1/tokens/burn/721
  def burnERC721Token() = socAuthcontroller.AuthenticatedAction { idToken => implicit request =>
    val userID = cache.get[UUID](idToken + "id").get
    val tokenRequest: Either[TokenControllerError, PostBurn721Request] = for {
      requestContent <- Either.fromOption(request.body.asJson, noJsonInRequest())
      requestJson = requestContent.toString()
      tokenRequest <- decode[PostBurn721Request](requestJson).leftMap(cannotParseJson)
    } yield tokenRequest

    val response: Future[Either[TokenControllerError, Result]] = {

      val eitherTResult = for {
        requestContent <- EitherT.fromEither[Future](tokenRequest)
        txHash <- EitherT(tokensPort.burn721Token(requestContent, userID))
          .leftMap(cannotMintBurnTokenError)
      } yield {
        Ok(TxHashView(txHash).asJson.noSpaces).as(JSON)
      }

      eitherTResult.value
    }
    def calculateResult(response: Future[Either[TokenControllerError, Result]]): Future[Result] = {
      response.map { resp =>
        resp.getOrElse(fromErrorToResult(resp.left.get))
      }
    }

    calculateResult(response)

  }

}

private object TokensController extends ErrorToResultConversions {
  case class TokensView(
      erc20_tokens: List[TokenView],
      erc721_tokens: List[TokenView],
  )

  case class TokenView(
      symbol: String,
      name: String
  )

  def toTokensView(alltokensInfo: AllTokensInfo): TokensView = {
    def toTokenView(token: Ethtoken): TokenView = TokenView(token.symbol, token.name)

    TokensView(
      erc20_tokens = alltokensInfo.erc20Tokens.map(toTokenView),
      erc721_tokens = alltokensInfo.erc721Tokens.map(toTokenView)
    )
  }

  case class TxHashView(txHash: String)

  sealed trait TokenControllerError {
    def message(): String
  }
  case object NoJsonInRequest extends TokenControllerError {
    override def message(): String =
      "A JSON payload was expected as body in this request"
  }
  case class CannotParseJson(error: io.circe.Error) extends TokenControllerError {
    override def message(): String = s"Cannot parse JSON body payload ($error)"
  }
  case class CannotCreateTokenError(error: TokenCreationError) extends TokenControllerError {
    override def message(): String = {
      error match {
        case NoEthAccountFoundForUser =>
          "Current logged in user does not have an ethereum account in the system. Contact support team (this is a bug)"
        case EthereumError(err) =>
          s"Opps; issues communicating with the ethereum network: $err"
        case TokenWithSymbolAlreadyExists(symbol) =>
          s"The token with symbol $symbol already exists; cannot be created twice"
        case TokenWithNameAlreadyExists(name) =>
          s"The token with name $name already exists; using a different name is needed"
      }
    }
  }
  case class CannotMintBurnTokenError(error: TokenMintBurnError) extends TokenControllerError {
    override def message(): String = {
      error match {
        case NoEthAccountFoundForUserM =>
          "Current logged in user does not have an ethereum account in the system. Contact support team (this is a bug)"
        case EthereumErrorMintBurn(err) =>
          s"Opps; issues communicating with the ethereum network: $err"
        case TokenWithSymbolNotExists(name) =>
          s"The token with symbol $name doesn't exists; using a different symbol is needed"
        case TokenWithIdNotExists(id) =>
          s"The token with ID $id doesn't exists; using a different ID is needed"

      }
    }
  }

  case class CannotFindType(error: TokenTypeError) extends TokenControllerError {
    override def message(): String = {
      error match {
        case NoErcTypeFound(erc_type) =>
          s"The token with type $erc_type doesn't exists"
      }
    }
  }

  def noJsonInRequest(): TokenControllerError                      = NoJsonInRequest
  def cannotParseJson(error: io.circe.Error): TokenControllerError = CannotParseJson(error)

  def cannotCreateTokenError(error: TokenCreationError): TokenControllerError =
    CannotCreateTokenError(error)

  def cannotMintBurnTokenError(error: TokenMintBurnError): TokenControllerError =
    CannotMintBurnTokenError(error)

  def cannotFindTypeError(error: TokenTypeError): TokenControllerError =
    CannotFindType(error)

  def fromErrorToResult(error: TokenControllerError): Result = {
    val resultConstructor = error match {
      case NoJsonInRequest    => BadRequest
      case CannotParseJson(_) => BadRequest
      case CannotFindType(_)  => BadRequest
      case CannotCreateTokenError(err) =>
        err match {
          case NoEthAccountFoundForUser        => Conflict
          case EthereumError(_)                => InternalServerError
          case TokenWithSymbolAlreadyExists(_) => Conflict
          case TokenWithNameAlreadyExists(_)   => Conflict
        }
      case CannotMintBurnTokenError(err) =>
        err match {
          case NoEthAccountFoundForUserM   => Conflict
          case EthereumErrorMintBurn(_)    => InternalServerError
          case TokenWithSymbolNotExists(_) => Conflict
          case TokenWithIdNotExists(_)     => Conflict

        }
    }

    resultConstructor(error.message())
  }
}
