package com.clluc.stockmind.adapter.postgres

import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Ignore, Matchers}

// TODO Implement this IT test using the new created ports
@Ignore
class BalanceDaoTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  //  val erc20BalanceDao = new Erc20BalanceDaoImpl
  //  val erc20TokenDao = new Erc20TokenDaoImpl
  //  val erc20TransferEventDao = new Erc20TransferEventDaoImpl
  //  val pendingTransferDao = new PendingTransferDaoImpl
  //  val ethereumAccountDao = new EthereumAccountDaoImpl
  //  val userDao = new UserDaoImpl

  override def afterAll() = TableCleaner.clean()

  behavior of "ERC20 Balance DAO (Postgres)"

  it should "find a balance (by address and token)" in {
    pending
    //    val token = genErc20Token.sample.get
    //    val transfer = genErc20Transfer(token).sample.get
    //    val expectedBalance = Balance(transfer.to, token.symbol, 0, transfer.value.value, 0, transfer.value.value, transfer.value.value)
    //    for {
    //      savedToken <- erc20TokenDao.create(token)
    //      transferEvent <- erc20TransferEventDao.create(transfer)
    //      found <- erc20BalanceDao.find(transfer.to, token.symbol)
    //    } yield found shouldBe Some(expectedBalance)
  }

  it should "find all balances (by address)" in {
    pending
    //    val firstToken = genErc20Token.sample.get
    //    val secondToken = genErc20Token.sample.get
    //    val firstTransfer = genErc20Transfer(firstToken).sample.get
    //    val secondTransfer = firstTransfer.copy(tokenSymbol = secondToken.symbol)
    //    for {
    //      savedFirstToken <- erc20TokenDao.create(firstToken)
    //      savedSecondToken <- erc20TokenDao.create(secondToken)
    //      firstTransferEvent <- erc20TransferEventDao.create(firstTransfer)
    //      secondTransferEvent <- erc20TransferEventDao.create(secondTransfer)
    //      found <- erc20BalanceDao.findAll(firstTransfer.to)
    //    } yield found.length shouldBe 2
  }

  it should "not find a balance with an unused token" in {
    pending
    //    val unusedToken = genErc20Token.sample.get
    //    val accountAddress = Address(genAddress.sample.get)
    //    for {
    //      savedToken <- erc20TokenDao.create(unusedToken)
    //      found <- erc20BalanceDao.find(accountAddress, unusedToken.symbol)
    //    } yield found shouldBe None
  }

  it should "not find balances for an address without transfers" in {
    pending
    //    val accountAddress = Address(genAddress.sample.get)
    //    for {
    //      found <- erc20BalanceDao.findAll(accountAddress)
    //    } yield found.length shouldBe 0
  }

  it should "update the balance after a positive transfer" in {
    pending
    //    val token = genErc20Token.sample.get
    //    val transfer = genErc20Transfer(token).sample.get
    //    val expectedBalance = Balance(transfer.to, token.symbol, 0, transfer.value.value*2, 0, transfer.value.value * 2, transfer.value.value * 2)
    //    for {
    //      savedToken <- erc20TokenDao.create(token)
    //      firstTransferEvent <- erc20TransferEventDao.create(transfer)
    //      secondTransferEvent <- erc20TransferEventDao.create(transfer)
    //      found <- erc20BalanceDao.find(transfer.to, token.symbol)
    //    } yield found shouldBe Some(expectedBalance)
  }

  it should "update the balance after a negative transfer" in {
    pending
    //    val token = genErc20Token.sample.get
    //    val firstTransfer = genErc20Transfer(token).sample.get
    //    val secondTransfer = firstTransfer.copy(from = firstTransfer.to, to = firstTransfer.from)
    //    val expectedBalance = Balance(firstTransfer.to, token.symbol, firstTransfer.value.value, firstTransfer.value.value, 0, 0, 0)
    //    for {
    //      savedToken <- erc20TokenDao.create(token)
    //      firstTransferEvent <- erc20TransferEventDao.create(firstTransfer)
    //      secondTransferEvent <- erc20TransferEventDao.create(secondTransfer)
    //      found <- erc20BalanceDao.find(firstTransfer.to, token.symbol)
    //    } yield found shouldBe Some(expectedBalance)
  }

  it should "correctly reflect withheld amounts due to unprocessed pending transfers" in {
    pending
    //    val user = genUser.sample.get
    //    val ethAccount = genEthereumAccount(user.userID).sample.get
    //    val token = genErc20Token.sample.get
    //    val transfer = genErc20Transfer(token).sample.get.copy(to = ethAccount.address, value = Uint(256, 100))
    //    val pendingTransfer = genPendingTransfer(user.userID, token.symbol).sample.get.copy(amount = 10)
    //
    //    val expectedBalance = Balance(ethAccount.address, token.symbol, 0, 100, 10, 100, 90)
    //    for {
    //      _ <- userDao.save(user)
    //      _ <- ethereumAccountDao.save(ethAccount)
    //      _ <- erc20TokenDao.create(token)
    //      _ <- erc20TransferEventDao.create(transfer)
    //      _ <- pendingTransferDao.create(pendingTransfer)
    //      balance <- erc20BalanceDao.find(ethAccount.address, token.symbol)
    //    } yield balance shouldBe Some(expectedBalance)
  }
}
