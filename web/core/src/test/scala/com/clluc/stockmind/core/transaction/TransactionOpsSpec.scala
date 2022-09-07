package com.clluc.stockmind.core.transaction

import com.clluc.stockmind.core.RawValueParser
import com.clluc.stockmind.core.transaction.TransactionOpsFixture.{StoreTransactionMetaInf, _}
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.{
  JsonRpcPlainResult,
  UnexpectedEthereumResponse
}
import com.clluc.stockmind.core.ethereum.{
  EthereumHash,
  HumanStandardTokenContract,
  SignableTransaction
}
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.transaction.Fixtures._
import com.clluc.stockmind.core.transaction.TokenTransactionError.{sourceUserHasNotEnoughBalance, _}
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterHandle}
import com.clluc.stockmind.core.user.Balance
import monocle.{Lens, Optional}
import monocle.macros.GenLens
import org.joda.time.DateTime
import org.scalatest.{FunSpec, Matchers}

class TransactionOpsSpec extends FunSpec with Matchers {

  private val amountInRequest = "1.0"
  private val translatedAmountIntoBigInt: BigInt =
    RawValueParser.parseIntoRawValue(amountInRequest, token.decimals).get

  private val amountIntoUint =
    Uint(value = translatedAmountIntoBigInt)

  private val timestamp = DateTime.now

  private val expectedMetaInf = Map(
    "key1" -> "value1",
    "key2" -> "value2"
  )

  object FixtureTransformations {

    // Removes the balance from the sender account
    val noBalanceInSenderAccountOptic: (TransactionOpsFixture) => TransactionOpsFixture = {
      val balanceLens = GenLens[TransactionOpsFixture](_.balanceForAccountToken)

      balanceLens.modify(_.filterNot {
        case (BalanceByEthAddressTokenKey(ethAddress, _), _) =>
          ethAddress == sourceEthAccountAddress
      })
    }

    // Sets a new balance in sender's account
    def customBalanceInSenderAccountOptic(
        targetBalance: BigInt
    ): (TransactionOpsFixture) => TransactionOpsFixture = {
      val balanceEntryLens = GenLens[TransactionOpsFixture](_.balanceForAccountToken)

      val balanceTupleOptional = {
        Optional[Map[BalanceByEthAddressTokenKey, Balance], (BalanceByEthAddressTokenKey, Balance)] {
          _.find {
            case (k, _) => k == BalanceByEthAddressTokenKey(sourceEthAccountAddress, tokenSymbol)
          }
        } { entry => map =>
          map + entry
        }
      }

      val balanceValueLens =
        Lens[(BalanceByEthAddressTokenKey, Balance), Balance] {
          case (_, balance) => balance
        }(newBalance => oldTuple => (oldTuple._1, newBalance))

      val resultingLens =
        balanceEntryLens.composeOptional(balanceTupleOptional).composeLens(balanceValueLens)

      resultingLens.modify(_.copy(effectiveBalance = targetBalance))
    }

    val destinationTwitterHandleNotAStockmindUserOptic
      : (TransactionOpsFixture) => TransactionOpsFixture = {
      val balanceEntryLens: Lens[TransactionOpsFixture, Map[UserId, TwitterAccount]] =
        GenLens[TransactionOpsFixture](_.twitterAccountByUserId)

      balanceEntryLens.modify { twitterAccountsMap =>
        twitterAccountsMap.filterNot {
          case (userId, _) => userId == destinationUserId
        }
      }
    }

    val destinationUserHasNoTwitterAccountOptic
      : (TransactionOpsFixture) => TransactionOpsFixture = {
      val mapLens = GenLens[TransactionOpsFixture](_.twitterIdForHandle)

      mapLens.modify(_.filterNot {
        case (twitterHandle, _) => twitterHandle == TwitterHandle(destinationUserTwitterScreenName)
      })
    }

    private def jsonRpcOptic(result: JsonRpcPlainResult) = {
      val txResultLens = GenLens[TransactionOpsFixture](_.sendWithdrawTxResult)

      txResultLens.modify(_ => result)
    }

    val jsonRpcCallSuccessfulOptic: (TransactionOpsFixture) => TransactionOpsFixture =
      jsonRpcOptic(Right(expectedTxHashInWithdrawals))

    val jsonRpcCallFailureOptic: (TransactionOpsFixture) => TransactionOpsFixture =
      jsonRpcOptic(
        Left(
          UnexpectedEthereumResponse(unexpectedEthResponseInWithdrawals,
                                     unexpectedEthResponseStatus)))

    def isOmnibusAccountAddressOptic(
        isIt: Boolean): (TransactionOpsFixture) => TransactionOpsFixture = {
      val lens = GenLens[TransactionOpsFixture](_._isOmnibusAccountAddress)

      lens.modify(_ => isIt)
    }

    val sourceUserNotInStockmindOptic: (TransactionOpsFixture) => TransactionOpsFixture = {
      val userForIdLens     = GenLens[TransactionOpsFixture](_.stockmindUserForId)
      val modifiedUserForId = userForIdLens.modify(_ - sourceUserId)

      val oauth1InfoLens     = GenLens[TransactionOpsFixture](_.oauth1InfoFromLogin)
      val modifiedOauth1Info = oauth1InfoLens.modify(_ - sourceUserLoginInfo)

      val userFromOauthLens     = GenLens[TransactionOpsFixture](_.userFromOauth)
      val modifiedUserFromOauth = userFromOauthLens.modify(_ - sourceUserLoginInfo)

      val twitterAccountLens     = GenLens[TransactionOpsFixture](_.twitterAccountByUserId)
      val modifiedTwitterAccount = twitterAccountLens.modify(_ - sourceUserId)

      val ethereumAccountLens     = GenLens[TransactionOpsFixture](_.ethereumAccountForUser)
      val modifiedEthereumAccount = ethereumAccountLens.modify(_ - sourceUserId)

      modifiedUserForId andThen modifiedOauth1Info andThen modifiedUserFromOauth andThen
        modifiedTwitterAccount andThen modifiedEthereumAccount
    }

    val tokenNotSupportedOptic: (TransactionOpsFixture) => TransactionOpsFixture = {
      val tokenLens = GenLens[TransactionOpsFixture](_.tokensErc20BySymbolAndType)

      tokenLens.modify(_ - tokenSymbolandType)
    }

    val sourceUserWithoutTwitterAccountOptic: (TransactionOpsFixture) => TransactionOpsFixture = {
      val twitterAccountLens = GenLens[TransactionOpsFixture](_.twitterAccountByUserId)

      twitterAccountLens.modify(_ - sourceUserId)
    }

    val sourceUserWithoutEthereumAccountOptic: (TransactionOpsFixture) => TransactionOpsFixture = {
      val ethAccLens = GenLens[TransactionOpsFixture](_.ethereumAccountForUser)

      ethAccLens.modify(_ - sourceUserId)
    }
  }

  private def processTxWithFixture(
      transactionRequest: TransactionRequest
  )(
      fixtureFx: TransactionOpsFixture => TransactionOpsFixture
  ): (Vector[Effect], ValidatedTransaction[TransactionResult]) = {
    val effectsFixture: TransactionOps[TestEffectsState] =
      fixtureFx(happyPathTokenTransactionFixture)

    effectsFixture
      .processTransaction(
        transactionRequest,
        twitterUserCredentials,
        timestamp,
        masterEthAccountPassword
      )
      .run(Vector.empty)
      .value
  }

  private val processTxWithTwitterHandlerFixture =
    processTxWithFixture(transactionRequestFixtureForTwitterHandle(amountInRequest))(_)

  private val processTxWithTwitterHandlerAndMetaInfFixture =
    processTxWithFixture(
      transactionRequestFixtureForTwitterHandleAndMetaInf(amountInRequest, expectedMetaInf))(_)

  private val processTxWithEthAddressInStockmindFixture =
    processTxWithFixture(transactionRequestFixtureForEthereumAddressInStockmind(amountInRequest))(_)

  private val processTxWithEthAddressNotInStockmindFixture =
    processTxWithFixture(
      transactionRequestFixtureForEthereumAddressNotInStockmind(amountInRequest))(_)

  private val processTxWithEthAddressInStockmindAndMetaInfFixture =
    processTxWithFixture(
      transactionRequestFixtureForEthereumAddressInStockmindAndMetaInf(amountInRequest,
                                                                       expectedMetaInf))(_)

  private val processTxWithEthAddressNotInStockmindAndMetaInfFixture =
    processTxWithFixture(
      transactionRequestFixtureForEthereumAddressNotInStockmindAndMetaInf(amountInRequest,
                                                                          expectedMetaInf)
    )(_)

  private val processTxWithMasterAccEthAddFixture =
    processTxWithFixture(transactionRequestFixtureForMasterAccountAddress(amountInRequest))(_)

  private def findFirstEffectOfType[T](effects: Seq[Effect], clazz: Class[T]): Effect =
    effects.find(_.getClass == clazz).get

  private def filterEffectsOfType[T](effects: Seq[Effect], clazz: Class[T]): Seq[Effect] =
    effects.filter(_.getClass == clazz)

  private def expectedOffChainTransfer(destination: Address) = OffChainTransfer(
    id = savedOffChainTransferId,
    tokenSymbol = tokenSymbol,
    erc_type = tokenType,
    from = sourceEthAccountAddress,
    to = destination,
    amount = amountIntoUint,
    created = timestamp
  )

  private val noSourceUserPotentialErrors: Set[ValidatedTransaction[TransactionResult]] = Set(
    Left(TransferSourceUserDoesNotExist(sourceUserId)),
    Left(NoTwitterAccountForStockmindUser(sourceUserId)),
    Left(UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
  )

  import FixtureTransformations._

  describe("When transacting with an ERC20 token") {
    describe("Given a transaction request to a twitter account") {
      describe("If the twitter account belongs to a Stockmind user") {
        describe("When the issuer of the transaction have no balance at all") {

          val fixtureCreationFx = noBalanceInSenderAccountOptic

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserNotInStockmindOptic
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors should contain(result)
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          val (effects, result) = processTxWithTwitterHandlerFixture(fixtureCreationFx)

          it("We get a Left result saying that the issuer of the transaction has no balance") {
            result shouldBe Left(SourceUserHasNoBalance)
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects shouldBe empty
          }
        }

        describe("When the issuer of the transaction doesn't have enough balance") {

          val actualUserBalance: BigInt = 0

          val fixtureCreationFx = customBalanceInSenderAccountOptic(actualUserBalance)

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithTwitterHandlerFixture(
              sourceUserNotInStockmindOptic andThen fixtureCreationFx
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors should contain(result)
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          val (effects, result) =
            processTxWithTwitterHandlerFixture(fixtureCreationFx)

          it(
            "We get a Left result saying that the issuer of the transaction has not enough balance") {
            result.left.get shouldBe a[SourceUserHasNotEnoughBalance]
          }

          it("We get the right information in the error instance regarding our current available balance and the requested one") {
            result shouldBe Left(
              sourceUserHasNotEnoughBalance(
                sourceEthAccountAddress.value,
                tokenSymbol,
                actualUserBalance,
                translatedAmountIntoBigInt
              )
            )
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects shouldBe empty
          }
        }

        describe("When the issuer of the transaction has enough balance") {
          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) =
              processTxWithTwitterHandlerFixture(sourceUserNotInStockmindOptic)

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithTwitterHandlerFixture(tokenNotSupportedOptic)

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) =
              processTxWithTwitterHandlerFixture(sourceUserWithoutTwitterAccountOptic)

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) =
              processTxWithTwitterHandlerFixture(sourceUserWithoutEthereumAccountOptic)

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          val (effects, result) = processTxWithTwitterHandlerFixture(identity)

          it("We get a result that states that the transaction has been completed") {
            result shouldBe Right(TransactionIsCompleted)
          }

          it("The effect of creating and off-chain transaction is invoked") {
            findFirstEffectOfType(effects, classOf[WriteOffChainTransfer]) shouldBe
              WriteOffChainTransfer(expectedOffChainTransfer(destinationEthAccountAddress))
          }

          it("The notification to the transaction recipient effect is invoked after the off-chain transaction is created") {
            findFirstEffectOfType(effects, classOf[NotifyStockmindRecipient]) shouldBe
              NotifyStockmindRecipient(expectedOffChainTransfer(destinationEthAccountAddress))
          }

          it("No other effect apart from the previous ones is invoked") {
            effects should contain theSameElementsAs Vector(
              WriteOffChainTransfer(expectedOffChainTransfer(destinationEthAccountAddress)),
              NotifyStockmindRecipient(expectedOffChainTransfer(destinationEthAccountAddress))
            )
          }

          it("The expected effects are called only once") {
            effects should have size 2
          }
        }

        describe(
          "When the issuer of the transaction has enough balance and also, if we have added meta inf " +
            "to the transaction request") {

          val (effects, result) =
            processTxWithTwitterHandlerAndMetaInfFixture(identity)

          it("We get a result that states that the transaction has been completed") {
            result shouldBe Right(TransactionIsCompleted)
          }

          it("The effect of creating and off-chain transaction is invoked") {
            findFirstEffectOfType(effects, classOf[WriteOffChainTransfer]) shouldBe
              WriteOffChainTransfer(expectedOffChainTransfer(destinationEthAccountAddress))
          }

          it("The notification to the transaction recipient effect is invoked after the off-chain transaction is created") {
            findFirstEffectOfType(effects, classOf[NotifyStockmindRecipient]) shouldBe
              NotifyStockmindRecipient(expectedOffChainTransfer(destinationEthAccountAddress))
          }

          it("The effect for persisting that meta inf is invoked") {
            findFirstEffectOfType(effects, classOf[StoreTransactionMetaInf]) shouldBe
              StoreTransactionMetaInf(expectedMetaInf)
          }

          it("Among the other 2 previously asserted") {
            effects should contain theSameElementsAs Vector(
              WriteOffChainTransfer(expectedOffChainTransfer(destinationEthAccountAddress)),
              NotifyStockmindRecipient(expectedOffChainTransfer(destinationEthAccountAddress)),
              StoreTransactionMetaInf(expectedMetaInf)
            )
          }

          it("All those effects are called only once") {
            effects should have size 3
          }
        }
      }

      describe("If the twitter account doesn't belong to a Stockmind user") {

        val savePendingTransactionExpectedEffect = SavePendingTransaction(
          PendingTransfer(
            fromUser = sourceUserId,
            toFutureUser = destinationUserLoginInfo,
            tokenSymbol = tokenSymbol,
            erc_type = tokenType,
            amount = translatedAmountIntoBigInt,
            created = timestamp,
            processed = None
          )
        )

        val notifyPendingTransferExpectedEffect = NotifyPendingTransfer(
          sourceEthAccountAddress
        )

        describe("If the issuer of the transaction has no balance at all") {

          val fixtureCreationFx = destinationTwitterHandleNotAStockmindUserOptic andThen
            noBalanceInSenderAccountOptic

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithTwitterHandlerFixture(
              sourceUserNotInStockmindOptic andThen fixtureCreationFx
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          val (effects, result) = processTxWithTwitterHandlerFixture(fixtureCreationFx)

          it("We get a Left result saying that the issuer of the transaction has no balance") {
            result shouldBe Left(sourceUserHasNoBalance())
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects.isEmpty shouldBe true
          }
        }

        describe("If the issuer of the transaction has no enough balance") {
          val sourceAccountBalance = 1

          val fixtureCreationFx = destinationTwitterHandleNotAStockmindUserOptic andThen
            customBalanceInSenderAccountOptic(sourceAccountBalance)

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithTwitterHandlerFixture(
              sourceUserNotInStockmindOptic andThen fixtureCreationFx
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          val (effects, result) = processTxWithTwitterHandlerFixture(fixtureCreationFx)

          it(
            "We get a Left result saying that the issuer of the transaction has not enough balance") {
            result.left.get.isInstanceOf[SourceUserHasNotEnoughBalance] shouldBe true
          }

          it("We get the right information in the error instance regarding our current available balance and the requested one") {
            result shouldBe Left(
              sourceUserHasNotEnoughBalance(
                sourceEthAccountAddress.value,
                tokenSymbol,
                sourceAccountBalance,
                translatedAmountIntoBigInt
              )
            )
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects.isEmpty shouldBe true // TODO Note for reviewer: do we prefer that or assert(result.isEmpty)?
          }
        }

        describe("If the issuer of the transaction has enough balance") {

          describe("When the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithTwitterHandlerFixture(
              sourceUserNotInStockmindOptic andThen destinationTwitterHandleNotAStockmindUserOptic
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              destinationTwitterHandleNotAStockmindUserOptic andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              destinationTwitterHandleNotAStockmindUserOptic andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithTwitterHandlerFixture(
              destinationTwitterHandleNotAStockmindUserOptic andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          val (effects, result) = processTxWithTwitterHandlerFixture(
            destinationTwitterHandleNotAStockmindUserOptic
          )

          it("the result is a TransactionIsPending") {
            result shouldBe Right(TransactionIsPending)
          }

          it("The create pending transaction effect is called once") {
            filterEffectsOfType(effects, classOf[SavePendingTransaction]) should have size 1
          }

          it("The create pending transaction effect is called with appropriate parameters") {
            findFirstEffectOfType(effects, classOf[SavePendingTransaction]) shouldBe savePendingTransactionExpectedEffect
          }

          it("The transfer issuer is notified that the transaction has been stored in order to update it's UI") {
            findFirstEffectOfType(effects, classOf[NotifyPendingTransfer]) shouldBe notifyPendingTransferExpectedEffect
          }

          it("No other effects apart from the previous ones are invoked") {
            effects should contain theSameElementsAs Vector(
              savePendingTransactionExpectedEffect,
              notifyPendingTransferExpectedEffect
            )
          }
        }

        describe(
          "If the issuer of the transaction has enough balance and also some meta inf is included in the transaction request") {

          val (effects, result) = processTxWithTwitterHandlerAndMetaInfFixture(
            destinationTwitterHandleNotAStockmindUserOptic
          )

          it("the result is a MetaInfoNotAllowedInPendingTransfers error") {
            result shouldBe Left(MetaInfoNotAllowedInPendingTransfers)
          }

          it("No effect is called") {
            effects shouldBe empty
          }
        }
      }

      describe(
        "If the transaction destination twitter handle doesn't exist (the destination user does not have a twitter account)") {

        val fixtureCreationFx = destinationTwitterHandleNotAStockmindUserOptic andThen
          destinationUserHasNoTwitterAccountOptic

        describe(
          "When for some reason the source user doesn't exist as such in the Stockmind platform") {

          val (effects, result) = processTxWithTwitterHandlerFixture(
            fixtureCreationFx andThen sourceUserNotInStockmindOptic
          )

          it("We get a left instance indicating that") {
            val localPotentialErrors = noSourceUserPotentialErrors + Left(
              DestinationUserHasNoTwitterAccount(TwitterHandle(destinationUserTwitterScreenName))
            )

            localPotentialErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        describe("When the token for the transfer request is not supported by Stockmind") {
          val (effects, result) = processTxWithTwitterHandlerFixture(
            fixtureCreationFx andThen tokenNotSupportedOptic
          )

          it("We get a Left instance indicating it") {
            val localPotentialErrors = Set(
              Left(DestinationUserHasNoTwitterAccount(
                TwitterHandle(destinationUserTwitterScreenName))),
              Left(TokenForTransferNotInPlatform(tokenSymbol))
            )

            localPotentialErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        describe("When the source user has no twitter account in the system") {
          val (effects, result) = processTxWithTwitterHandlerFixture(
            fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
          )

          it("We get a Left instance indicating it") {
            // Here the destination user has no twitter account error prevail
            // We fail fast on the first one we encounter; which one? arbitrary ...
            result shouldBe Left(
              DestinationUserHasNoTwitterAccount(TwitterHandle(destinationUserTwitterScreenName)))
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        describe("When the source user has no ethereum account in the system") {
          val (effects, result) = processTxWithTwitterHandlerFixture(
            fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
          )

          it("We get a Left instance indicating it") {
            val possibleErrors: Set[ValidatedTransaction[TransactionResult]] = Set(
              Left(DestinationUserHasNoTwitterAccount(
                TwitterHandle(destinationUserTwitterScreenName))),
              Left(UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            )

            possibleErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        val (effects, result) = processTxWithTwitterHandlerFixture(fixtureCreationFx)

        it("We get the corresponding error as a result") {
          result shouldBe Left(
            DestinationUserHasNoTwitterAccount(
              TwitterHandle(destinationUserTwitterScreenName)
            ))
        }

        it("No effect are called") {
          effects shouldBe empty
        }
      }
    }

    describe("Given a transaction request to an ethereum address") {
      describe("When the destination address is managed by a Stockmind account") {
        describe("And the issuer of the transaction has no balance at all") {

          val fixtureCreationFx = noBalanceInSenderAccountOptic

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen sourceUserNotInStockmindOptic
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors should contain(result)
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          val (effects, result) = processTxWithEthAddressInStockmindFixture(fixtureCreationFx)

          it("We get a Left result saying that the issuer of the transaction has no balance") {
            result shouldBe Left(SourceUserHasNoBalance)
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects shouldBe empty
          }
        }

        describe("And the issuer of the transaction has not enough balance") {

          val actualUserBalance: BigInt = 0

          val fixtureCreationFx = customBalanceInSenderAccountOptic(actualUserBalance)

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen sourceUserNotInStockmindOptic
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithEthAddressInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          val (effects, result) =
            processTxWithEthAddressInStockmindFixture(fixtureCreationFx)

          it(
            "We get a Left result saying that the issuer of the transaction has not enough balance") {
            result.left.get.isInstanceOf[SourceUserHasNotEnoughBalance]
          }

          it("We get the right information in the error instance regarding our current available balance and the requested one") {
            result shouldBe Left(
              sourceUserHasNotEnoughBalance(
                sourceEthAccountAddress.value,
                tokenSymbol,
                actualUserBalance,
                translatedAmountIntoBigInt
              )
            )
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects.isEmpty shouldBe true
          }
        }

        val _expectedOffChainTransfer = expectedOffChainTransfer(destinationEthAccountAddress)

        describe("And the issuer of the transaction has enough balance") {

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {
            val (effects, result) =
              processTxWithEthAddressInStockmindFixture(sourceUserNotInStockmindOptic)

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors should contain(result)
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) =
              processTxWithEthAddressInStockmindFixture(tokenNotSupportedOptic)

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) =
              processTxWithEthAddressInStockmindFixture(sourceUserWithoutTwitterAccountOptic)

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) =
              processTxWithEthAddressInStockmindFixture(sourceUserWithoutEthereumAccountOptic)

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          val (effects, result) = processTxWithEthAddressInStockmindFixture(identity)

          it("We get a result that states that the transaction has been completed") {
            result shouldBe Right(TransactionIsCompleted)
          }

          it("The effect of creating and off-chain transaction is invoked") {
            effects should contain(WriteOffChainTransfer(_expectedOffChainTransfer))
          }

          it("The notification to the transaction recipient effect is invoked after the off-chain transaction is created") {
            effects.indexOf(WriteOffChainTransfer(_expectedOffChainTransfer)) <
              effects.indexOf(NotifyStockmindRecipient(_expectedOffChainTransfer))
          }

          it("No other effect apart from the previous ones is invoked") {
            effects should contain theSameElementsAs Set(
              WriteOffChainTransfer(_expectedOffChainTransfer),
              NotifyStockmindRecipient(_expectedOffChainTransfer)
            )
          }

          it("The expected effects are called only once") {
            effects should have size 2
          }
        }

        describe(
          "The issuer of the transaction has enough balance and also some meta inf has been included in the request") {

          val (effects, result) = processTxWithEthAddressInStockmindAndMetaInfFixture(identity)

          val expectedStoreMetaInfEffect = StoreTransactionMetaInf(expectedMetaInf)

          it("We get a result that states that the transaction has been completed") {
            result shouldBe Right(TransactionIsCompleted)
          }

          it("The effect of creating and off-chain transaction is invoked") {
            effects should contain(WriteOffChainTransfer(_expectedOffChainTransfer))
          }

          it("The notification to the transaction recipient effect is invoked after the off-chain transaction is created") {
            effects.indexOf(WriteOffChainTransfer(_expectedOffChainTransfer)) <
              effects.indexOf(NotifyStockmindRecipient(_expectedOffChainTransfer))
          }

          it("The persist meta inf effect is invoked") {
            effects should contain(expectedStoreMetaInfEffect)
          }

          it("No other effect apart from the previous ones is invoked") {
            effects should contain theSameElementsAs Set(
              WriteOffChainTransfer(_expectedOffChainTransfer),
              NotifyStockmindRecipient(_expectedOffChainTransfer),
              expectedStoreMetaInfEffect
            )
          }

          it("The expected effects are called only once") {
            effects should have size 3
          }
        }
      }

      describe("When the destination address is not managed by a Stockmind account") {
        describe("And the issuer of the transaction has no balance at all") {

          val fixtureCreationFx = noBalanceInSenderAccountOptic

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen sourceUserNotInStockmindOptic
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects shouldBe empty
            }
          }

          val (effects, result) = processTxWithEthAddressNotInStockmindFixture(fixtureCreationFx)

          it("We get a Left result saying that the issuer of the transaction has no balance") {
            result shouldBe Left(SourceUserHasNoBalance)
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects shouldBe empty
          }
        }

        describe("And the issuer of the transaction has not enough balance") {

          val actualUserBalance: BigInt = 0

          val fixtureCreationFx = customBalanceInSenderAccountOptic(actualUserBalance)

          describe(
            "When for some reason the source user doesn't exist as such in the Stockmind platform") {

            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen sourceUserNotInStockmindOptic
            )

            it("We get a left instance indicating that") {
              noSourceUserPotentialErrors.contains(result) shouldBe true
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the token for the transfer request is not supported by Stockmind") {
            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen tokenNotSupportedOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no twitter account in the system") {
            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          describe("When the source user has no ethereum account in the system") {
            val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
              fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
            )

            it("We get a Left instance indicating it") {
              result shouldBe Left(
                UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
            }

            it("No effects take place") {
              effects.isEmpty shouldBe true
            }
          }

          val (effects, result) =
            processTxWithEthAddressNotInStockmindFixture(fixtureCreationFx)

          it(
            "We get a Left result saying that the issuer of the transaction has not enough balance") {
            result.left.get.isInstanceOf[SourceUserHasNotEnoughBalance] shouldBe true
          }

          it("We get the right information in the error instance regarding our current available balance and the requested one") {
            result shouldBe Left(
              sourceUserHasNotEnoughBalance(
                sourceEthAccountAddress.value,
                tokenSymbol,
                actualUserBalance,
                translatedAmountIntoBigInt
              )
            )
          }

          it("No other effects are called (neither transaction nor notification occur)") {
            effects.isEmpty shouldBe true
          }
        }

        val _expectedOffChainTransfer = expectedOffChainTransfer(masterAccountAddress)

        val expectedEthTx = HumanStandardTokenContract(ethtoken).transfer(
          masterAccountAddress,
          externalEthAddress,
          amountIntoUint
        )

        val expectedSendWithdrawTx = SendWithdrawTx(
          SignableTransaction(
            expectedEthTx,
            masterEthAccountPassword
          )
        )

        describe("And the issuer of the transaction has enough balance") {
          describe("If the on-chain call goes smooth") {
            describe(
              "When for some reason the source user doesn't exist as such in the Stockmind platform") {

              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                jsonRpcCallSuccessfulOptic andThen sourceUserNotInStockmindOptic
              )

              it("We get a left instance indicating that") {
                noSourceUserPotentialErrors should contain(result)
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            describe("When the token for the transfer request is not supported by Stockmind") {
              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                jsonRpcCallSuccessfulOptic andThen tokenNotSupportedOptic
              )

              it("We get a Left instance indicating it") {
                result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            describe("When the source user has no twitter account in the system") {
              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                jsonRpcCallSuccessfulOptic andThen sourceUserWithoutTwitterAccountOptic
              )

              it("We get a Left instance indicating it") {
                result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            describe("When the source user has no ethereum account in the system") {
              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                jsonRpcCallSuccessfulOptic andThen sourceUserWithoutEthereumAccountOptic
              )

              it("We get a Left instance indicating it") {
                result shouldBe Left(
                  UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            val (effects, result) =
              processTxWithEthAddressNotInStockmindFixture(jsonRpcCallSuccessfulOptic)

            it("The result of the operation is a transaction is completed") {
              result shouldBe Right(TransactionIsCompleted)
            }

            it("An off chain transfer effect from the source user's eth address to master should be invoked") {
              // TODO Replace this with should contain
              val effect = findFirstEffectOfType(effects, classOf[WriteOffChainTransfer])

              effect shouldBe WriteOffChainTransfer(_expectedOffChainTransfer)
            }

            it("The send withdrawal effect should have the expected data") {
              val effect = findFirstEffectOfType(effects, classOf[SendWithdrawTx])

              effect shouldBe expectedSendWithdrawTx
            }

            it(
              "A save outbound transfer data effect should be called with the expected information") {
              val effect = findFirstEffectOfType(effects, classOf[SaveOutboundTransferData])

              effect shouldBe SaveOutboundTransferData(
                OutboundTransfer(
                  EthereumHash.decodePrefixedHexString(expectedTxHashInWithdrawals).get,
                  savedOffChainTransferId
                )
              )
            }

            it("No more effects apart from the previous three should be invoked") {
              effects should have size 3
            }
          }

          describe("If the on-chain call give us an error result") {

            val fixtureCreationFx = jsonRpcCallFailureOptic

            describe(
              "When for some reason the source user doesn't exist as such in the Stockmind platform") {

              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                fixtureCreationFx andThen sourceUserNotInStockmindOptic
              )

              it("We get a left instance indicating that") {
                noSourceUserPotentialErrors should contain(result)
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            describe("When the token for the transfer request is not supported by Stockmind") {
              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                fixtureCreationFx andThen tokenNotSupportedOptic
              )

              it("We get a Left instance indicating it") {
                result shouldBe Left(TokenForTransferNotInPlatform(tokenSymbol))
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            describe("When the source user has no twitter account in the system") {
              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
              )

              it("We get a Left instance indicating it") {
                result shouldBe Left(NoTwitterAccountForStockmindUser(sourceUserId))
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            describe("When the source user has no ethereum account in the system") {
              val (effects, result) = processTxWithEthAddressNotInStockmindFixture(
                fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
              )

              it("We get a Left instance indicating it") {
                result shouldBe Left(
                  UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName))
              }

              it("No effects take place") {
                effects shouldBe empty
              }
            }

            val (effects, result) =
              processTxWithEthAddressNotInStockmindFixture(fixtureCreationFx)

            it("A result indicating that the transaction is failed is returned") {
              result shouldBe Left(
                EthereumIssue(
                  UnexpectedEthereumResponse(
                    unexpectedEthResponseInWithdrawals,
                    unexpectedEthResponseStatus
                  )
                )
              )
            }

            it("The off-chain action is rolled back") {
              // TODO This is a new feature not yet implemented
              pending
            }

            it("An alert effect of the fact that the process has been aborted is invoked") {
              // TODO This is also a new feature not yet implemented
              pending
            }

            it("The expected so far effects occur") {
              effects shouldBe Vector(
                WriteOffChainTransfer(expectedOffChainTransfer(masterAccountAddress)),
                expectedSendWithdrawTx
              )
            }

            it("No other effect apart from those is invoked") {
              effects should have size 2
            }
          }
        }

        describe(
          "The issuer of the transaction has enough balance, the onchain call goes smooth and some tx meta inf comes in the request") {

          val (effects, result) =
            processTxWithEthAddressNotInStockmindAndMetaInfFixture(jsonRpcCallSuccessfulOptic)

          it("The result of the operation is a transaction is completed") {
            result shouldBe Right(TransactionIsCompleted)
          }

          it("An off chain transfer effect from the source user's eth address to master should be invoked") {
            effects should contain(WriteOffChainTransfer(_expectedOffChainTransfer))
          }

          it("The send withdrawal effect should have the expected data") {
            effects should contain(expectedSendWithdrawTx)
          }

          it("A save outbound transfer data effect should be called with the expected information") {
            effects should contain(
              SaveOutboundTransferData(
                OutboundTransfer(
                  EthereumHash.decodePrefixedHexString(expectedTxHashInWithdrawals).get,
                  savedOffChainTransferId
                )
              ))
          }

          it("A save meta inf effect should be called with the expected info") {
            effects should contain(
              StoreTransactionMetaInf(expectedMetaInf)
            )
          }

          it("No more effects apart from the previous three should be invoked") {
            effects should have size 4
          }
        }
      }

      describe("When the destination address happens to be the master / omnibus account address") {

        val fixtureCreationFx = isOmnibusAccountAddressOptic(true)

        val basePotentialErrors: Set[ValidatedTransaction[TransactionResult]] =
          Set(Left(TriedWithdrawToInvalidAccount()))

        describe(
          "When for some reason the source user doesn't exist as such in the Stockmind platform") {

          val (effects, result) = processTxWithMasterAccEthAddFixture(
            fixtureCreationFx andThen sourceUserNotInStockmindOptic
          )

          it("We get a left instance indicating that") {
            val localPotentialErrors = noSourceUserPotentialErrors + Left(
              TriedWithdrawToInvalidAccount())

            localPotentialErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        describe("When the token for the transfer request is not supported by Stockmind!") {
          val (effects, result) = processTxWithMasterAccEthAddFixture(
            fixtureCreationFx andThen tokenNotSupportedOptic
          )

          it("We get a Left instance indicating it") {
            val localPotentialErrors = basePotentialErrors + Left(
              TokenForTransferNotInPlatform(tokenSymbol))

            localPotentialErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        describe("When the source user has no twitter account in the system") {
          val (effects, result) = processTxWithMasterAccEthAddFixture(
            fixtureCreationFx andThen sourceUserWithoutTwitterAccountOptic
          )

          it("We get a Left instance indicating it") {
            val localPotentialErrors = basePotentialErrors + Left(
              NoTwitterAccountForStockmindUser(sourceUserId))
            localPotentialErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        describe("When the source user has no ethereum account in the system") {
          val (effects, result) = processTxWithMasterAccEthAddFixture(
            fixtureCreationFx andThen sourceUserWithoutEthereumAccountOptic
          )

          it("We get a Left instance indicating it") {
            val localPotentialErrors = basePotentialErrors + Left(
              UserDoesNotHaveEthAccountInPlatform(sourceUserId, sourceUserTwitterScreenName)
            )

            localPotentialErrors should contain(result)
          }

          it("No effects take place") {
            effects shouldBe empty
          }
        }

        val (effects, result) = processTxWithMasterAccEthAddFixture(fixtureCreationFx)

        it("We forbid that operation, and return an error indicating so") {
          result shouldBe Left(TriedWithdrawToInvalidAccount())
        }

        it("No effects take place") {
          effects shouldBe empty
        }
      }
    }
  }

  describe("When transacting with ether") {
    // TODO Describe what should happen here; probably good to pair program on this and drive implementation from this specification
    pending
  }
}
