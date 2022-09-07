package com.clluc.stockmind.core.transaction

import java.util.UUID

import cats.data.EitherT
import cats.{Applicative, Functor, Monad}
import com.clluc.stockmind.core.RawValueParser.parseIntoRawValue
import com.clluc.stockmind.core.auth.{LoginInfo, OAuth1Info}
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.JsonRpcPlainResult
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.transaction.TokenTransactionError._
import com.clluc.stockmind.core.transaction.TransactionOps.{
  TransferRequest,
  TransferRequest721,
  TransferRequestUser,
  TransferRequestUser721,
  WithdrawInfo,
  WithdrawInfo721,
  WithdrawRequest,
  WithdrawRequest721
}
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterHandle}
import com.clluc.stockmind.core.user.{Balance, Balance721, EmailHandle, User}
import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.applicative._

import com.clluc.stockmind.core.transaction.TransactionResult.transactionIsCompleted
import org.joda.time.DateTime

private[transaction] trait TransactionOps[P[_]] {

  /*
   *******************************************************
   * PRIMITIVE OPERATIONS
   *******************************************************
   */
  def savePendingTransaction(transfer: PendingTransfer): P[TransactionResult]

  /**
    * Notify through websocket both the issuer and the recipient of a transfer.
    * @param transfer
    * @return
    */
  def notifyStockmindTransferParties(transfer: OffChainTransfer): P[Unit]

  def notifyPendingTransfer(transfer: Address): P[Unit]

  def findErc20TokenBySymbolAndType(symbol_erc_type: String): P[Option[Erc20Token]]

  def findEthTokenBySymbolAndType(symbol_erc_type: String): P[Option[Ethtoken]]

  def findToken721ByIdAndOwner(id: BigInt, tokenOwner: Address): P[Option[Erc721Token]]

  def findTwitterAccountByUserId(id: UUID): P[Option[TwitterAccount]]

  def findAccountByUserId(id: UUID): P[Option[User]]

  def findTwitterAccountByScreenName(screenName: TwitterHandle): P[Option[TwitterAccount]]

  def findUserByIdentifier(screenName: EmailHandle): P[Option[User]]

  def findEthereumAccountForUserId(id: UUID): P[Option[EthereumAccount]]

  def findEthereumAccountByAddress(address: Address): P[Option[EthereumAccount]]

  def findBalanceForEthereumAddressAndToken(address: Address, token: String): P[Option[Balance]]

  def findBalance721ForEthereumAddressAndTokenId(address: Address,
                                                 idtoken: BigInt): P[Option[Balance721]]

  def findTwitterApiUserIdFromScreenName(screenName: TwitterHandle,
                                         credentials: OAuth1Info): P[Option[Long]]

  def findStockmindUserFromId(id: UUID): P[Option[User]]

  def findOAuth1InfoFromLoginInfo(loginInfo: LoginInfo): P[Option[OAuth1Info]]

  def findUserIdFromOAuthProviderAndLoginKey(loginInfo: LoginInfo): P[Option[UUID]]

  def findUserIdFromOAuthProviderAndIdentifier(identifier: String): P[Option[UUID]]

  def findPendingTransfersByDestination(loginInfo: LoginInfo): P[List[PendingTransfer]]

  def isOmnibusAccountAddress(address: Address): P[Boolean]

  def sendWithdrawTx(signableTx: SignableTransaction): P[JsonRpcPlainResult]

  def writeOffChainTransfer(transfer: OffChainTransfer): P[OffChainTransfer]

  def saveOutboundTransferData(outboundTransfer: OutboundTransfer): P[OutboundTransfer]

  def storePendingTransactionInRepository(pendingTransferId: Long,
                                          tokenTransfer: OffChainTransfer): P[Unit]

  def stockmindUrl: P[String]

  def storeTransactionMetaInf(txId: Long, metaInf: Map[String, String]): P[Unit]

  /*
   *******************************************************
   * DERIVED OPERATIONS
   *******************************************************
   */
  // Misc
  def storeTransactionMetaInfIfAny(txId: Long, metaInf: Option[Map[String, String]])(
      implicit ev: Applicative[P]): P[Unit] = {
    metaInf.map(meta => storeTransactionMetaInf(txId, meta)).getOrElse(().pure[P])
  }

  /*
  // PRIMITIVE OPERATIONS LIFTED TO VALIDATED DATA STRUCTURES
   */

  private type ValidatedTransactionTransformer[A] = EitherT[P, TokenTransactionError, A]

  private object LiftedPrimitiveOperations { // Give this derived operations a name space

    def validateOptionP[A](optionP: P[Option[A]], errorIfNone: TokenTransactionError)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[A] = {

      EitherT(optionP.map(Either.fromOption(_, errorIfNone)))
    }

    def liftTwitterAccountByScreenName(
        screenName: TwitterHandle
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[TwitterAccount] =
      validateOptionP(findTwitterAccountByScreenName(screenName),
                      nonExistentTwitterUser(screenName))

    def liftUserByIdentifier(
        email: EmailHandle
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[User] =
      validateOptionP(findUserByIdentifier(email), nonExistentUser(email))

    def liftFindEthereumAccountForUserId(
        userId: UUID,
        twitterScreenName: String
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[EthereumAccount] =
      validateOptionP(findEthereumAccountForUserId(userId),
                      userDoesNotHaveEthAccountInPlatform(userId, twitterScreenName))

    def liftFindEthereumAccountForUserIdIdentifier(
        userId: UUID,
        identifier: String
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[EthereumAccount] =
      validateOptionP(findEthereumAccountForUserId(userId),
                      UserDoesNotHaveEthAccountInPlatformIdentifier(userId, identifier))

    def liftEthereumAccountForTwitterScreenName(
        twitterScreenName: String
    )(implicit ev: Monad[P]): ValidatedTransactionTransformer[EthereumAccount] = {

      for {
        twitterAcc <- liftTwitterAccountByScreenName(TwitterHandle(twitterScreenName))
        ethAcc     <- liftFindEthereumAccountForUserId(twitterAcc.userID, twitterAcc.screenName)
      } yield ethAcc
    }

    def liftEthereumAccountForUserIdentifier(
        identifier: String
    )(implicit ev: Monad[P]): ValidatedTransactionTransformer[EthereumAccount] = {

      for {
        userAcc <- liftUserByIdentifier(EmailHandle(identifier))
        ethAcc  <- liftFindEthereumAccountForUserIdIdentifier(userAcc.userID, userAcc.identifier)
      } yield ethAcc
    }

    def liftEthereumAccountForAddress(address: Address)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[EthereumAccount] =
      validateOptionP(findEthereumAccountByAddress(address), noEthereumAccountForAddress(address))

    def liftBalanceForAddressAndToken(
        token: Erc20Token,
        account: EthereumAccount
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[Balance] = {
      validateOptionP(findBalanceForEthereumAddressAndToken(account.address, token.symbol),
                      sourceUserHasNoBalance())
    }

    def liftBalance721ForAddressAndTokenId(
        token: Erc721Token,
        account: EthereumAccount
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[Balance721] = {
      validateOptionP(findBalance721ForEthereumAddressAndTokenId(account.address, token.id),
                      sourceUserHasNoBalance())
    }

    def liftBalance(
        effectiveBalance: BigInt,
        transferSourceAddress: String,
        tokenSymbol: String,
        tokenDecimals: Int,
        requestedAmountAsString: String
    )(implicit ev: Applicative[P]): ValidatedTransactionTransformer[BigInt] = {

      val validatedAmount: Either[TokenTransactionError, BigInt] =
        Either.fromOption(
          parseIntoRawValue(requestedAmountAsString, tokenDecimals),
          specifiedAmountAsStringNotValid(requestedAmountAsString, tokenDecimals)
        )

      def validatedEnoughAmount(rawAmount: BigInt): Either[TokenTransactionError, BigInt] =
        validatedAmount.ensure(
          sourceUserHasNotEnoughBalance(transferSourceAddress,
                                        tokenSymbol,
                                        effectiveBalance,
                                        rawAmount)
        )(_ <= effectiveBalance)

      EitherT.fromEither[P](
        validatedAmount.flatMap(validatedEnoughAmount)
      )
    }

    def liftTransactionSourceInfo(
        tokenSymbol: String,
        erc_type: String,
        transferSourceUserId: UUID,
        transferSourceTwitterScreenName: String,
        requestedAmountStr: String
    )(implicit ev: Monad[P]): ValidatedTransactionTransformer[TransactionSourceInfo] = {
      for {
        token <- liftErc20TokenBySymbolAndType(tokenSymbol, erc_type)
        account <- liftFindEthereumAccountForUserId(transferSourceUserId,
                                                    transferSourceTwitterScreenName)
        balance <- liftBalanceForAddressAndToken(token, account)
        amount <- liftBalance(
          balance.effectiveBalance,
          account.address.value,
          token.symbol,
          token.decimals,
          requestedAmountStr
        )
      } yield
        TransactionSourceInfo(Ethtoken(token.symbol,
                                       token.erc_type,
                                       token.name,
                                       token.contract,
                                       token.owner,
                                       token.birthBlock),
                              account,
                              balance,
                              amount)
    }

    def liftTransaction721SourceInfo(
        transferSourceUserId: UUID,
        transferSourceTwitterScreenName: String,
        tokenid: BigInt
    )(implicit ev: Monad[P]): ValidatedTransactionTransformer[TransactionSourceInfo721] = {
      for {
        account <- liftFindEthereumAccountForUserId(transferSourceUserId,
                                                    transferSourceTwitterScreenName)
        token   <- liftToken721ByIdAndOwner(tokenid, account.address)
        balance <- liftBalance721ForAddressAndTokenId(token, account)
        amount <- liftBalance(
          balance.effectiveBalance,
          account.address.value,
          token.symbol,
          0,
          "1"
        )
      } yield
        TransactionSourceInfo721(Ethtoken(token.symbol,
                                          token.erc_type,
                                          token.name,
                                          token.contract,
                                          token.owner,
                                          token.birthBlock),
                                 account,
                                 tokenid)
    }

    def liftTransactionSourceInfoIdentifier(
        tokenSymbol: String,
        erc_type: String,
        transferSourceUserId: UUID,
        identifier: String,
        requestedAmountStr: String
    )(implicit ev: Monad[P]): ValidatedTransactionTransformer[TransactionSourceInfo] = {
      for {
        token   <- liftErc20TokenBySymbolAndType(tokenSymbol, erc_type)
        account <- liftFindEthereumAccountForUserIdIdentifier(transferSourceUserId, identifier)
        balance <- liftBalanceForAddressAndToken(token, account)
        amount <- liftBalance(
          balance.effectiveBalance,
          account.address.value,
          token.symbol,
          token.decimals,
          requestedAmountStr
        )
      } yield
        TransactionSourceInfo(Ethtoken(token.symbol,
                                       token.erc_type,
                                       token.name,
                                       token.contract,
                                       token.owner,
                                       token.birthBlock),
                              account,
                              balance,
                              amount)
    }

    def liftTransaction721SourceInfoIdentifier(
        transferSourceUserId: UUID,
        identifier: String,
        tokenid: BigInt
    )(implicit ev: Monad[P]): ValidatedTransactionTransformer[TransactionSourceInfo721] = {
      for {
        account <- liftFindEthereumAccountForUserIdIdentifier(transferSourceUserId, identifier)
        token   <- liftToken721ByIdAndOwner(tokenid, account.address)
        balance <- liftBalance721ForAddressAndTokenId(token, account)
        amount <- liftBalance(
          balance.effectiveBalance,
          account.address.value,
          token.symbol,
          0,
          "1"
        )
      } yield
        TransactionSourceInfo721(Ethtoken(token.symbol,
                                          token.erc_type,
                                          token.name,
                                          token.contract,
                                          token.owner,
                                          token.birthBlock),
                                 account,
                                 tokenid)
    }

    def liftErc20TokenBySymbolAndType(
        tokenSymbol: String,
        erc_type: String
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[Erc20Token] =
      validateOptionP(findErc20TokenBySymbolAndType(tokenSymbol + "|" + erc_type),
                      tokenForTransferNotInPlatform(tokenSymbol))

    def liftTokenBySymbolAndType(
        tokenSymbol: String,
        erc_type: String
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[Ethtoken] =
      validateOptionP(findEthTokenBySymbolAndType(tokenSymbol + "|" + erc_type),
                      tokenForTransferNotInPlatform(tokenSymbol))

    def liftToken721ByIdAndOwner(id: BigInt, tokenOwner: Address)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[Erc721Token] =
      validateOptionP(findToken721ByIdAndOwner(id, tokenOwner),
                      tokenForTransferNotInPlatform(id.toString())) //TODO

    def liftUserById(id: UUID)(implicit ev: Functor[P]): ValidatedTransactionTransformer[User] =
      validateOptionP(findStockmindUserFromId(id), transferSourceUserDoesNotExist(id))

    def liftUserIdFromOAuthProviderAndLoginKey(
        oauthProvider: String,
        key: String
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[UUID] =
      validateOptionP(
        findUserIdFromOAuthProviderAndLoginKey(LoginInfo(oauthProvider, key)),
        notUserWithLoginKey(oauthProvider, key)
      )

    def liftUserIdFromOAuthProviderAndIdentifier(
        oauthProvider: String,
        identifier: String
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[UUID] =
      validateOptionP(
        findUserIdFromOAuthProviderAndIdentifier(identifier),
        notUserWithIdentifier(oauthProvider, identifier)
      )

    def liftOauth1InfoForUser(user: User)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[OAuth1Info] =
      validateOptionP(findOAuth1InfoFromLoginInfo(user.loginInfo),
                      twitterCredentialsForTransferSenderNotValid(user.userID))

    def liftTwitterApiUserIdFromScreenName(
        screenName: TwitterHandle
    )(
        twitterUserCredentials: OAuth1Info
    )(implicit ev: Functor[P]): ValidatedTransactionTransformer[Long] =
      validateOptionP(
        findTwitterApiUserIdFromScreenName(
          screenName,
          twitterUserCredentials
        ),
        destinationUserHasNoTwitterAccount(screenName)
      )

    /**
      * Given an address, returns a Left if that address is the omnibus one; or Right with the given address if it's not
      * @param address
      * @tparam P The context or higher kind into which our computations are to be wrapped
      * @return
      */
    def liftOmnibusAddress(address: Address)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[Address] =
      EitherT(
        isOmnibusAccountAddress(address).map {
          Either.cond(_, address, triedWithdrawToInvalidAccount())
        }
      )

    def liftTwitterAccountByUserId(userId: UUID)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[TwitterAccount] =
      validateOptionP(findTwitterAccountByUserId(userId), noTwitterAccountForStockmindUser(userId))

    def liftAccountByUserId(userId: UUID)(
        implicit ev: Functor[P]): ValidatedTransactionTransformer[User] =
      validateOptionP(findAccountByUserId(userId), noAccountForStockmindUser(userId))

  }

  /*
   // CODE THAT PROCESS REGULAR TRANSACTIONS (TRANSACTION REQUESTS)
   */

  import LiftedPrimitiveOperations._

  def processTransaction(
      transactionRequest: TransactionRequest,
      credentials: OAuth1Info,
      timestampFx: => DateTime,
      masterAccountPassword: String
  )(implicit ev: Monad[P]): P[ValidatedTransaction[TransactionResult]] = {

    /*
     * Some auxiliary functions that handle different transaction use cases
     */
    def transferRequestFromTwitterHandle(handle: TwitterHandle) =
      TransferRequest(
        transactionRequest.sourceUserId,
        handle,
        transactionRequest.tokenSymbol,
        transactionRequest.erc_type,
        transactionRequest.amount,
        transactionRequest.metaInf
      )

    // This is an off chain 'send tokens to another platform user' action
    def offchainTransfer(
        request: TransferRequest
    ): P[ValidatedTransaction[TransactionResult]] = {
      (
        for {
          recipientEthereumAccount <- liftEthereumAccountForTwitterScreenName(
            request.destinationScreenName.value)
          senderTwitterAccount <- liftTwitterAccountByUserId(request.sourceUserId)
          transferSourceInfo <- liftTransactionSourceInfo(
            request.tokenSymbol,
            request.erc_type,
            request.sourceUserId,
            senderTwitterAccount.screenName,
            request.amount
          )
          offChainTx = OffChainTransfer(
            tokenSymbol = transferSourceInfo.token.symbol,
            erc_type = transferSourceInfo.token.erc_type,
            from = transferSourceInfo.account.address,
            to = recipientEthereumAccount.address,
            amount = Uint(value = transferSourceInfo.amount),
            created = timestampFx
          )
          writeTxResult <- EitherT.liftT[P, TokenTransactionError, OffChainTransfer](
            writeOffChainTransfer(offChainTx)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            storeTransactionMetaInfIfAny(writeTxResult.id, request.metaInf)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            notifyStockmindTransferParties(writeTxResult)
          )
        } yield transactionIsCompleted()
      ).value
    }

    def inPlatformOffchainTransfer(
        request: TransferRequest
    ): P[ValidatedTransaction[TransactionResult]] = {

      def pendingTransfer(): P[ValidatedTransaction[TransactionResult]] = {
        (
          for {
            _ <- EitherT.fromEither[P](
              if (request.metaInf.isDefined)
                Left(metaInfoNotAllowedInPendingTransfers())
              else
                Right(())
            )
            targetUserTwitterId <- liftTwitterApiUserIdFromScreenName(
              request.destinationScreenName)(credentials)
            senderTwitterAccount <- liftTwitterAccountByUserId(request.sourceUserId)
            transferSourceInfo <- liftTransactionSourceInfo(
              request.tokenSymbol,
              request.erc_type,
              request.sourceUserId,
              senderTwitterAccount.screenName,
              request.amount
            )
            pendingTransfer = PendingTransfer(
              fromUser = request.sourceUserId,
              toFutureUser = LoginInfo("twitter", targetUserTwitterId.toString),
              tokenSymbol = request.tokenSymbol,
              erc_type = request.erc_type,
              amount = transferSourceInfo.amount,
              created = timestampFx,
              processed = None
            )
            sourceUserId = pendingTransfer.fromUser
            ethAcc <- liftFindEthereumAccountForUserId(sourceUserId,
                                                       senderTwitterAccount.screenName)
            _ <- EitherT.liftT[P, TokenTransactionError, Unit](
              notifyPendingTransfer(ethAcc.address))
            txResult <- EitherT.liftT[P, TokenTransactionError, TransactionResult](
              savePendingTransaction(pendingTransfer))
          } yield txResult
        ).value
      }

      def isStockmindUser(destinationTwitterScreenName: TwitterHandle): P[Boolean] =
        findTwitterAccountByScreenName(destinationTwitterScreenName).map(_.isDefined)

      isStockmindUser(request.destinationScreenName).flatMap {
        if (_) {
          offchainTransfer(request)
        } else {
          pendingTransfer()
        }
      }
    }

    /*
     * Function that handles a transaction to another ethereum address
     */
    def ethereumAddressCase(address: Address): P[ValidatedTransaction[TransactionResult]] = {

      def transferRequestFromEthereumAddressRepr(
          addressRepr: String): ValidatedTransactionTransformer[TransferRequest] =
        for {
          ethAcc     <- liftEthereumAccountForAddress(Address(addressRepr))
          twitterAcc <- liftTwitterAccountByUserId(ethAcc.user)
        } yield transferRequestFromTwitterHandle(TwitterHandle(twitterAcc.screenName))

      def translateIntoOffChainTransfer(
          addressRepr: String
      ): P[ValidatedTransaction[TransactionResult]] = {
        (for {
          transferRequest <- transferRequestFromEthereumAddressRepr(addressRepr)
          transferResult  <- EitherT(offchainTransfer(transferRequest))
        } yield transferResult).value
      }

      def doWithdraw(
          address: Address
      ): P[ValidatedTransaction[TransactionResult]] = {

        def withdrawAction(
            withdrawRequest: WithdrawRequest
        ): P[ValidatedTransaction[OutboundTransfer]] = {

          lazy val parsedWithdrawRequest: ValidatedTransactionTransformer[WithdrawInfo] =
            for {
              senderTwitterAccount <- liftTwitterAccountByUserId(withdrawRequest.sourceUserId)
              withdrawSourceInfo <- liftTransactionSourceInfo(
                withdrawRequest.tokenSymbol,
                withdrawRequest.erc_type,
                withdrawRequest.sourceUserId,
                senderTwitterAccount.screenName,
                withdrawRequest.amount
              )
              sourceAddress      = withdrawRequest.masterAccountAddress
              destinationAddress = withdrawRequest.destinationEthereumAddress
            } yield WithdrawInfo(withdrawSourceInfo, sourceAddress, destinationAddress)

          def liftSendWithdrawTx(
              signableTx: SignableTransaction
          ): ValidatedTransactionTransformer[String] =
            EitherT(sendWithdrawTx(signableTx).map(_.leftMap(ethereumIssue)))

          def liftWriteOffChainTransfer(
              transfer: OffChainTransfer
          ): ValidatedTransactionTransformer[OffChainTransfer] =
            EitherT(writeOffChainTransfer(transfer).map(_.asRight[TokenTransactionError]))

          def liftGenerateOffchainTransfer(
              wi: WithdrawInfo,
              timestampFx: => DateTime
          ): ValidatedTransactionTransformer[OffChainTransfer] = {

            def generateOffChainTransfer(
                wi: WithdrawInfo,
                timestampFx: => DateTime
            ): OffChainTransfer = {
              OffChainTransfer(
                tokenSymbol = wi.transactionInfo.token.symbol,
                erc_type = wi.transactionInfo.token.erc_type,
                from = wi.transactionInfo.account.address,
                to = wi.masterAddress,
                amount = Uint(value = wi.transactionInfo.amount),
                created = timestampFx,
                onchainTransferId = None
              )
            }

            EitherT.fromEither(
              generateOffChainTransfer(wi, timestampFx).asRight[TokenTransactionError])
          }

          def liftGenerateWithdrawTx(
              wi: WithdrawInfo
          ): ValidatedTransactionTransformer[SignableTransaction] = {

            def generateWithdrawTx(
                wi: WithdrawInfo
            ): SignableTransaction = {

              if (wi.transactionInfo.token.symbol == "ETH") {

                SignableTransaction(EthTransaction(wi.masterAddress,
                                                   wi.destinationAddress,
                                                   wi.transactionInfo.amount),
                                    masterAccountPassword)

              } else {

                val contract = HumanStandardTokenContract(wi.transactionInfo.token)

                SignableTransaction(
                  contract.transfer(
                    from = wi.masterAddress,
                    to = wi.destinationAddress,
                    value = Uint(value = wi.transactionInfo.amount)
                  ),
                  masterAccountPassword
                )
              }
            }

            EitherT.fromEither(generateWithdrawTx(wi).asRight[TokenTransactionError])
          }

          def liftSaveOutboundTransferData(
              data: OutboundTransfer): ValidatedTransactionTransformer[OutboundTransfer] =
            EitherT(saveOutboundTransferData(data).map(_.asRight[TokenTransactionError]))

          def liftEthereumHashFromString(
              txHash: String): ValidatedTransactionTransformer[EthereumHash] =
            EitherT.fromOption(
              EthereumHash.decodePrefixedHexString(txHash),
              resultingEthereumTxHashNotValid(txHash)
            )

          (
            for {
              withdrawInfo           <- parsedWithdrawRequest
              signableTx             <- liftGenerateWithdrawTx(withdrawInfo)
              offchainTransfer       <- liftGenerateOffchainTransfer(withdrawInfo, timestampFx)
              offchainTransferResult <- liftWriteOffChainTransfer(offchainTransfer)
              withdrawResultTxHash   <- liftSendWithdrawTx(signableTx)
              resultingEthHash       <- liftEthereumHashFromString(withdrawResultTxHash)
              outboundTransfer = OutboundTransfer(resultingEthHash, offchainTransferResult.id)
              result <- liftSaveOutboundTransferData(outboundTransfer)
              _ <- EitherT.liftT[P, TokenTransactionError, Unit](
                storeTransactionMetaInfIfAny(offchainTransferResult.id, withdrawRequest.metaInf)
              )
            } yield result
          ).value
        }

        val withdrawResult: P[ValidatedTransaction[OutboundTransfer]] =
          withdrawAction(
            WithdrawRequest(
              transactionRequest.sourceUserId,
              transactionRequest.masterAccountAddress,
              address,
              transactionRequest.tokenSymbol,
              transactionRequest.erc_type,
              transactionRequest.amount,
              transactionRequest.metaInf
            )
          )

        EitherT(withdrawResult).map(_ => transactionIsCompleted()).value
      }

      def isStockmindAddress(address: Address): P[Boolean] =
        findEthereumAccountByAddress(address).map(_.isDefined)

      /*
       * Possible use cases:
       *  - We get a twitter handler; it's just a plain call to processAndDoTransfer.
       *  - We get an ethereum address. Several possible scenarios:
       *    - The ethereum address is the master / omnibus: forbid the operation
       *    - The ethereum address belongs to a Stockmind user: handle it as an of-chain transfer calling doTransferWithEffects
       *    - Any other case, call processAndDoWithdraw
       */

      (for {
        stockmnindAdd <- isStockmindAddress(address)
        omnibusAdd    <- isOmnibusAccountAddress(address)
      } yield {
        if (omnibusAdd) triedWithdrawToInvalidAccount().asLeft[TransactionResult].pure[P]
        else if (stockmnindAdd) translateIntoOffChainTransfer(address.value)
        else doWithdraw(address)
      }).flatten
    }

    /*
     * Actual processTransfer logic from here:
     *  - Resolve which use case we are in.
     *  - Take the appropriate action
     */

    val transferDestination: Either[Address, TwitterHandle] = transactionRequest.destination

    transferDestination match {
      case Left(address) =>
        ethereumAddressCase(address)

      case Right(twitterHandle) =>
        inPlatformOffchainTransfer(transferRequestFromTwitterHandle(twitterHandle))
    }
  }

  def processTransaction721(
      transactionRequest: TransactionRequest721,
      credentials: OAuth1Info,
      timestampFx: => DateTime,
      masterAccountPassword: String
  )(implicit ev: Monad[P]): P[ValidatedTransaction[TransactionResult]] = {

    /*
     * Some auxiliary functions that handle different transaction use cases
     */
    def transferRequestFromTwitterHandle(handle: TwitterHandle) =
      TransferRequest721(
        transactionRequest.sourceUserId,
        handle,
        transactionRequest.id,
        transactionRequest.metaInf
      )

    // This is an off chain 'send tokens to another platform user' action
    def offchainTransfer(
        request: TransferRequest721
    ): P[ValidatedTransaction[TransactionResult]] = {
      (
        for {
          recipientEthereumAccount <- liftEthereumAccountForTwitterScreenName(
            request.destinationScreenName.value)
          senderTwitterAccount <- liftTwitterAccountByUserId(request.sourceUserId)
          transferSourceInfo <- liftTransaction721SourceInfo(
            request.sourceUserId,
            senderTwitterAccount.screenName,
            request.tokenId
          )
          offChainTx = OffChainTransfer(
            tokenSymbol = transferSourceInfo.token.symbol,
            erc_type = transferSourceInfo.token.erc_type,
            from = transferSourceInfo.account.address,
            to = recipientEthereumAccount.address,
            amount = Uint(value = 1),
            created = timestampFx,
            token_id = Some(request.tokenId)
          )
          writeTxResult <- EitherT.liftT[P, TokenTransactionError, OffChainTransfer](
            writeOffChainTransfer(offChainTx)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            storeTransactionMetaInfIfAny(writeTxResult.id, request.metaInf)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            notifyStockmindTransferParties(writeTxResult)
          )
        } yield transactionIsCompleted()
      ).value
    }

    def inPlatformOffchainTransfer(
        request: TransferRequest721
    ): P[ValidatedTransaction[TransactionResult]] = {

      def pendingTransfer(): P[ValidatedTransaction[TransactionResult]] = {
        (
          for {
            _ <- EitherT.fromEither[P](
              if (request.metaInf.isDefined)
                Left(metaInfoNotAllowedInPendingTransfers())
              else
                Right(())
            )
            targetUserTwitterId <- liftTwitterApiUserIdFromScreenName(
              request.destinationScreenName)(credentials)
            senderTwitterAccount <- liftTwitterAccountByUserId(request.sourceUserId)
            transferSourceInfo <- liftTransaction721SourceInfo(
              request.sourceUserId,
              senderTwitterAccount.screenName,
              request.tokenId
            )
            pendingTransfer = PendingTransfer(
              fromUser = request.sourceUserId,
              toFutureUser = LoginInfo("twitter", targetUserTwitterId.toString),
              tokenSymbol = transferSourceInfo.token.symbol,
              erc_type = transferSourceInfo.token.erc_type,
              amount = 1,
              created = timestampFx,
              processed = None,
              token_id = Some(request.tokenId)
            )
            sourceUserId = pendingTransfer.fromUser
            ethAcc <- liftFindEthereumAccountForUserId(sourceUserId,
                                                       senderTwitterAccount.screenName)
            _ <- EitherT.liftT[P, TokenTransactionError, Unit](
              notifyPendingTransfer(ethAcc.address))
            txResult <- EitherT.liftT[P, TokenTransactionError, TransactionResult](
              savePendingTransaction(pendingTransfer))
          } yield txResult
        ).value
      }

      def isStockmindUser(destinationTwitterScreenName: TwitterHandle): P[Boolean] =
        findTwitterAccountByScreenName(destinationTwitterScreenName).map(_.isDefined)

      isStockmindUser(request.destinationScreenName).flatMap {
        if (_) {
          offchainTransfer(request)
        } else {
          pendingTransfer()
        }
      }
    }

    /*
     * Function that handles a transaction to another ethereum address
     */
    def ethereumAddressCase(address: Address): P[ValidatedTransaction[TransactionResult]] = {

      def transferRequestFromEthereumAddressRepr(
          addressRepr: String): ValidatedTransactionTransformer[TransferRequest721] =
        for {
          ethAcc     <- liftEthereumAccountForAddress(Address(addressRepr))
          twitterAcc <- liftTwitterAccountByUserId(ethAcc.user)
        } yield transferRequestFromTwitterHandle(TwitterHandle(twitterAcc.screenName))

      def translateIntoOffChainTransfer(
          addressRepr: String
      ): P[ValidatedTransaction[TransactionResult]] = {
        (for {
          transferRequest <- transferRequestFromEthereumAddressRepr(addressRepr)
          transferResult  <- EitherT(offchainTransfer(transferRequest))
        } yield transferResult).value
      }

      def doWithdraw(
          address: Address
      ): P[ValidatedTransaction[TransactionResult]] = {

        def withdrawAction(
            withdrawRequest: WithdrawRequest721
        ): P[ValidatedTransaction[OutboundTransfer]] = {

          lazy val parsedWithdrawRequest: ValidatedTransactionTransformer[WithdrawInfo721] =
            for {
              senderTwitterAccount <- liftTwitterAccountByUserId(withdrawRequest.sourceUserId)
              withdrawSourceInfo <- liftTransaction721SourceInfo(
                withdrawRequest.sourceUserId,
                senderTwitterAccount.screenName,
                withdrawRequest.tokenId
              )
              sourceAddress      = withdrawRequest.masterAccountAddress
              destinationAddress = withdrawRequest.destinationEthereumAddress
            } yield WithdrawInfo721(withdrawSourceInfo, sourceAddress, destinationAddress)

          def liftSendWithdrawTx(
              signableTx: SignableTransaction
          ): ValidatedTransactionTransformer[String] =
            EitherT(sendWithdrawTx(signableTx).map(_.leftMap(ethereumIssue)))

          def liftWriteOffChainTransfer(
              transfer: OffChainTransfer
          ): ValidatedTransactionTransformer[OffChainTransfer] =
            EitherT(writeOffChainTransfer(transfer).map(_.asRight[TokenTransactionError]))

          def liftGenerateOffchainTransfer(
              wi: WithdrawInfo721,
              timestampFx: => DateTime
          ): ValidatedTransactionTransformer[OffChainTransfer] = {

            def generateOffChainTransfer(
                wi: WithdrawInfo721,
                timestampFx: => DateTime
            ): OffChainTransfer = {
              OffChainTransfer(
                tokenSymbol = wi.transactionInfo.token.symbol,
                erc_type = wi.transactionInfo.token.erc_type,
                from = wi.transactionInfo.account.address,
                to = wi.masterAddress,
                amount = Uint(value = 1),
                created = timestampFx,
                onchainTransferId = None,
                token_id = Some(wi.transactionInfo.tokenId)
              )
            }

            EitherT.fromEither(
              generateOffChainTransfer(wi, timestampFx).asRight[TokenTransactionError])
          }

          def liftGenerateWithdrawTx(
              wi: WithdrawInfo721
          ): ValidatedTransactionTransformer[SignableTransaction] = {

            def generateWithdrawTx(
                wi: WithdrawInfo721
            ): SignableTransaction = {

              val contract = NFTTokenContract(wi.transactionInfo.token)

              SignableTransaction(
                contract.transfer(
                  from = wi.masterAddress,
                  to = wi.destinationAddress,
                  id = Uint(256, wi.transactionInfo.tokenId)
                ),
                masterAccountPassword
              )

            }

            EitherT.fromEither(generateWithdrawTx(wi).asRight[TokenTransactionError])
          }

          def liftSaveOutboundTransferData(
              data: OutboundTransfer): ValidatedTransactionTransformer[OutboundTransfer] =
            EitherT(saveOutboundTransferData(data).map(_.asRight[TokenTransactionError]))

          def liftEthereumHashFromString(
              txHash: String): ValidatedTransactionTransformer[EthereumHash] =
            EitherT.fromOption(
              EthereumHash.decodePrefixedHexString(txHash),
              resultingEthereumTxHashNotValid(txHash)
            )

          (
            for {
              withdrawInfo           <- parsedWithdrawRequest
              signableTx             <- liftGenerateWithdrawTx(withdrawInfo)
              offchainTransfer       <- liftGenerateOffchainTransfer(withdrawInfo, timestampFx)
              offchainTransferResult <- liftWriteOffChainTransfer(offchainTransfer)
              withdrawResultTxHash   <- liftSendWithdrawTx(signableTx)
              resultingEthHash       <- liftEthereumHashFromString(withdrawResultTxHash)
              outboundTransfer = OutboundTransfer(resultingEthHash, offchainTransferResult.id)
              result <- liftSaveOutboundTransferData(outboundTransfer)
              _ <- EitherT.liftT[P, TokenTransactionError, Unit](
                storeTransactionMetaInfIfAny(offchainTransferResult.id, withdrawRequest.metaInf)
              )
            } yield result
          ).value
        }

        val withdrawResult: P[ValidatedTransaction[OutboundTransfer]] =
          withdrawAction(
            WithdrawRequest721(
              transactionRequest.sourceUserId,
              transactionRequest.masterAccountAddress,
              address,
              transactionRequest.id,
              transactionRequest.metaInf
            )
          )

        EitherT(withdrawResult).map(_ => transactionIsCompleted()).value
      }

      def isStockmindAddress(address: Address): P[Boolean] =
        findEthereumAccountByAddress(address).map(_.isDefined)

      /*
       * Possible use cases:
       *  - We get a twitter handler; it's just a plain call to processAndDoTransfer.
       *  - We get an ethereum address. Several possible scenarios:
       *    - The ethereum address is the master / omnibus: forbid the operation
       *    - The ethereum address belongs to a Stockmind user: handle it as an of-chain transfer calling doTransferWithEffects
       *    - Any other case, call processAndDoWithdraw
       */

      (for {
        stockmnindAdd <- isStockmindAddress(address)
        omnibusAdd    <- isOmnibusAccountAddress(address)
      } yield {
        if (omnibusAdd) triedWithdrawToInvalidAccount().asLeft[TransactionResult].pure[P]
        else if (stockmnindAdd) translateIntoOffChainTransfer(address.value)
        else doWithdraw(address)
      }).flatten
    }

    /*
     * Actual processTransfer logic from here:
     *  - Resolve which use case we are in.
     *  - Take the appropriate action
     */

    val transferDestination: Either[Address, TwitterHandle] = transactionRequest.destination

    transferDestination match {
      case Left(address) =>
        ethereumAddressCase(address)

      case Right(twitterHandle) =>
        inPlatformOffchainTransfer(transferRequestFromTwitterHandle(twitterHandle))
    }
  }

  def processTransactionUser(
      transactionRequest: TransactionRequestUser,
      timestampFx: => DateTime,
      masterAccountPassword: String
  )(implicit ev: Monad[P]): P[ValidatedTransaction[TransactionResult]] = {

    /*
     * Some auxiliary functions that handle different transaction use cases
     */
    def transferRequestFromEmailHandle(handle: EmailHandle) =
      TransferRequestUser(
        transactionRequest.sourceUserId,
        handle,
        transactionRequest.tokenSymbol,
        transactionRequest.erc_type,
        transactionRequest.amount,
        transactionRequest.metaInf
      )

    // This is an off chain 'send tokens to another platform user' action
    def offchainTransfer(
        request: TransferRequestUser
    ): P[ValidatedTransaction[TransactionResult]] = {
      (
        for {
          recipientEthereumAccount <- liftEthereumAccountForUserIdentifier(
            request.destinationIdentifier.value)
          transferSourceInfo <- liftTransactionSourceInfoIdentifier(
            request.tokenSymbol,
            request.erc_type,
            request.sourceUserId,
            request.destinationIdentifier.value,
            request.amount
          )
          offChainTx = OffChainTransfer(
            tokenSymbol = transferSourceInfo.token.symbol,
            erc_type = transferSourceInfo.token.erc_type,
            from = transferSourceInfo.account.address,
            to = recipientEthereumAccount.address,
            amount = Uint(value = transferSourceInfo.amount),
            created = timestampFx
          )
          writeTxResult <- EitherT.liftT[P, TokenTransactionError, OffChainTransfer](
            writeOffChainTransfer(offChainTx)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            storeTransactionMetaInfIfAny(writeTxResult.id, request.metaInf)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            notifyStockmindTransferParties(writeTxResult)
          )
        } yield transactionIsCompleted()
      ).value
    }

    def inPlatformOffchainTransfer(
        request: TransferRequestUser
    ): P[ValidatedTransaction[TransactionResult]] = {

      def pendingTransfer(): P[ValidatedTransaction[TransactionResult]] = {
        (
          for {
            _ <- EitherT.fromEither[P](
              if (request.metaInf.isDefined)
                Left(metaInfoNotAllowedInPendingTransfers())
              else
                Right(())
            )
            //targetUserId <- liftUserByIdentifier(request.destinationIdentifier)
            transferSourceInfo <- liftTransactionSourceInfoIdentifier(
              request.tokenSymbol,
              request.erc_type,
              request.sourceUserId,
              request.destinationIdentifier.value,
              request.amount
            )
            pendingTransfer = PendingTransfer(
              fromUser = request.sourceUserId,
              toFutureUser = LoginInfo("auth0", request.destinationIdentifier.value),
              tokenSymbol = request.tokenSymbol,
              erc_type = request.erc_type,
              amount = transferSourceInfo.amount,
              created = timestampFx,
              processed = None
            )
            sourceUserId = pendingTransfer.fromUser
            ethAcc <- liftFindEthereumAccountForUserIdIdentifier(
              sourceUserId,
              request.destinationIdentifier.value)
            _ <- EitherT.liftT[P, TokenTransactionError, Unit](
              notifyPendingTransfer(ethAcc.address))
            txResult <- EitherT.liftT[P, TokenTransactionError, TransactionResult](
              savePendingTransaction(pendingTransfer))
          } yield txResult
        ).value
      }

      def isStockmindUser(destinationUserEmail: EmailHandle): P[Boolean] =
        findUserByIdentifier(destinationUserEmail).map(_.isDefined)

      isStockmindUser(request.destinationIdentifier).flatMap {
        if (_) {
          offchainTransfer(request)
        } else {
          pendingTransfer()
        }
      }
    }

    /*
     * Function that handles a transaction to another ethereum address
     */
    def ethereumAddressCase(address: Address): P[ValidatedTransaction[TransactionResult]] = {

      def transferRequestFromEthereumAddressRepr(
          addressRepr: String): ValidatedTransactionTransformer[TransferRequestUser] =
        for {
          ethAcc  <- liftEthereumAccountForAddress(Address(addressRepr))
          userAcc <- liftAccountByUserId(ethAcc.user)
        } yield transferRequestFromEmailHandle(EmailHandle(userAcc.identifier))

      def translateIntoOffChainTransfer(
          addressRepr: String
      ): P[ValidatedTransaction[TransactionResult]] = {
        (for {
          transferRequest <- transferRequestFromEthereumAddressRepr(addressRepr)
          transferResult  <- EitherT(offchainTransfer(transferRequest))
        } yield transferResult).value
      }

      def doWithdraw(
          address: Address
      ): P[ValidatedTransaction[TransactionResult]] = {

        def withdrawAction(
            withdrawRequest: WithdrawRequest
        ): P[ValidatedTransaction[OutboundTransfer]] = {

          lazy val parsedWithdrawRequest: ValidatedTransactionTransformer[WithdrawInfo] =
            for {
              senderAccount <- liftAccountByUserId(withdrawRequest.sourceUserId)
              withdrawSourceInfo <- liftTransactionSourceInfoIdentifier(
                withdrawRequest.tokenSymbol,
                withdrawRequest.erc_type,
                withdrawRequest.sourceUserId,
                senderAccount.identifier,
                withdrawRequest.amount
              )
              sourceAddress      = withdrawRequest.masterAccountAddress
              destinationAddress = withdrawRequest.destinationEthereumAddress
            } yield WithdrawInfo(withdrawSourceInfo, sourceAddress, destinationAddress)

          def liftSendWithdrawTx(
              signableTx: SignableTransaction
          ): ValidatedTransactionTransformer[String] =
            EitherT(sendWithdrawTx(signableTx).map(_.leftMap(ethereumIssue)))

          def liftWriteOffChainTransfer(
              transfer: OffChainTransfer
          ): ValidatedTransactionTransformer[OffChainTransfer] =
            EitherT(writeOffChainTransfer(transfer).map(_.asRight[TokenTransactionError]))

          def liftGenerateOffchainTransfer(
              wi: WithdrawInfo,
              timestampFx: => DateTime
          ): ValidatedTransactionTransformer[OffChainTransfer] = {

            def generateOffChainTransfer(
                wi: WithdrawInfo,
                timestampFx: => DateTime
            ): OffChainTransfer = {
              OffChainTransfer(
                tokenSymbol = wi.transactionInfo.token.symbol,
                erc_type = wi.transactionInfo.token.erc_type,
                from = wi.transactionInfo.account.address,
                to = wi.masterAddress,
                amount = Uint(value = wi.transactionInfo.amount),
                created = timestampFx,
                onchainTransferId = None
              )
            }

            EitherT.fromEither(
              generateOffChainTransfer(wi, timestampFx).asRight[TokenTransactionError])
          }

          def liftGenerateWithdrawTx(
              wi: WithdrawInfo
          ): ValidatedTransactionTransformer[SignableTransaction] = {

            def generateWithdrawTx(
                wi: WithdrawInfo
            ): SignableTransaction = {

              if (wi.transactionInfo.token.symbol == "ETH") {

                SignableTransaction(EthTransaction(wi.masterAddress,
                                                   wi.destinationAddress,
                                                   wi.transactionInfo.amount),
                                    masterAccountPassword)

              } else {

                val contract = HumanStandardTokenContract(wi.transactionInfo.token)

                SignableTransaction(
                  contract.transfer(
                    from = wi.masterAddress,
                    to = wi.destinationAddress,
                    value = Uint(value = wi.transactionInfo.amount)
                  ),
                  masterAccountPassword
                )
              }
            }

            EitherT.fromEither(generateWithdrawTx(wi).asRight[TokenTransactionError])
          }

          def liftSaveOutboundTransferData(
              data: OutboundTransfer): ValidatedTransactionTransformer[OutboundTransfer] =
            EitherT(saveOutboundTransferData(data).map(_.asRight[TokenTransactionError]))

          def liftEthereumHashFromString(
              txHash: String): ValidatedTransactionTransformer[EthereumHash] =
            EitherT.fromOption(
              EthereumHash.decodePrefixedHexString(txHash),
              resultingEthereumTxHashNotValid(txHash)
            )

          (
            for {
              withdrawInfo           <- parsedWithdrawRequest
              signableTx             <- liftGenerateWithdrawTx(withdrawInfo)
              offchainTransfer       <- liftGenerateOffchainTransfer(withdrawInfo, timestampFx)
              offchainTransferResult <- liftWriteOffChainTransfer(offchainTransfer)
              withdrawResultTxHash   <- liftSendWithdrawTx(signableTx)
              resultingEthHash       <- liftEthereumHashFromString(withdrawResultTxHash)
              outboundTransfer = OutboundTransfer(resultingEthHash, offchainTransferResult.id)
              result <- liftSaveOutboundTransferData(outboundTransfer)
              _ <- EitherT.liftT[P, TokenTransactionError, Unit](
                storeTransactionMetaInfIfAny(offchainTransferResult.id, withdrawRequest.metaInf)
              )
            } yield result
          ).value
        }

        val withdrawResult: P[ValidatedTransaction[OutboundTransfer]] =
          withdrawAction(
            WithdrawRequest(
              transactionRequest.sourceUserId,
              transactionRequest.masterAccountAddress,
              address,
              transactionRequest.tokenSymbol,
              transactionRequest.erc_type,
              transactionRequest.amount,
              transactionRequest.metaInf
            )
          )

        EitherT(withdrawResult).map(_ => transactionIsCompleted()).value
      }

      def isStockmindAddress(address: Address): P[Boolean] =
        findEthereumAccountByAddress(address).map(_.isDefined)

      /*
       * Possible use cases:
       *  - We get a twitter handler; it's just a plain call to processAndDoTransfer.
       *  - We get an ethereum address. Several possible scenarios:
       *    - The ethereum address is the master / omnibus: forbid the operation
       *    - The ethereum address belongs to a Stockmind user: handle it as an of-chain transfer calling doTransferWithEffects
       *    - Any other case, call processAndDoWithdraw
       */

      (for {
        stockmnindAdd <- isStockmindAddress(address)
        omnibusAdd    <- isOmnibusAccountAddress(address)
      } yield {
        if (omnibusAdd) triedWithdrawToInvalidAccount().asLeft[TransactionResult].pure[P]
        else if (stockmnindAdd) translateIntoOffChainTransfer(address.value)
        else doWithdraw(address)
      }).flatten
    }

    /*
     * Actual processTransfer logic from here:
     *  - Resolve which use case we are in.
     *  - Take the appropriate action
     */

    val transferDestination: Either[Address, EmailHandle] = transactionRequest.destination

    transferDestination match {
      case Left(address) =>
        ethereumAddressCase(address)

      case Right(emailHandle) =>
        inPlatformOffchainTransfer(transferRequestFromEmailHandle(emailHandle))
    }
  }

  def processTransactionUser721(
      transactionRequest: TransactionRequestUser721,
      timestampFx: => DateTime,
      masterAccountPassword: String
  )(implicit ev: Monad[P]): P[ValidatedTransaction[TransactionResult]] = {

    /*
     * Some auxiliary functions that handle different transaction use cases
     */
    def transferRequestFromEmailHandle(handle: EmailHandle) =
      TransferRequestUser721(
        transactionRequest.sourceUserId,
        handle,
        transactionRequest.id,
        transactionRequest.metaInf
      )

    // This is an off chain 'send tokens to another platform user' action
    def offchainTransfer(
        request: TransferRequestUser721
    ): P[ValidatedTransaction[TransactionResult]] = {
      (
        for {
          recipientEthereumAccount <- liftEthereumAccountForUserIdentifier(
            request.destinationIdentifier.value)
          transferSourceInfo <- liftTransaction721SourceInfoIdentifier(
            request.sourceUserId,
            request.destinationIdentifier.value,
            request.tokenId
          )
          offChainTx = OffChainTransfer(
            tokenSymbol = transferSourceInfo.token.symbol,
            erc_type = transferSourceInfo.token.erc_type,
            from = transferSourceInfo.account.address,
            to = recipientEthereumAccount.address,
            amount = Uint(value = 1),
            created = timestampFx,
            token_id = Some(request.tokenId)
          )
          writeTxResult <- EitherT.liftT[P, TokenTransactionError, OffChainTransfer](
            writeOffChainTransfer(offChainTx)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            storeTransactionMetaInfIfAny(writeTxResult.id, request.metaInf)
          )
          _ <- EitherT.liftT[P, TokenTransactionError, Unit](
            notifyStockmindTransferParties(writeTxResult)
          )
        } yield transactionIsCompleted()
      ).value
    }

    def inPlatformOffchainTransfer(
        request: TransferRequestUser721
    ): P[ValidatedTransaction[TransactionResult]] = {

      def pendingTransfer(): P[ValidatedTransaction[TransactionResult]] = {
        (
          for {
            _ <- EitherT.fromEither[P](
              if (request.metaInf.isDefined)
                Left(metaInfoNotAllowedInPendingTransfers())
              else
                Right(())
            )
            //targetUserId <- liftUserByIdentifier(request.destinationIdentifier)
            transferSourceInfo <- liftTransaction721SourceInfoIdentifier(
              request.sourceUserId,
              request.destinationIdentifier.value,
              request.tokenId
            )
            pendingTransfer = PendingTransfer(
              fromUser = request.sourceUserId,
              toFutureUser = LoginInfo("auth0", request.destinationIdentifier.value),
              tokenSymbol = transferSourceInfo.token.symbol,
              erc_type = transferSourceInfo.token.erc_type,
              amount = 1,
              created = timestampFx,
              processed = None,
              token_id = Some(request.tokenId)
            )
            sourceUserId = pendingTransfer.fromUser
            ethAcc <- liftFindEthereumAccountForUserIdIdentifier(
              sourceUserId,
              request.destinationIdentifier.value)
            _ <- EitherT.liftT[P, TokenTransactionError, Unit](
              notifyPendingTransfer(ethAcc.address))
            txResult <- EitherT.liftT[P, TokenTransactionError, TransactionResult](
              savePendingTransaction(pendingTransfer))
          } yield txResult
        ).value
      }

      def isStockmindUser(destinationUserEmail: EmailHandle): P[Boolean] =
        findUserByIdentifier(destinationUserEmail).map(_.isDefined)

      isStockmindUser(request.destinationIdentifier).flatMap {
        if (_) {
          offchainTransfer(request)
        } else {
          pendingTransfer()
        }
      }
    }

    /*
     * Function that handles a transaction to another ethereum address
     */
    def ethereumAddressCase(address: Address): P[ValidatedTransaction[TransactionResult]] = {

      def transferRequestFromEthereumAddressRepr(
          addressRepr: String): ValidatedTransactionTransformer[TransferRequestUser721] =
        for {
          ethAcc  <- liftEthereumAccountForAddress(Address(addressRepr))
          userAcc <- liftAccountByUserId(ethAcc.user)
        } yield transferRequestFromEmailHandle(EmailHandle(userAcc.identifier))

      def translateIntoOffChainTransfer(
          addressRepr: String
      ): P[ValidatedTransaction[TransactionResult]] = {
        (for {
          transferRequest <- transferRequestFromEthereumAddressRepr(addressRepr)
          transferResult  <- EitherT(offchainTransfer(transferRequest))
        } yield transferResult).value
      }

      def doWithdraw(
          address: Address
      ): P[ValidatedTransaction[TransactionResult]] = {

        def withdrawAction(
            withdrawRequest: WithdrawRequest721
        ): P[ValidatedTransaction[OutboundTransfer]] = {

          lazy val parsedWithdrawRequest: ValidatedTransactionTransformer[WithdrawInfo721] =
            for {
              senderAccount <- liftAccountByUserId(withdrawRequest.sourceUserId)
              withdrawSourceInfo <- liftTransaction721SourceInfoIdentifier(
                withdrawRequest.sourceUserId,
                senderAccount.identifier,
                withdrawRequest.tokenId
              )
              sourceAddress      = withdrawRequest.masterAccountAddress
              destinationAddress = withdrawRequest.destinationEthereumAddress
            } yield WithdrawInfo721(withdrawSourceInfo, sourceAddress, destinationAddress)

          def liftSendWithdrawTx(
              signableTx: SignableTransaction
          ): ValidatedTransactionTransformer[String] =
            EitherT(sendWithdrawTx(signableTx).map(_.leftMap(ethereumIssue)))

          def liftWriteOffChainTransfer(
              transfer: OffChainTransfer
          ): ValidatedTransactionTransformer[OffChainTransfer] =
            EitherT(writeOffChainTransfer(transfer).map(_.asRight[TokenTransactionError]))

          def liftGenerateOffchainTransfer(
              wi: WithdrawInfo721,
              timestampFx: => DateTime
          ): ValidatedTransactionTransformer[OffChainTransfer] = {

            def generateOffChainTransfer(
                wi: WithdrawInfo721,
                timestampFx: => DateTime
            ): OffChainTransfer = {
              OffChainTransfer(
                tokenSymbol = wi.transactionInfo.token.symbol,
                erc_type = wi.transactionInfo.token.erc_type,
                from = wi.transactionInfo.account.address,
                to = wi.masterAddress,
                amount = Uint(value = 1),
                created = timestampFx,
                onchainTransferId = None,
                token_id = Some(wi.transactionInfo.tokenId)
              )
            }

            EitherT.fromEither(
              generateOffChainTransfer(wi, timestampFx).asRight[TokenTransactionError])
          }

          def liftGenerateWithdrawTx(
              wi: WithdrawInfo721
          ): ValidatedTransactionTransformer[SignableTransaction] = {

            def generateWithdrawTx(
                wi: WithdrawInfo721
            ): SignableTransaction = {

              val contract = NFTTokenContract(wi.transactionInfo.token)

              SignableTransaction(
                contract.transfer(
                  from = wi.masterAddress,
                  to = wi.destinationAddress,
                  id = Uint(value = wi.transactionInfo.tokenId)
                ),
                masterAccountPassword
              )

            }

            EitherT.fromEither(generateWithdrawTx(wi).asRight[TokenTransactionError])
          }

          def liftSaveOutboundTransferData(
              data: OutboundTransfer): ValidatedTransactionTransformer[OutboundTransfer] =
            EitherT(saveOutboundTransferData(data).map(_.asRight[TokenTransactionError]))

          def liftEthereumHashFromString(
              txHash: String): ValidatedTransactionTransformer[EthereumHash] =
            EitherT.fromOption(
              EthereumHash.decodePrefixedHexString(txHash),
              resultingEthereumTxHashNotValid(txHash)
            )

          (
            for {
              withdrawInfo           <- parsedWithdrawRequest
              signableTx             <- liftGenerateWithdrawTx(withdrawInfo)
              offchainTransfer       <- liftGenerateOffchainTransfer(withdrawInfo, timestampFx)
              offchainTransferResult <- liftWriteOffChainTransfer(offchainTransfer)
              withdrawResultTxHash   <- liftSendWithdrawTx(signableTx)
              resultingEthHash       <- liftEthereumHashFromString(withdrawResultTxHash)
              outboundTransfer = OutboundTransfer(resultingEthHash, offchainTransferResult.id)
              result <- liftSaveOutboundTransferData(outboundTransfer)
              _ <- EitherT.liftT[P, TokenTransactionError, Unit](
                storeTransactionMetaInfIfAny(offchainTransferResult.id, withdrawRequest.metaInf)
              )
            } yield result
          ).value
        }

        val withdrawResult: P[ValidatedTransaction[OutboundTransfer]] =
          withdrawAction(
            WithdrawRequest721(
              transactionRequest.sourceUserId,
              transactionRequest.masterAccountAddress,
              address,
              transactionRequest.id,
              transactionRequest.metaInf
            )
          )

        EitherT(withdrawResult).map(_ => transactionIsCompleted()).value
      }

      def isStockmindAddress(address: Address): P[Boolean] =
        findEthereumAccountByAddress(address).map(_.isDefined)

      /*
       * Possible use cases:
       *  - We get a twitter handler; it's just a plain call to processAndDoTransfer.
       *  - We get an ethereum address. Several possible scenarios:
       *    - The ethereum address is the master / omnibus: forbid the operation
       *    - The ethereum address belongs to a Stockmind user: handle it as an of-chain transfer calling doTransferWithEffects
       *    - Any other case, call processAndDoWithdraw
       */

      (for {
        stockmnindAdd <- isStockmindAddress(address)
        omnibusAdd    <- isOmnibusAccountAddress(address)
      } yield {
        if (omnibusAdd) triedWithdrawToInvalidAccount().asLeft[TransactionResult].pure[P]
        else if (stockmnindAdd) translateIntoOffChainTransfer(address.value)
        else doWithdraw(address)
      }).flatten
    }

    /*
     * Actual processTransfer logic from here:
     *  - Resolve which use case we are in.
     *  - Take the appropriate action
     */

    val transferDestination: Either[Address, EmailHandle] = transactionRequest.destination

    transferDestination match {
      case Left(address) =>
        ethereumAddressCase(address)

      case Right(emailHandle) =>
        inPlatformOffchainTransfer(transferRequestFromEmailHandle(emailHandle))
    }
  }

  /*
   // CODE THAT PROCESSES PENDING TRANSACTIONS WITH TWITTER
   */
  def settleTransfersYetPending(
      oauthProvider: String,
      destinationUserKey: String,
      timestampFx: => DateTime
  )(implicit ev: Monad[P], ev1: WithEitherRecoverable[P, TokenTransactionError, Unit])
    : P[List[ValidatedTransaction[Unit]]] = {

    lazy val pendingTransfers
      : P[List[ValidatedTransaction[(PendingTransfer, OffChainTransfer)]]] = {

      import cats.syntax.traverse._
      import cats.instances.list._

      def liftPendingToOffchainTransaction(pendingTransfer: PendingTransfer)
        : P[ValidatedTransaction[(PendingTransfer, OffChainTransfer)]] = {
        (
          for {
            token <- liftTokenBySymbolAndType(pendingTransfer.tokenSymbol, pendingTransfer.erc_type)
            destinationUser <- liftUserIdFromOAuthProviderAndLoginKey(oauthProvider,
                                                                      destinationUserKey)
            destinationTwitterAccount <- liftTwitterAccountByUserId(destinationUser)
            destinationEthAccount <- liftFindEthereumAccountForUserIdIdentifier(
              destinationUser,
              destinationTwitterAccount.screenName
            )
            sourceTwitterAccount <- liftTwitterAccountByUserId(pendingTransfer.fromUser)
            sourceEthAccount <- liftFindEthereumAccountForUserIdIdentifier(
              pendingTransfer.fromUser,
              sourceTwitterAccount.screenName
            )
          } yield
            (
              pendingTransfer,
              OffChainTransfer(
                tokenSymbol = token.symbol,
                erc_type = token.erc_type,
                from = sourceEthAccount.address,
                to = destinationEthAccount.address,
                amount = Uint(value = pendingTransfer.amount),
                created = timestampFx,
                token_id = pendingTransfer.token_id
              )
            )
        ).value
      }

      findPendingTransfersByDestination(LoginInfo(oauthProvider, destinationUserKey))
        .map { pendingTransfers =>
          pendingTransfers.map { pendingTransfer =>
            liftPendingToOffchainTransaction(pendingTransfer)
          }
        }
        .map(_.sequence)
        .flatten
    }

    import WithEitherRecoverable.Syntax._

    import cats.syntax.traverse._
    import cats.instances.either._
    import cats.instances.list._

    pendingTransfers
      .map {
        _.map {
          _.map {
            case (pendingTransfer, tokenTransfer) =>
              storePendingTransactionInRepository(pendingTransfer.id, tokenTransfer)
          }
        }
      }
      // Flatten the onion
      // TODO Do something with this please
      .map(_.map(_.map(_.throwableAsLeft[TokenTransactionError](exceptionInProcess))))
      .map(_.map(_.sequence))
      .map(_.sequence)
      .flatten
      .map(_.map(_.flatten))
  }

  /*
   // CODE THAT PROCESSES PENDING TRANSACTIONS WITH AUTH0 USER/PASS
   */
  def settleTransfersYetPendingUser(
      oauthProvider: String,
      destinationUserEmail: String,
      timestampFx: => DateTime
  )(implicit ev: Monad[P], ev1: WithEitherRecoverable[P, TokenTransactionError, Unit])
    : P[List[ValidatedTransaction[Unit]]] = {

    lazy val pendingTransfers
      : P[List[ValidatedTransaction[(PendingTransfer, OffChainTransfer)]]] = {

      import cats.syntax.traverse._
      import cats.instances.list._

      def liftPendingToOffchainTransaction(pendingTransfer: PendingTransfer)
        : P[ValidatedTransaction[(PendingTransfer, OffChainTransfer)]] = {
        (
          for {
            token <- liftTokenBySymbolAndType(pendingTransfer.tokenSymbol, pendingTransfer.erc_type)
            destinationUser <- liftUserIdFromOAuthProviderAndIdentifier(oauthProvider,
                                                                        destinationUserEmail)
            destinationAccount <- liftAccountByUserId(destinationUser)
            destinationEthAccount <- liftFindEthereumAccountForUserIdIdentifier(
              destinationUser,
              destinationAccount.identifier
            )
            sourceAccount <- liftAccountByUserId(pendingTransfer.fromUser)
            sourceEthAccount <- liftFindEthereumAccountForUserIdIdentifier(
              pendingTransfer.fromUser,
              sourceAccount.identifier
            )
          } yield
            (
              pendingTransfer,
              OffChainTransfer(
                tokenSymbol = token.symbol,
                erc_type = token.erc_type,
                from = sourceEthAccount.address,
                to = destinationEthAccount.address,
                amount = Uint(value = pendingTransfer.amount),
                created = timestampFx,
                token_id = pendingTransfer.token_id
              )
            )
        ).value
      }

      findPendingTransfersByDestination(LoginInfo(oauthProvider, destinationUserEmail))
        .map { pendingTransfers =>
          pendingTransfers.map { pendingTransfer =>
            liftPendingToOffchainTransaction(pendingTransfer)
          }
        }
        .map(_.sequence)
        .flatten
    }

    import WithEitherRecoverable.Syntax._

    import cats.syntax.traverse._
    import cats.instances.either._
    import cats.instances.list._

    pendingTransfers
      .map {
        _.map {
          _.map {
            case (pendingTransfer, tokenTransfer) =>
              storePendingTransactionInRepository(pendingTransfer.id, tokenTransfer)
          }
        }
      }
      // Flatten the onion
      // TODO Do something with this please
      .map(_.map(_.map(_.throwableAsLeft[TokenTransactionError](exceptionInProcess))))
      .map(_.map(_.sequence))
      .map(_.sequence)
      .flatten
      .map(_.map(_.flatten))
  }
}

private[transaction] object TransactionOps {

  // Entities associated to these effects
  case class TransferRequest(
      sourceUserId: UUID,
      destinationScreenName: TwitterHandle,
      tokenSymbol: String,
      erc_type: String,
      amount: String,
      metaInf: Option[Map[String, String]] = None
  )

  case class TransferRequest721(
      sourceUserId: UUID,
      destinationScreenName: TwitterHandle,
      tokenId: BigInt,
      metaInf: Option[Map[String, String]] = None
  )
  // Entities associated to these effects
  case class TransferRequestUser(
      sourceUserId: UUID,
      destinationIdentifier: EmailHandle,
      tokenSymbol: String,
      erc_type: String,
      amount: String,
      metaInf: Option[Map[String, String]] = None
  )

  case class TransferRequestUser721(
      sourceUserId: UUID,
      destinationIdentifier: EmailHandle,
      tokenId: BigInt,
      metaInf: Option[Map[String, String]] = None
  )

  case class WithdrawRequest(
      sourceUserId: UUID,
      masterAccountAddress: Address,
      destinationEthereumAddress: Address,
      tokenSymbol: String,
      erc_type: String,
      amount: String,
      metaInf: Option[Map[String, String]] = None
  )
  case class WithdrawRequest721(
      sourceUserId: UUID,
      masterAccountAddress: Address,
      destinationEthereumAddress: Address,
      tokenId: BigInt,
      metaInf: Option[Map[String, String]] = None
  )
  case class WithdrawInfo(
      transactionInfo: TransactionSourceInfo,
      masterAddress: Address,
      destinationAddress: Address
  )
  case class WithdrawInfo721(
      transactionInfo: TransactionSourceInfo721,
      masterAddress: Address,
      destinationAddress: Address
  )
}
