package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.adapter.postgres.Generators._
import com.clluc.stockmind.core.ethereum.Erc20Token
import com.clluc.stockmind.core.ethereum.solidity.Address
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresErc20InfoAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  override def afterAll() = TableCleaner.clean()

  val pgErc20Info = new PostgresErc20InfoAdapter(testTransactor)

  behavior of "PostgresErc20InfoAdapterTest - tokens"

  it should "create new tokens" in {
    val token = genErc20Token.sample.get
    for {
      createdToken <- pgErc20Info.createEthereumToken(token)
    } yield createdToken shouldBe token
  }

  it should "not create two tokens with the same symbol" in {
    val originalToken = genErc20Token.sample.get
    val duplicatedToken = genErc20Token.sample.get.copy(symbol = originalToken.symbol)
    pgErc20Info.createEthereumToken(originalToken).flatMap { _ =>
      recoverToSucceededIf[PSQLException](pgErc20Info.createEthereumToken(duplicatedToken))
    }
  }

  it should "not find a token that is not stored (by symbol)" in {
    for {
      found <- pgErc20Info.findEthereumTokenBySymbol("NOTSAVED")
    } yield found shouldBe None
  }

  private def testFindToken[T](byParFx: Erc20Token => T)(findFx: T => Future[Option[Erc20Token]]) = {
    val token = genErc20Token.sample.get
    for {
      _ <- pgErc20Info.createEthereumToken(token)
      found <- findFx(byParFx(token))
    } yield found shouldBe Some(token)
  }

  it should "find a token (by symbol)" in {
    testFindToken(_.symbol)(pgErc20Info.findEthereumTokenBySymbol)
  }

  it should "find a token (by name)" in {
    testFindToken(_.name)(pgErc20Info.findEthereumTokenByName)
  }

  it should "not find a token that is not stored (by address)" in {
    for {
      found <- pgErc20Info.findEthereumTokenByAddress(Address.default)
    } yield found shouldBe None
  }

  it should "find a token (by address)" in {
    testFindToken(_.contract)(pgErc20Info.findEthereumTokenByAddress)
  }

  it should "find all tokens" in {
    val tokens = 1.to(4).map(_ => genErc20Token.sample.get).toList
    for {
      _ <- TableCleaner.deleteErc20Tokens
      _ <- Future.sequence(tokens.map(pgErc20Info.createEthereumToken))
      foundTokens <- pgErc20Info.findAllEthereumTokens()
    } yield tokens.sortBy(_.contract.value) shouldBe foundTokens.sortBy(_.contract.value)
  }
}
