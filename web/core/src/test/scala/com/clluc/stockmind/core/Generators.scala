package com.clluc.stockmind.core

import java.util.UUID

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction.{Counterparty, IncomingTx, OutgoingTx, TokenAmount}
import org.joda.time.DateTime
import org.scalacheck.Gen

private[core] object Generators {

  def genHexString(length: Int): Gen[String] =
    for {
      low       <- Gen.choose[Char]('a', 'f')
      number    <- Gen.choose[Char]('0', '9')
      hexString <- Gen.listOfN(length, Gen.oneOf(low, number)).map(_.mkString)
    } yield hexString

  def genAddress: Gen[String] = {
    genHexString(40)
  }

  def genEthHash: Gen[String] = genHexString(64)

  def genLoggedEvent: Gen[LoggedEvent] = {
    def genEncodedData: Gen[String] =
      for {
        data <- genHexString(64)
      } yield s"0x$data"

    for {
      address   <- genAddress
      block     <- Gen.choose(1, 2000000)
      topics    <- Gen.listOfN(3, genEncodedData)
      data      <- genEncodedData
      txHash    <- genEthHash
      txIndex   <- Gen.choose(0, 100)
      blockHash <- genEthHash
    } yield
      LoggedEvent(
        Address(address),
        Block(block),
        topics,
        data,
        EthereumHash(txHash),
        txIndex,
        EthereumHash(blockHash)
      )
  }

  def genEthereumAccount(userID: UUID): Gen[EthereumAccount] =
    for {
      address  <- genAddress
      password <- Gen.alphaNumStr
    } yield EthereumAccount(userID, Address(address), password)

  def genStringNumber(length: Int): Gen[String] =
    for {
      stringNumber <- Gen.listOfN(length, Gen.choose[Char]('0', '9')).map(_.mkString)
    } yield stringNumber

  def genNumber: Gen[Int] =
    for {
      number <- Gen.choose(1, Int.MaxValue)
    } yield number

  def genBigintNumber: Gen[BigInt] =
    for {
      number <- Gen.choose(1, 18)
    } yield number

  def genDecimals: Gen[Int] =
    for {
      number <- Gen.choose(0, 18)
    } yield number

  def genThreeDigitsInt: Gen[Int] =
    for {
      number <- Gen.choose(100, 999)
    } yield number

  def genEthereumToken: Gen[Erc20Token] =
    for {
      symbol   <- Gen.listOfN(3, Gen.choose[Char]('a', 'z')).map(_.mkString)
      erc_type <- Gen.listOfN(6, Gen.choose[Char]('a', 'z')).map(_.mkString)
      name     <- Gen.listOfN(8, Gen.choose[Char]('a', 'z')).map(_.mkString)
      decimals <- Gen.choose(1, 18)
      address  <- genAddress
    } yield Erc20Token(symbol, erc_type, name, decimals, Address(address), None, None)

  def genEthToken: Gen[Ethtoken] =
    for {
      symbol   <- Gen.listOfN(3, Gen.choose[Char]('a', 'z')).map(_.mkString)
      erc_type <- Gen.listOfN(6, Gen.choose[Char]('a', 'z')).map(_.mkString)
      name     <- Gen.listOfN(8, Gen.choose[Char]('a', 'z')).map(_.mkString)
      address  <- genAddress
    } yield Ethtoken(symbol, erc_type, name, Address(address), None, None)

  def genEthereumToken721: Gen[Erc721Token] =
    for {
      symbol   <- Gen.listOfN(3, Gen.choose[Char]('a', 'z')).map(_.mkString)
      erc_type <- Gen.listOfN(6, Gen.choose[Char]('a', 'z')).map(_.mkString)
      name     <- Gen.listOfN(8, Gen.choose[Char]('a', 'z')).map(_.mkString)
      meta     <- Gen.listOfN(8, Gen.choose[Char]('a', 'z')).map(_.mkString)
      id       <- genBigintNumber
      name     <- Gen.listOfN(8, Gen.choose[Char]('a', 'z')).map(_.mkString)
      address  <- genAddress
    } yield Erc721Token(symbol, erc_type, name, meta, id, Address(address), None, None)

  def genTransactionCounterparty: Gen[Counterparty] =
    for {
      ethAdd <- Gen.option(genAddress)
    } yield
      Counterparty(
        ethAdd,
        None
      )

  def genTokenAmount: Gen[TokenAmount] =
    for {
      whole   <- Gen.numStr
      decimal <- Gen.numStr
    } yield TokenAmount(whole, decimal)

  def genTransaction: Gen[com.clluc.stockmind.core.transaction.StockmindTransaction] =
    for {
      id               <- Gen.choose(Long.MinValue, Long.MaxValue)
      direction        <- Gen.oneOf(Seq(IncomingTx, OutgoingTx))
      pending          <- Gen.oneOf(Seq(true, false))
      counterparty     <- genTransactionCounterparty
      token            <- Gen.alphaChar
      erc_type         <- Gen.alphaChar
      tokenDescription <- Gen.alphaLowerStr
      decimals         <- Gen.choose(3, 18)
      amount           <- genTokenAmount
      date             <- Gen.const(DateTime.now())
    } yield
      com.clluc.stockmind.core.transaction.StockmindTransaction(
        id,
        direction,
        pending,
        counterparty,
        token.toString * 3,
        erc_type.toString * 6,
        tokenDescription,
        decimals,
        amount,
        None,
        date
      )

  def generateSample[T](generator: Gen[T]): T = generator.sample.get
}
