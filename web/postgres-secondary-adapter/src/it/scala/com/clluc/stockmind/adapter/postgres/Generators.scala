package com.clluc.stockmind.adapter.postgres

import java.util.UUID

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.transaction.{OffChainTransfer, PendingTransfer}
import com.clluc.stockmind.core.twitter.TwitterAccount
import com.clluc.stockmind.core.user._
import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import io.circe.parser.parse
import org.joda.time.{DateTime, Period}
import org.scalacheck.Gen
import com.fortysevendeg.scalacheck.datetime.instances.joda._

private[postgres] object Generators {

  def genLoggedEvent: Gen[LoggedEvent] =
    for {
      address <- genAddress
      block <- Gen.choose(1, 2000000)
      topics <- Gen.listOfN(3, genEncodedData)
      data <- genEncodedData
      txHash <- genEthHash
      txIndex <- Gen.choose(0, 100)
    } yield LoggedEvent(
      Address(address), Block(block), topics, data, EthereumHash(txHash), txIndex, EthereumHash(txHash)
    )

  def genConfigKey: Gen[(String, String)] =
    for {
      key <- Gen.alphaStr.retryUntil(_.nonEmpty)
      value <- Gen.alphaStr
    } yield key -> value

  def genUser: Gen[User] = {
    for {
      id <- Gen.uuid
      providerId <- Gen.alphaStr
      providerKey <- Gen.alphaStr
    } yield User(
      id,
      LoginInfo(providerId, providerKey),
      LocalDirectoryData()
    )
  }

  def genAuthToken(userId: UUID) =
    for {
      id <- Gen.uuid
      datetime <- genDateTimeWithinRange(new DateTime(1, 1, 1, 0, 0), Period.years(294275))
    } yield AuthToken(id, userId, datetime)

  def genOauth1Info: Gen[OAuth1Info] =
    for {
      token <- Gen.alphaStr
      secret <- Gen.alphaStr
    } yield OAuth1Info(token, secret)

  def genAddressString: Gen[String] = genHexString(40).map(_.toLowerCase)

  def genErc20Token: Gen[Erc20Token] =
    for {
      symbolLength <- Gen.choose(4, 7)
      symbol <- Gen.listOfN(symbolLength, Gen.alphaChar).map(_.mkString)
      name <- Gen.alphaStr
      decimals <- Gen.choose(0, 18)
      contractAddress <- genAddressString
      contract = Address(contractAddress)
    } yield Erc20Token(symbol, name, decimals, contract, None, None)

  def genPendingTransfer(fromUserId: UUID, tokenSymbol: String): Gen[PendingTransfer] =
    for {
      id <- Gen.choose(0L, Long.MaxValue)
      providerKey <- Gen.numStr
      amount <- Gen.choose(1, 100000)
      created <- genDateTimeWithinRange(new DateTime(1, 1, 1, 0, 0), Period.years(12345))
    } yield PendingTransfer(id, fromUserId, LoginInfo("twitter", providerKey), tokenSymbol, amount, created, None)

  def genOffchainTransfer(tokenSymbol: String): Gen[OffChainTransfer] =
    for {
      id <- Gen.choose(0L, Long.MaxValue)
      from <- genAddressString
      to <- genAddressString
      amount <- Gen.choose(1, 100000)
      created <- genDateTimeWithinRange(new DateTime(1, 1, 1, 0, 0), Period.years(12345))
    } yield OffChainTransfer(id, tokenSymbol, Address(from), Address(to), Uint(256, amount), created, None)

  def genHexString(length: Int): Gen[String] =
    for {
      low <- Gen.choose[Char]('a', 'f')
      upper <- Gen.choose[Char]('A', 'F')
      number <- Gen.choose[Char]('0', '9')
      hexString <- Gen.listOfN(length, Gen.oneOf(low, upper, number)).map(_.mkString)
    } yield hexString

  def genAddress: Gen[String] = genHexString(40).map(_.toLowerCase)

  def genEthHash: Gen[String] = genHexString(64)

  def genEncodedData: Gen[String] =
    for {
      data <- genHexString(64)
    } yield s"0x$data"

  def genErc20Transfer(token: Erc20Token) = for {
    fromValue <- genAddress
    from = Address(fromValue)
    toValue <- genAddress
    to = Address(toValue)
    valueValue <- Gen.choose(0, 10)
    value = Uint(256, valueValue)
    blockValue <- Gen.choose(0, 10)
    block = Block(blockValue)
    txHashValue <- genEthHash
    txHash = EthereumHash(txHashValue)
    txIndex <- Gen.choose(0, 10)
    processedDate <- genDateTimeWithinRange(new DateTime(1, 1, 1, 0, 0), Period.years(12345))
  } yield TransferEvent(0L, token.symbol, from, to, value, block, txHash, txIndex, Some(processedDate))

  def genTwitterAccount(userID: UUID): Gen[TwitterAccount] = for {
    accountID <- Gen.choose(0L, Long.MaxValue)
    screenName <- Gen.alphaNumStr
    verified <- Gen.oneOf(true, false)
    followers <- Gen.choose(0, Int.MaxValue)
    avatarUrl <- Gen.option(Gen.alphaStr)
  } yield TwitterAccount(userID, accountID, screenName, verified, followers, avatarUrl)

  def genEthereumAccount(userID: UUID): Gen[EthereumAccount] = for {
    address <- genAddress
    password <- Gen.alphaNumStr
  } yield EthereumAccount(userID, Address(address), password)

  def genThreeDigitsInt: Gen[Int] =
    for {
      number <- Gen.choose(100, 999)
    } yield number

  def genNumber: Gen[Int] =
    for {
      number <- Gen.choose(1, Int.MaxValue)
    } yield number

  def genDecimals: Gen[Int] =
    for {
      number <- Gen.choose(0, 18)
    } yield number

  def genStringNumber(length: Int): Gen[String] =
    for {
      stringNumber <- Gen.listOfN(length, Gen.choose[Char]('0', '9')).map(_.mkString)
    } yield stringNumber

  def genLocalDirectoryData() =
    for {
      jsonKey1 <- Gen.alphaNumStr
      jsonKey2 <- Gen.alphaNumStr
      jsonVal1 <- Gen.alphaNumStr
      jsonVal2 <- Gen.alphaNumStr
      jsonString = s"""{"$jsonKey1":"$jsonVal1","$jsonKey2":"$jsonVal2"}"""
      json = parse(jsonString).right.get
    } yield LocalDirectoryData(json)

}
