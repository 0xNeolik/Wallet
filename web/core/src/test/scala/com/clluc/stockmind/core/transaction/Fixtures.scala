package com.clluc.stockmind.core.transaction

import java.util.UUID

import cats.syntax.either._
import com.clluc.stockmind.core.Generators.{genAddress, genEthHash}
import com.clluc.stockmind.core.RawValueParser
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.transaction.TransactionOps.{WithdrawInfo, WithdrawRequest}
import com.clluc.stockmind.core.transaction.TransactionOpsFixture.BalanceByEthAddressTokenKey
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterHandle}
import com.clluc.stockmind.core.user._

import scala.util.Random

/**
  * Data that simulates a coherent state for the entire application.
  * It is meant to be reused by unit tests.
  */
private[transaction] object Fixtures {

  val sourceUserId: UUID = UUID.randomUUID()

  val destinationUserId: UUID = UUID.randomUUID()

  val sourceEthAccountPassword: String = Random.nextString(10)

  val masterEthAccountPassword: String = Random.nextString(10)

  def genAddressSample: Address = Address(genAddress.sample.get)

  val destinationEthAccountAddress: Address = genAddressSample

  val destinationEthAccPwd: String = Random.nextString(10)

  val tokenContractAdd: Address = genAddressSample

  val sourceEthAccountAddress: Address = genAddressSample

  val sourceUserTwitterScreenName: String = "sourceHandle"

  val destinationUserTwitterScreenName: String = "destHandle"

  val destinationUserEmail: String = "es@es.com"

  val sourceUserTwitterNumFollowers: Int = 114

  val sourceUserTwitterId: Long = Random.nextLong()

  val destUserTwitterId: Long = Random.nextLong()

  val externalEthAddress: Address = genAddressSample

  val tokenDecimals = 1

  def genTxHashSample: String = s"0x${genEthHash.sample.get}"

  val expectedTxHashInWithdrawals: String = genTxHashSample

  val unexpectedEthResponseInWithdrawals = "unexpected and unparseable body"

  val unexpectedEthResponseStatus = 200

  val token: Erc20Token =
    Erc20Token(
      symbol = "SLD",
      erc_type = "ERC-20",
      name = "Solid",
      decimals = tokenDecimals,
      contract = tokenContractAdd,
      owner = None,
      birthBlock = None
    )

  val token721: Erc721Token =
    Erc721Token(
      symbol = "SLD",
      erc_type = "ERC-721",
      name = "Solid",
      meta = "metadata",
      id = RawValueParser.parseIntoRawValue("1", token.decimals).get,
      contract = tokenContractAdd,
      owner = None,
      birthBlock = None
    )

  val ethtoken: Ethtoken =
    Ethtoken(
      symbol = "SLD",
      erc_type = "ERC-20",
      name = "Solid",
      contract = tokenContractAdd,
      owner = None,
      birthBlock = None
    )

  val sourceEthAccount: EthereumAccount =
    EthereumAccount(
      user = sourceUserId,
      address = sourceEthAccountAddress,
      password = sourceEthAccountPassword
    )

  val destinationEthereumAccount: EthereumAccount =
    EthereumAccount(
      user = destinationUserId,
      address = destinationEthAccountAddress,
      password = destinationEthAccPwd
    )

  def fromAmountToWithdrawalValue(amount: String): BigInt =
    RawValueParser.parseIntoRawValue(amount, token.decimals).get

  val masterAccountAddress: Address = genAddressSample

  def expectedSignableTx(amount: String): SignableTransaction = {
    val contract = HumanStandardTokenContract(ethtoken)
    SignableTransaction(
      contract.transfer(masterAccountAddress,
                        destinationEthAccountAddress,
                        Uint(value = fromAmountToWithdrawalValue(amount))),
      sourceEthAccount.password
    )
  }

  val tokenSymbol: String        = token.symbol
  val tokenType: String          = token.erc_type
  val tokenSymbolandType: String = token.symbol + "|" + token.erc_type
  val token721Id: BigInt         = token721.id
  val token721Meta: String       = token721.meta

  def withdrawRequestFixture(amount: String): WithdrawRequest =
    WithdrawRequest(
      sourceUserId,
      masterAccountAddress,
      destinationEthAccountAddress,
      token.symbol,
      token.erc_type,
      amount
    )

  def withdrawInfoFixture(): WithdrawInfo =
    WithdrawInfo(
      TransactionSourceInfo(ethtoken,
                            sourceEthAccount,
                            Balance(sourceEthAccount.address, token, 100, 200, 0, 100, 100),
                            10),
      masterAccountAddress,
      destinationEthAccountAddress
    )

  private def transactionRequestFixture(
      amount: String,
      destination: Either[Address, TwitterHandle]): TransactionRequest =
    TransactionRequest(
      sourceUserId = sourceUserId,
      masterAccountAddress = masterAccountAddress,
      destination = destination,
      tokenSymbol = tokenSymbol,
      erc_type = tokenType,
      amount = amount
    )

  def transactionRequestFixtureForTwitterHandle(amount: String): TransactionRequest =
    transactionRequestFixture(amount,
                              TwitterHandle(destinationUserTwitterScreenName).asRight[Address])

  def transactionRequestFixtureForTwitterHandleAndMetaInf(amount: String,
                                                          _metaInf: Map[String, String]) =
    transactionRequestFixtureForTwitterHandle(amount).copy(metaInf = Some(_metaInf))

  def transactionRequestFixtureForEthereumAddressInStockmind(amount: String): TransactionRequest =
    transactionRequestFixture(amount, destinationEthAccountAddress.asLeft[TwitterHandle])

  def transactionRequestFixtureForEthereumAddressInStockmindAndMetaInf(
      amount: String,
      _metaInf: Map[String, String]) =
    transactionRequestFixtureForEthereumAddressInStockmind(amount).copy(metaInf = Some(_metaInf))

  def transactionRequestFixtureForEthereumAddressNotInStockmind(
      amount: String): TransactionRequest =
    transactionRequestFixture(amount, externalEthAddress.asLeft[TwitterHandle])

  def transactionRequestFixtureForEthereumAddressNotInStockmindAndMetaInf(
      amount: String,
      _metaInf: Map[String, String]) =
    transactionRequestFixtureForEthereumAddressNotInStockmind(amount).copy(metaInf = Some(_metaInf))

  def transactionRequestFixtureForMasterAccountAddress(amount: String): TransactionRequest =
    transactionRequestFixture(amount, masterAccountAddress.asLeft[TwitterHandle])

  val twitterUserCredentials = OAuth1Info(
    "twitterKey",
    "twitterSecret"
  )

  val sourceUserTwitterAccount: TwitterAccount = TwitterAccount(
    userID = sourceUserId,
    accountID = sourceUserTwitterId,
    screenName = sourceUserTwitterScreenName,
    verified = false,
    followers = sourceUserTwitterNumFollowers,
    avatarURL = None
  )

  val destinationUserTwitterAccount: TwitterAccount = TwitterAccount(
    userID = destinationUserId,
    accountID = destUserTwitterId,
    screenName = destinationUserTwitterScreenName,
    verified = false,
    followers = 30,
    avatarURL = None
  )

  val sourceBalance = Balance(
    ethAddress = sourceEthAccountAddress,
    token = token,
    totalSent = 100,
    totalReceived = 200,
    totalWithheld = 0,
    realBalance = 100,
    effectiveBalance = 100
  )

  val sourceBalance721 = Balance721(
    ethAddress = sourceEthAccountAddress,
    token = token721,
    totalSent = 100,
    totalReceived = 200,
    totalWithheld = 0,
    realBalance = 100,
    effectiveBalance = 100
  )

  val destinationBalance = Balance(
    ethAddress = destinationEthAccountAddress,
    token = token,
    totalSent = 50,
    totalReceived = 60,
    totalWithheld = 0,
    realBalance = 10,
    effectiveBalance = 10
  )

  val sourceBalanceTuple: (BalanceByEthAddressTokenKey, Balance) =
    BalanceByEthAddressTokenKey(sourceEthAccountAddress, tokenSymbol) -> sourceBalance

  val providerId = "twitter"

  val sourceUserLoginInfo = LoginInfo(
    providerId,
    Random.nextString(8)
  )

  val sourceUser = User(
    userID = sourceUserId,
    loginInfo = sourceUserLoginInfo,
    directoryData = LocalDirectoryData(),
    "identifier"
  )

  val destinationUserLoginInfo = LoginInfo(
    providerId,
    destUserTwitterId.toString
  )

  val destinationUser = User(
    userID = destinationUserId,
    loginInfo = destinationUserLoginInfo,
    directoryData = LocalDirectoryData(),
    "identifier"
  )

  val sourceUserOAuth1Info = OAuth1Info(
    Random.nextString(8),
    Random.nextString(8)
  )

  val stockmindUrl = "https://stockmind.io"

  val savedOffChainTransferId: Long = Random.nextLong

  val happyPathTokenTransactionFixture = new TransactionOpsFixture(
    stockmindUserForId = Map(
      sourceUserId      -> sourceUser,
      destinationUserId -> destinationUser
    ),
    oauth1InfoFromLogin = Map(
      sourceUserLoginInfo -> sourceUserOAuth1Info
    ),
    userFromOauth = Map(
      sourceUserLoginInfo -> sourceUserId
    ),
    twitterIdForHandle = Map(
      TwitterHandle(destinationUserTwitterScreenName) -> destUserTwitterId
    ),
    emailIdForHandle = Map(
      EmailHandle(destinationUserEmail) -> destinationUser
    ),
    emailIdForHandleString = Map(
      sourceUser.identifier -> sourceUser.userID
    ),
    twitterAccountByUserId = Map(
      sourceUserId      -> sourceUserTwitterAccount,
      destinationUserId -> destinationUserTwitterAccount
    ),
    ethereumAccountForUser = Map(
      destinationUserId -> destinationEthereumAccount,
      sourceUserId      -> sourceEthAccount
    ),
    tokens = Map(tokenSymbolandType -> ethtoken),
    balanceForAccountToken = Map(
      BalanceByEthAddressTokenKey(
        sourceEthAccountAddress,
        tokenSymbol
      ) -> sourceBalance,
      BalanceByEthAddressTokenKey(
        destinationEthAccountAddress,
        tokenSymbol
      ) -> destinationBalance
    ),
    sendWithdrawTxResult = Right(expectedTxHashInWithdrawals),
    _stockmindUrl = stockmindUrl,
    savedOutboundTransferId = savedOffChainTransferId,
    _isOmnibusAccountAddress = false,
    tokensErc20BySymbolAndType = Map(
      tokenSymbolandType -> token,
    ),
    tokens721ByIdAndOwner = Map(
      (token721Id, sourceEthAccountAddress) -> token721
    ),
    findBalance721ForEthereumAddressAndTokenId = Map(
      (sourceEthAccountAddress, token721Id) -> sourceBalance721
    )
  )
}
