package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.Generators.{genAddress, genEthHash, generateSample}
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterUserInfo}
import com.clluc.stockmind.core.user.{LocalDirectoryData, User}
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Random

object RetrieveTransactionsOpsSpecFixtures {

  object RetrieveTransactionsUtils {
    // *********************************
    // Functions to generate data
    // *********************************
    def randomEthAdd: Address = Address(generateSample(genAddress))

    def randomEthHash: EthereumHash = EthereumHash(generateSample(genEthHash))

    def createEthAcc(add: Address, userId: UUID) = EthereumAccount(
      userId,
      add,
      Random.nextString(10)
    )

    def createErc20Token(symbol: String,
                         erc_type: String,
                         name: String,
                         decimals: Int): Erc20Token =
      Erc20Token(
        symbol,
        erc_type,
        name,
        decimals,
        randomEthAdd,
        None,
        None
      )

    def createEthToken(symbol: String, erc_type: String, name: String): Ethtoken =
      Ethtoken(
        symbol,
        erc_type,
        name,
        randomEthAdd,
        None,
        None
      )

    def createErc721Token(symbol: String,
                          erc_type: String,
                          name: String,
                          meta: String,
                          id: BigInt): Erc721Token =
      Erc721Token(
        symbol,
        erc_type,
        name,
        meta,
        id,
        randomEthAdd,
        None,
        None
      )

    def createTwitterAcc(userId: UUID,
                         screenName: String,
                         avatarURL: Option[String]): TwitterAccount =
      TwitterAccount(
        userID = userId,
        accountID = Random.nextLong(),
        screenName = screenName,
        verified = false,
        followers = Random.nextInt(500),
        avatarURL = avatarURL
      )

    def randomStr = Random.nextString(10)

    def generateLoginInfo = {
      val authProviderKey = "twitter"

      def generateProviderKey = Random.nextLong().toString

      LoginInfo(authProviderKey, generateProviderKey)
    }

    def createTwitterUserInfo(
        screenName: String,
        fullName: String,
        avatarUrl: String
    ): TwitterUserInfo =
      TwitterUserInfo(
        screenName = screenName,
        fullName = fullName,
        isVerified = false,
        followersCount = Random.nextInt(500),
        avatarUrl = avatarUrl
      )

    def createDate(millisSinceEpoch: Long): DateTime =
      new DateTime(millisSinceEpoch).withZone(DateTimeZone.UTC)

    def addressRepr(add: Address): String = add.toHex

    def hashRepr(hash: EthereumHash): String = hash.toPrefixedHexString
  }

  import RetrieveTransactionsUtils._

  object TestFirstPersonData {
    // *********************************
    // User for which we retrieve transactions
    // *********************************
    val userForTestId         = UUID.randomUUID()
    val userForTestEthAdd     = randomEthAdd
    val userForTestEthAcc     = createEthAcc(userForTestEthAdd, userForTestId)
    val userForTestScreenName = "userForTestSn"
    val userForTestAvatarUrl  = "http://user.png"

    val userForTestTwitterAcc =
      createTwitterAcc(userForTestId, userForTestScreenName, Some(userForTestAvatarUrl))
    val userForTestFirstName = "userForTest"
    val userForTestLastName  = "deLosDolores"
    val userForTestFullName  = "UserForTest DeLosDolores"
    val userForTestEmail     = "delosdolores@gmail.com"
    val userForTestOAuthInfo = OAuth1Info(randomStr, randomStr)

    val userForTestInfo = User(
      userID = userForTestId,
      loginInfo = generateLoginInfo,
      directoryData = LocalDirectoryData(),
      "identifier"
    )
  }

  import TestFirstPersonData._

  object OffchainIncomingTxData {
    // ********* CREATE OffchainTransfer INSTANCE ********* //
    val offchainIncomingTxTokenSymbol        = "TK1"
    val offchainIncomingTxTokenSymbolAndType = "TK1|ERC-20"
    val offchainIncomingTx20TokenType        = "ERC-20"
    val offchainIncomingTx721TokenType       = "ERC-721"
    val offchainIncomingTx721TokenMeta       = "Metadata"

    val offchainIncomingTx721TokenId =
      "71120786848863412851373030999642871879808768922518165984257232620739138279176"
    val offchainIncomingTxTokenName         = "Token 1"
    val offchainIncomingTxTokenDecimals     = 5
    val offchainIncomingTxCounterpartUserId = UUID.randomUUID
    val offchainIncomingTxFromAdd           = randomEthAdd
    val offchainIncomingTxAmount            = Uint(value = 10L)
    val offchainIncomingTxCreated           = createDate(10000L)

    val offchain20IncomingTx = OffChainTransfer(
      id = Random.nextLong(),
      tokenSymbol = offchainIncomingTxTokenSymbol,
      erc_type = offchainIncomingTx20TokenType,
      from = offchainIncomingTxFromAdd,
      to = userForTestEthAdd,
      amount = offchainIncomingTxAmount,
      created = offchainIncomingTxCreated,
      onchainTransferId = None
    )

    val offchain721IncomingTx = OffChainTransfer(
      id = Random.nextLong(),
      tokenSymbol = offchainIncomingTxTokenSymbol,
      erc_type = offchainIncomingTx721TokenType,
      from = offchainIncomingTxFromAdd,
      to = userForTestEthAdd,
      amount = Uint(256, 1),
      created = offchainIncomingTxCreated,
      onchainTransferId = None
    )

    // ********* DATA FOR OPS FIXTURE ********* //
    val offchainIncomingTxSourceUserTwScreenName = "sn1"
    val offchainIncomingTxSourceUserTwAvatarUrl  = "http://sn1.png"

    val offchainIncomingTxEthereumErc20Token = createErc20Token(
      symbol = offchainIncomingTxTokenSymbol,
      erc_type = offchainIncomingTx20TokenType,
      name = offchainIncomingTxTokenName,
      decimals = offchainIncomingTxTokenDecimals
    )

    val offchainIncomingTxEthToken = createEthToken(symbol = offchainIncomingTxTokenSymbol,
                                                    erc_type = offchainIncomingTx20TokenType,
                                                    name = offchainIncomingTxTokenName)

    val offchainIncomingTxEthereumErc721Token = createErc721Token(
      symbol = offchainIncomingTxTokenSymbol,
      erc_type = offchainIncomingTx721TokenType,
      name = offchainIncomingTxTokenName,
      meta = offchainIncomingTx721TokenMeta,
      id = BigInt(offchainIncomingTx721TokenId)
    )

    // Ethereum account for counterparty address map
    val offchainIncomingTxEthAccForCounterpartUserMap =
      Map[UUID, EthereumAccount](
        offchainIncomingTxCounterpartUserId -> createEthAcc(
          offchainIncomingTxFromAdd,
          offchainIncomingTxCounterpartUserId
        )
      )

    // Ethereum account for each address involved
    val ethereumAccountForAddressesMap = Map[Address, EthereumAccount](
      offchainIncomingTxFromAdd -> createEthAcc(offchainIncomingTxFromAdd,
                                                offchainIncomingTxCounterpartUserId)
    )

    // erc20Token for the transaction (map)
    val offchainIncomingTxEthErc20TokenBySymbolMap = Map[String, Erc20Token](
      offchainIncomingTxTokenSymbolAndType -> offchainIncomingTxEthereumErc20Token
    )

    // erc20Token for the transaction (map)
    val offchainIncomingTxEthTokenBySymbolMap = Map[String, Ethtoken](
      offchainIncomingTxTokenSymbol -> offchainIncomingTxEthToken
    )

    // erc721Token for the transaction (map)
    val offchainIncomingTxEthErc721TokenBySymbolMap = Map[String, Erc721Token](
      offchainIncomingTxTokenSymbol -> offchainIncomingTxEthereumErc721Token
    )

    // Twitter account for counterpart user
    val offchainIncomingTxTwitterAccForCounterpartUserMap =
      Map[UUID, TwitterAccount](
        offchainIncomingTxCounterpartUserId -> createTwitterAcc(
          offchainIncomingTxCounterpartUserId,
          offchainIncomingTxSourceUserTwScreenName,
          Some(offchainIncomingTxSourceUserTwAvatarUrl)
        )
      )

    // Info for counterpart user
    val offchainIncomingTxCounterpartUserInfoMap = {
      val user = User(
        offchainIncomingTxCounterpartUserId,
        generateLoginInfo,
        LocalDirectoryData(),
        "identifier"
      )

      Map[UUID, User](
        offchainIncomingTxCounterpartUserId -> user
      )
    }

    // ********* EXPECTED StockmindTransaction INSTANCE ********* //
    val offchain20IncomingStockmindTx = StockmindTransaction(
      id = offchain20IncomingTx.id,
      direction = IncomingTx,
      pending = false,
      counterparty = Counterparty(
        Some(addressRepr(offchainIncomingTxFromAdd)),
        None
      ),
      token = offchainIncomingTxEthereumErc20Token.symbol,
      erc_type = offchain20IncomingTx.erc_type,
      tokenDescription = offchainIncomingTxEthereumErc20Token.name,
      decimals = offchainIncomingTxEthereumErc20Token.decimals,
      amount = TokenAmount("0", "00010"),
      txHash = None, // it's incoming; no Tx hash
      date = offchain20IncomingTx.created
    )

    // ********* EXPECTED StockmindTransaction INSTANCE ********* //
    val offchain721IncomingStockmindTx = Stockmind721Transaction(
      id = offchain721IncomingTx.id,
      direction = IncomingTx,
      pending = false,
      counterparty = Counterparty(
        Some(addressRepr(offchainIncomingTxFromAdd)),
        None
      ),
      token = offchainIncomingTxEthereumErc721Token.symbol,
      erc_type = offchain721IncomingTx.erc_type,
      tokenDescription = offchainIncomingTxEthereumErc721Token.name,
      meta = offchainIncomingTxEthereumErc721Token.meta,
      token_id = offchainIncomingTxEthereumErc721Token.id,
      txHash = None, // it's incoming; no Tx hash
      date = offchain721IncomingTx.created
    )

  }

  object OffchainOutgoingTxData {
    // ********* CREATE OffchainTransfer INSTANCE ********* //
    val offchainOutgoingTxTokenSymbol        = "TK2"
    val offchainOutgoingTxTokenSymbolAndType = "TK2|ERC-20"
    val offchainOutgoingTxToken20Type        = "ERC-20"
    val offchainOutgoingTxToken721Type       = "ERC-721"
    val offchainOutgoingTxTokenName          = "Token 2"
    val offchainOutgoingTxTokenDecimals      = 8
    val offchainOutgoingTxCounterpartUserId  = UUID.randomUUID
    val offchainOutgoingTxToAdd              = randomEthAdd
    val offchainOutgoingTxAmount             = Uint(value = 50000L)
    val offchainOutgoingTxCreated            = createDate(40000L)

    val offchainOutgoingTxEthereum20Token = createErc20Token(
      symbol = offchainOutgoingTxTokenSymbol,
      erc_type = offchainOutgoingTxToken20Type,
      name = offchainOutgoingTxTokenName,
      decimals = offchainOutgoingTxTokenDecimals
    )

    val offchainOutgoingTxEthoken = createEthToken(
      symbol = offchainOutgoingTxTokenSymbol,
      erc_type = offchainOutgoingTxToken20Type,
      name = offchainOutgoingTxTokenName,
    )

    val offchain20OutgoingTx = OffChainTransfer(
      id = Random.nextLong(),
      tokenSymbol = offchainOutgoingTxTokenSymbol,
      erc_type = offchainOutgoingTxToken20Type,
      from = userForTestEthAdd,
      to = offchainOutgoingTxToAdd,
      amount = offchainOutgoingTxAmount,
      created = offchainOutgoingTxCreated
    )

    val offchain721OutgoingTx = OffChainTransfer(
      id = Random.nextLong(),
      tokenSymbol = offchainOutgoingTxTokenSymbol,
      erc_type = offchainOutgoingTxToken721Type,
      from = userForTestEthAdd,
      to = offchainOutgoingTxToAdd,
      amount = offchainOutgoingTxAmount,
      created = offchainOutgoingTxCreated
    )

    // ********* DATA FOR OPS FIXTURE ********* //
    val offchainOutgoingTxDestUserTwScreenName = "osn1"
    val offchainOutgoingTxDestUserTwAvatarUrl  = "http://osn1.png"

    // Ethereum account for counterpart user
    val offchainOutgoingTxEthAccountForCounterpartUserMap =
      Map[UUID, EthereumAccount](
        offchainOutgoingTxCounterpartUserId -> createEthAcc(
          offchainOutgoingTxToAdd,
          offchainOutgoingTxCounterpartUserId
        )
      )

    // Ethereum account for counterparty address map
    val ethereumAccountForAddressesMap = Map[Address, EthereumAccount](
      offchainOutgoingTxToAdd -> createEthAcc(offchainOutgoingTxToAdd,
                                              offchainOutgoingTxCounterpartUserId)
    )

    // Token for the transaction
    val offchainOutgoingExEthTokenBySymbolMap = Map[String, Ethtoken](
      offchainOutgoingTxTokenSymbol -> offchainOutgoingTxEthoken
    )

    val offchainOutgoingExErc20TokenBySymbolMap = Map[String, Erc20Token](
      offchainOutgoingTxTokenSymbolAndType -> offchainOutgoingTxEthereum20Token
    )

    // Twitter account for counterpart user
    val offchainOutgoingTxTwitterAccForCounterpartUserMap =
      Map[UUID, TwitterAccount](
        offchainOutgoingTxCounterpartUserId -> createTwitterAcc(
          userId = offchainOutgoingTxCounterpartUserId,
          screenName = offchainOutgoingTxDestUserTwScreenName,
          avatarURL = Some(offchainOutgoingTxDestUserTwAvatarUrl)
        )
      )

    // Info for counterpart user
    val offchainOutgoingTxCounterpartUserInfoMap = {
      val user = User(
        offchainOutgoingTxCounterpartUserId,
        generateLoginInfo,
        LocalDirectoryData(),
        "identifier"
      )

      Map[UUID, User](
        offchainOutgoingTxCounterpartUserId -> user
      )
    }

    // ********* EXPECTED StockmindTransaction INSTANCE ********* //
    val offchain20OutgoingStockmindTx = StockmindTransaction(
      id = offchain20OutgoingTx.id,
      direction = OutgoingTx,
      pending = false,
      counterparty = Counterparty(
        Some(addressRepr(offchainOutgoingTxToAdd)),
        None
      ),
      token = offchainOutgoingTxEthereum20Token.symbol,
      erc_type = offchainOutgoingTxEthereum20Token.erc_type,
      tokenDescription = offchainOutgoingTxEthereum20Token.name,
      decimals = offchainOutgoingTxEthereum20Token.decimals,
      amount = TokenAmount("0", "00050000"),
      None, // It's off-chain; even though it's incoming there's no Tx hash
      date = offchain20OutgoingTx.created
    )
  }

  object OnchainIncomingTxData {
    // ********* CREATE OnchainTransfer INSTANCE ********* //
    val onchainIncomingTxTokenSymbol        = "TK3"
    val onchainIncomingTxTokenSymbolAndType = "TK3|ERC-20"
    val onchainIncomingTxTokenType          = "ERC-20"
    val onchainIncomingTxTokenName          = "Token 3"
    val onchainIncomingTxTokenDecimals      = 8
    val onchainIncomingTxFromAdd            = randomEthAdd
    val onchainIncomingTxAmount             = Uint(value = 435263748L)
    val onchainIncomingTxCreated            = createDate(60000L)
    val onchainId                           = Random.nextLong()
    val masterEthAdd                        = randomEthAdd
    val onchainTxHash                       = randomEthHash

    val onchain20IncomingTx = OffChainTransfer(
      id = Random.nextLong(),
      tokenSymbol = onchainIncomingTxTokenSymbol,
      erc_type = onchainIncomingTxTokenType,
      from = masterEthAdd,
      to = userForTestEthAdd,
      amount = onchainIncomingTxAmount,
      created = onchainIncomingTxCreated,
      onchainTransferId = Some(onchainId)
    )

    // Ethereum account for counterpart user doesn't exist (is on-chain)
    // Token for the transaction (map)
    val onchainIncomingTxEthTokenBySymbolMap = Map[String, Ethtoken](
      onchainIncomingTxTokenSymbol -> createEthToken(
        onchainIncomingTxTokenSymbol,
        onchainIncomingTxTokenType,
        onchainIncomingTxTokenName,
      )
    )

    val onchainIncomingTxEthTokenBySymbolMap2 = Map[String, Erc20Token](
      onchainIncomingTxTokenSymbolAndType -> createErc20Token(
        onchainIncomingTxTokenSymbol,
        onchainIncomingTxTokenType,
        onchainIncomingTxTokenName,
        onchainIncomingTxTokenDecimals
      )
    )

    // Twitter account for counterpart user doesn't exist either
    // Info for counterpart user doesn't exist
    // Onchain transaction detail
    val onchain20Tx = TransferEvent(
      id = onchainId,
      tokenSymbol = onchainIncomingTxTokenSymbol,
      erc_type = onchainIncomingTxTokenType,
      from = onchainIncomingTxFromAdd,
      to = userForTestEthAdd,
      value = onchainIncomingTxAmount,
      block = Block(Random.nextInt(50000)),
      txHash = onchainTxHash,
      txIndex = Random.nextInt(50000),
      processedDate = Some(onchainIncomingTxCreated.minusSeconds(3))
    )

    val onchain20TxFromIdMap = Map[Long, TransferEvent](
      onchainId -> onchain20Tx
    )

    // ********* EXPECTED StockmindTransaction INSTANCE ********* //
    val onchainIncoming20StockmindTx = StockmindTransaction(
      id = onchain20IncomingTx.id,
      direction = IncomingTx,
      pending = false,
      counterparty = Counterparty(
        Some(addressRepr(onchainIncomingTxFromAdd)), // It's on-chain and incoming; so it comes from an actual transaction in ethereum
        None
      ),
      token = onchainIncomingTxTokenSymbol,
      erc_type = onchainIncomingTxTokenType,
      tokenDescription = onchainIncomingTxTokenName,
      decimals = onchainIncomingTxTokenDecimals,
      amount = TokenAmount("4", "35263748"),
      txHash = Some(hashRepr(onchainTxHash)),
      date = onchain20IncomingTx.created
    )
  }

  object OnchainOutgoingTxData {
    // ********* CREATE OnchainTransfer INSTANCE ********* //
    val onchainOutgoingTxTokenSymbol        = "TK4"
    val onchainOutgoingTxTokenSymbolAndType = "TK4|ERC-20"
    val onchainOutgoingTxTokenType          = "ERC-20"
    val onchainOutgoingTxTokenName          = "Token 4"
    val onchainOutgoingTxTokenDecimals      = 4
    val onchainOutgoingTxCounterpartUserId  = UUID.randomUUID
    val onchainOutgoingTxToAdd              = randomEthAdd
    val onchainOutgoingTxAmount             = Uint(value = 463748L)
    val onchainOutgoingTxCreated            = createDate(20000L)
    val onchainId                           = Random.nextLong()
    val masterEthAdd                        = randomEthAdd
    val onchainTxHash                       = randomEthHash

    val onchainOutgoingTx = OffChainTransfer(
      Random.nextLong(),
      onchainOutgoingTxTokenSymbol,
      onchainOutgoingTxTokenType,
      userForTestEthAdd,
      masterEthAdd,
      onchainOutgoingTxAmount,
      onchainOutgoingTxCreated,
      Some(onchainId)
    )

    // ********* DATA FOR OPS FIXTURE ********* //
    // Ethereum account for counterpart user doesn't exist (is on-chain)
    // Token for the transaction (map)
    val onchainOutgoingTxEthTokenBySymbolMap = Map[String, Ethtoken](
      onchainOutgoingTxTokenSymbol -> createEthToken(onchainOutgoingTxTokenSymbol,
                                                     onchainOutgoingTxTokenType,
                                                     onchainOutgoingTxTokenName)
    )

    val onchainOutgoingTxErc20TokenBySymbolMap = Map[String, Erc20Token](
      onchainOutgoingTxTokenSymbolAndType -> createErc20Token(onchainOutgoingTxTokenSymbol,
                                                              onchainOutgoingTxTokenType,
                                                              onchainOutgoingTxTokenName,
                                                              onchainOutgoingTxTokenDecimals)
    )

    // Twitter account for counterpart user doesn't exist either
    // Info for counterpart user doesn't exist
    // Onchain transaction detail
    val onchainTx = TransferEvent(
      id = onchainId,
      tokenSymbol = onchainOutgoingTxTokenSymbol,
      erc_type = onchainOutgoingTxTokenType,
      from = masterEthAdd,
      to = onchainOutgoingTxToAdd,
      value = onchainOutgoingTxAmount,
      block = Block(Random.nextInt(60000)),
      txHash = onchainTxHash,
      txIndex = Random.nextInt(50000),
      processedDate = Some(onchainOutgoingTxCreated.plusSeconds(3))
    )

    val onchainTxFromIdMap = Map[Long, TransferEvent](
      onchainId -> onchainTx
    )

    // ********* EXPECTED StockmindTransaction INSTANCE ********* //
    val onchainOutgoingStockmindTx = StockmindTransaction(
      id = onchainOutgoingTx.id,
      direction = OutgoingTx,
      pending = false,
      counterparty = Counterparty(
        Some(addressRepr(onchainOutgoingTxToAdd)), // It's on-chain and outgoing; so it comes from an actual transaction in ethereum
        None
      ),
      token = onchainOutgoingTxTokenSymbol,
      erc_type = onchainOutgoingTxTokenType,
      tokenDescription = onchainOutgoingTxTokenName,
      decimals = onchainOutgoingTxTokenDecimals,
      amount = TokenAmount("46", "3748"),
      Some(hashRepr(onchainTxHash)),
      date = onchainOutgoingTx.created
    )
  }

  object PendingTxsData {
    // Pending transfers themselves
    val pending1ReceiverLoginInfo = generateLoginInfo
    val pendingTokenSymbol        = "TK5"
    val pendingTokenSymbolAndType = "TK5|ERC-20"
    val pendingTokenType          = "ERC-20"
    val pendingTokenName          = "Token5"
    val pendingTokenDecimals      = 2
    val pending1Amount            = 3
    val pending1Created           = createDate(30000L)

    val pendingTx1 =
      PendingTransfer(
        id = Random.nextLong(),
        fromUser = userForTestId,
        toFutureUser = pending1ReceiverLoginInfo,
        tokenSymbol = pendingTokenSymbol,
        erc_type = pendingTokenType,
        amount = pending1Amount,
        created = pending1Created,
        processed = None
      )

    val pending2ReceiverLoginInfo = generateLoginInfo
    val pending2Amount            = 5
    val pending2Created           = createDate(50000L)

    val pendingTx2 =
      PendingTransfer(
        id = Random.nextLong(),
        fromUser = userForTestId,
        toFutureUser = pending2ReceiverLoginInfo,
        tokenSymbol = pendingTokenSymbol,
        erc_type = pendingTokenType,
        amount = pending2Amount,
        created = pending2Created,
        processed = None
      )

    // ********* DATA FOR OPS FIXTURE ********* //
    val pendingTransfersByIssuerMap = Map[UUID, List[PendingTransfer]](
      userForTestId -> List(pendingTx1, pendingTx2)
    )

    val ethereumTokenBySymbolMap = Map[String, Ethtoken](
      pendingTokenSymbol -> createEthToken(
        pendingTokenSymbol,
        pendingTokenType,
        pendingTokenName
      )
    )

    val erc20TokenBySymbolMap = Map[String, Erc20Token](
      pendingTokenSymbolAndType -> createErc20Token(
        pendingTokenSymbol,
        pendingTokenType,
        pendingTokenName,
        pendingTokenDecimals
      )
    )

    val pendingTx1DestScreenName = "ptDestSn1"
    val pendingTx1DestFullName   = "ptDestFullName1"
    val pendingTx1DestAvatarUrl  = "http://ptDestFullName1"

    val pendingTx2DestScreenName = "ptDestSn2"
    val pendingTx2DestFullName   = "ptDestFullName2"
    val pendingTx2DestAvatarUrl  = "http://ptDestFullName2"

    val twitterUserInfoByTwitterIdMapForPending1 = Map[Long, TwitterUserInfo](
      pending1ReceiverLoginInfo.providerKey.toLong -> createTwitterUserInfo(
        pendingTx1DestScreenName,
        pendingTx1DestFullName,
        pendingTx1DestAvatarUrl
      ))

    val twitterUserInfoByTwitterIdMapForPending2 = Map[Long, TwitterUserInfo](
      pending2ReceiverLoginInfo.providerKey.toLong -> createTwitterUserInfo(
        pendingTx2DestScreenName,
        pendingTx2DestFullName,
        pendingTx2DestAvatarUrl
      ))

    // ********* EXPECTED StockmindTransaction INSTANCE ********* //
    val pendingTx1StockmindTx = StockmindTransaction(
      id = pendingTx1.id,
      direction = OutgoingTx,
      pending = true,
      counterparty = Counterparty(
        None,
        None
      ),
      token = pendingTokenSymbol,
      erc_type = pendingTokenType,
      tokenDescription = pendingTokenName,
      decimals = pendingTokenDecimals,
      amount = TokenAmount("0", "03"),
      txHash = None,
      date = pendingTx1.created
    )

    val pendingTx2StockmindTx = StockmindTransaction(
      id = pendingTx2.id,
      direction = OutgoingTx,
      pending = true,
      counterparty = Counterparty(
        None,
        None
      ),
      token = pendingTokenSymbol,
      erc_type = pendingTokenType,
      tokenDescription = pendingTokenName,
      decimals = pendingTokenDecimals,
      amount = TokenAmount("0", "05"),
      txHash = None,
      date = pendingTx2.created
    )
  }
}
