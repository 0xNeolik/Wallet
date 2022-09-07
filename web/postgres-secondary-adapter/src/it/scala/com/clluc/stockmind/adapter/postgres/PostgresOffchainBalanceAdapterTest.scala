package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.adapter.postgres.Generators._
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.user.Balance
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

class PostgresOffchainBalanceAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  val erc20TokenDao = new PostgresErc20InfoAdapter(testTransactor)(executionContext)
  val offchainTransferDao = new PostgresOffChainTransferAdapter(testTransactor)
  val offchainBalanceDao = new PostgresOffchainBalanceAdapter(testTransactor)(executionContext)

  override def afterAll() = TableCleaner.clean()

  behavior of "PostgresOffchainBalanceAdapter"

  it should "not find balances of addresses with no transfers" in {
    val address = Address(genAddressString.sample.get)
    for {
      balances <- offchainBalanceDao.findBalancesForAddress(address)
    } yield balances shouldBe empty
  }

  it should "find balances of an address with some transfers" in {
    val token1 = genErc20Token.sample.get
    val token2 = genErc20Token.sample.get
    val t1 = genOffchainTransfer(token1.symbol).sample.get
    val t2 = genOffchainTransfer(token2.symbol).sample.get.copy(to = t1.to)
    for {
      _ <- erc20TokenDao.createEthereumToken(token1)
      _ <- erc20TokenDao.createEthereumToken(token2)
      _ <- offchainTransferDao.create(t1)
      _ <- offchainTransferDao.create(t2)
      balances <- offchainBalanceDao.findBalancesForAddress(t1.to)
    } yield balances should contain theSameElementsAs List(
      Balance(t1.to, token1, 0, t1.amount.value, 0, t1.amount.value, t1.amount.value),
      Balance(t2.to, token2, 0, t2.amount.value, 0, t2.amount.value, t2.amount.value)
    )
  }

  it should "not find balances of a non-existent address-token pair " in {
    val token1 = genErc20Token.sample.get
    val token2 = genErc20Token.sample.get
    val t1 = genOffchainTransfer(token1.symbol).sample.get
    val t2 = genOffchainTransfer(token2.symbol).sample.get.copy(to = t1.to)
    for {
      _ <- erc20TokenDao.createEthereumToken(token1)
      _ <- erc20TokenDao.createEthereumToken(token2)
      _ <- offchainTransferDao.create(t1)
      _ <- offchainTransferDao.create(t2)
      balances <- offchainBalanceDao.findBalanceByAddressAndEthereumToken(t1.to, "FAIL")
    } yield balances shouldBe empty
  }

  it should "find balances of an address-token pair" in {
    val token1 = genErc20Token.sample.get
    val token2 = genErc20Token.sample.get
    val t1 = genOffchainTransfer(token1.symbol).sample.get
    val t2 = genOffchainTransfer(token2.symbol).sample.get.copy(to = t1.to)
    for {
      _ <- erc20TokenDao.createEthereumToken(token1)
      _ <- erc20TokenDao.createEthereumToken(token2)
      _ <- offchainTransferDao.create(t1)
      _ <- offchainTransferDao.create(t2)
      balances <- offchainBalanceDao.findBalanceByAddressAndEthereumToken(t1.to, token1.symbol)
    } yield balances shouldBe Some(
      Balance(t1.to, token1, 0, t1.amount.value, 0, t1.amount.value, t1.amount.value)
    )
  }

}
