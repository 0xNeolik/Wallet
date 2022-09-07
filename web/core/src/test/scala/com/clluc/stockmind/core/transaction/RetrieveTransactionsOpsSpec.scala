package com.clluc.stockmind.core.transaction

import java.util.UUID

import com.clluc.stockmind.core.ethereum._
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction.RetrieveTransactionsOpsSpec.RetrieveTxOpsForTest
import com.clluc.stockmind.core.transaction.RetrieveTransactionsOpsSpecFixtures._
import TestFirstPersonData._
import cats.Id
import com.clluc.stockmind.core.transaction.StockmindTransaction.ValidatedTxRetrievalResult
import com.clluc.stockmind.core.twitter.{TwitterAccount, TwitterUserInfo}
import com.clluc.stockmind.core.user.User
import org.scalatest.{EitherValues, FunSpec, Matchers}
import monocle.macros.GenLens

class RetrieveTransactionsOpsSpec extends FunSpec with Matchers with EitherValues {

  // Auxiliary method to allow code reuse
  private[transaction] def transactionsPage(
      ops: RetrieveTransactionsOps[cats.Id])(offset: Int, limit: Int, erc_type: String) =
    ops.transactionsPage(userForTestId, offset, limit, erc_type)

  describe("Happy path") {

    // We are going to use a compositional approach to define the fixture of each test case
    // This is a common base. All fixture must have this in the happy path to work
    // We will build specific derived fixtures in a functional style using Lens
    // That is; defining additions / modifications individually and composing them via function composition
    val happyPathBaseOps = RetrieveTxOpsForTest(
      ethereumAccountByUser = Map(
        userForTestId -> userForTestEthAcc
      ),
      twitterAccountsForUser = Map(
        userForTestId -> userForTestTwitterAcc
      ),
      ethereumAccountsByAddress = Map(
        userForTestEthAdd -> userForTestEthAcc
      ),
      usersInfoFromId = Map(
        userForTestId -> userForTestInfo
      )
    )

    describe("For individual use cases; to ease debugging and locating failures") {

      // Fix the offset and limit for those test cases where those parameter are not taken into account
      val first10ElementsOps: RetrieveTransactionsOps[Id] => Id[
        ValidatedTxRetrievalResult[List[StockmindTransaction]]] =
        transactionsPage(_)(0, 10, "ERC-20")

      it("An onchain incoming") {
        // Set up use case fixture using lenses
        val offchainTransfersByAddressLens =
          GenLens[RetrieveTxOpsForTest](_.offchainTransfersByAddress).set(
            Map(
              (userForTestEthAdd, OnchainIncomingTxData.onchainIncomingTxTokenType) -> List(
                OnchainIncomingTxData.onchain20IncomingTx
              )
            )
          )

        val ethereumTokenBySymbolLens =
          GenLens[RetrieveTxOpsForTest](_.erc20TokensBySymbol)
            .set(OnchainIncomingTxData.onchainIncomingTxEthTokenBySymbolMap2)

        val onchainTxsFromIdLens =
          GenLens[RetrieveTxOpsForTest](_.onchainTxsFromId)
            .modify(_ + (OnchainIncomingTxData.onchainId -> OnchainIncomingTxData.onchain20Tx))

        val ops =
          (offchainTransfersByAddressLens andThen ethereumTokenBySymbolLens andThen onchainTxsFromIdLens)(
            happyPathBaseOps)

        // Test use case
        val result = first10ElementsOps(ops)

        result.right.value shouldBe List(OnchainIncomingTxData.onchainIncoming20StockmindTx)
      }

      it("A pending tx") {
        // Set up use case fixture using lenses
        val ethereumTokensBySymbolLens =
          GenLens[RetrieveTxOpsForTest](_.erc20TokensBySymbol)
            .modify(_ ++ PendingTxsData.erc20TokenBySymbolMap)

        val twitterUsersInfoByTwitterIdLens =
          GenLens[RetrieveTxOpsForTest](_.twitterUsersInfoByTwitterId)
            .modify(_ ++ PendingTxsData.twitterUserInfoByTwitterIdMapForPending1)

        val pendingTransfersByIssuerLens =
          GenLens[RetrieveTxOpsForTest](_.pendingTransfersByIssuer)
            .modify(
              _ ++ Map(
                (userForTestId, PendingTxsData.pendingTokenType) -> List(
                  PendingTxsData.pendingTx1
                )
              ))

        val ops =
          (ethereumTokensBySymbolLens andThen twitterUsersInfoByTwitterIdLens andThen pendingTransfersByIssuerLens)(
            happyPathBaseOps)

        // Test use case
        val result = first10ElementsOps(ops)

        result.right.value shouldBe List(PendingTxsData.pendingTx1StockmindTx)
      }

      it("An offchain outgoing") {
        // Set up use case fixture using lenses
        val ethereumAccountByUserLens =
          GenLens[RetrieveTxOpsForTest](_.ethereumAccountByUser)
            .modify(_ ++ OffchainOutgoingTxData.offchainOutgoingTxEthAccountForCounterpartUserMap)

        val offchainTransfersByAddressLens =
          GenLens[RetrieveTxOpsForTest](_.offchainTransfersByAddress)
            .set(
              Map((userForTestEthAdd, OffchainOutgoingTxData.offchainOutgoingTxToken20Type) -> List(
                OffchainOutgoingTxData.offchain20OutgoingTx)))

        val ethereumTokensBySymbolLens =
          GenLens[RetrieveTxOpsForTest](_.erc20TokensBySymbol)
            .modify(_ ++ OffchainOutgoingTxData.offchainOutgoingExErc20TokenBySymbolMap)

        val twitterAccountsForUserLens =
          GenLens[RetrieveTxOpsForTest](_.twitterAccountsForUser)
            .modify(_ ++ OffchainOutgoingTxData.offchainOutgoingTxTwitterAccForCounterpartUserMap)

        val ethereumAccountsByAddressLens =
          GenLens[RetrieveTxOpsForTest](_.ethereumAccountsByAddress)
            .modify(_ ++ OffchainOutgoingTxData.ethereumAccountForAddressesMap)

        val usersInfoFromIdLens =
          GenLens[RetrieveTxOpsForTest](_.usersInfoFromId)
            .modify(_ ++ OffchainOutgoingTxData.offchainOutgoingTxCounterpartUserInfoMap)

        val ops = (ethereumAccountByUserLens
          andThen offchainTransfersByAddressLens
          andThen ethereumTokensBySymbolLens
          andThen twitterAccountsForUserLens
          andThen ethereumAccountsByAddressLens
          andThen usersInfoFromIdLens)(happyPathBaseOps)

        // Test use case
        val result = first10ElementsOps(ops)

        result.right.value shouldBe List(OffchainOutgoingTxData.offchain20OutgoingStockmindTx)
      }

      it("An onchain outgoing") {
        // Set up use case fixture using lenses
        val offchainTransfersByAddressLens =
          GenLens[RetrieveTxOpsForTest](_.offchainTransfersByAddress)
            .set(Map((userForTestEthAdd, OnchainOutgoingTxData.onchainOutgoingTxTokenType) -> List(
              OnchainOutgoingTxData.onchainOutgoingTx)))

        val ethereumTokensBySymbolLens =
          GenLens[RetrieveTxOpsForTest](_.erc20TokensBySymbol)
            .modify(_ ++ OnchainOutgoingTxData.onchainOutgoingTxErc20TokenBySymbolMap)

        val onchainTxsFromIdLens =
          GenLens[RetrieveTxOpsForTest](_.onchainTxsFromId)
            .modify(_ + (OnchainOutgoingTxData.onchainId -> OnchainOutgoingTxData.onchainTx))

        val ops = (offchainTransfersByAddressLens
          andThen ethereumTokensBySymbolLens
          andThen onchainTxsFromIdLens)(happyPathBaseOps)

        // Test use case
        val result = first10ElementsOps(ops)

        result.right.value shouldBe List(OnchainOutgoingTxData.onchainOutgoingStockmindTx)
      }

      it("An offchain incoming") {
        // Set up use case fixture using lenses
        val ethereumAccountByUserLens =
          GenLens[RetrieveTxOpsForTest](_.ethereumAccountByUser)
            .modify(_ ++ OffchainIncomingTxData.offchainIncomingTxEthAccForCounterpartUserMap)

        val offchainTransfersByAddressLens =
          GenLens[RetrieveTxOpsForTest](_.offchainTransfersByAddress)
            .set(
              Map((userForTestEthAdd, OffchainIncomingTxData.offchainIncomingTx20TokenType) -> List(
                OffchainIncomingTxData.offchain20IncomingTx)))

        val ethereumErc20TokensBySymbolLens =
          GenLens[RetrieveTxOpsForTest](_.erc20TokensBySymbol)
            .modify(_ ++ OffchainIncomingTxData.offchainIncomingTxEthErc20TokenBySymbolMap)

        val twitterAccountsForUserLens =
          GenLens[RetrieveTxOpsForTest](_.twitterAccountsForUser)
            .modify(_ ++ OffchainIncomingTxData.offchainIncomingTxTwitterAccForCounterpartUserMap)

        val ethereumAccountsByAddressLens =
          GenLens[RetrieveTxOpsForTest](_.ethereumAccountsByAddress)
            .modify(_ ++ OffchainIncomingTxData.ethereumAccountForAddressesMap)

        val usersInfoFromIdLens =
          GenLens[RetrieveTxOpsForTest](_.usersInfoFromId)
            .modify(_ ++ OffchainIncomingTxData.offchainIncomingTxCounterpartUserInfoMap)

        val ops = (ethereumAccountByUserLens
          andThen offchainTransfersByAddressLens
          andThen ethereumErc20TokensBySymbolLens
          andThen twitterAccountsForUserLens
          andThen ethereumAccountsByAddressLens
          andThen usersInfoFromIdLens)(happyPathBaseOps)

        // Test use case
        val result = first10ElementsOps(ops)

        result.right.value shouldBe List(OffchainIncomingTxData.offchain20IncomingStockmindTx)
      }
    }
    describe("For complex use cases that involve the behaviour of pagination parameters") {
      describe("Given a user that mix 4 completed transactions " +
        "(2 off-chain , 1 incoming and 1 outgoing; and 2 on-chain; " +
        "1 incoming and 1 withdrawal) and 2 pending transactions in a given chronological order") {

        val ops = RetrieveTxOpsForTest(
          ethereumAccountByUser = Map(
            userForTestId -> userForTestEthAcc
          ) ++ OffchainIncomingTxData.offchainIncomingTxEthAccForCounterpartUserMap
            ++ OffchainOutgoingTxData.offchainOutgoingTxEthAccountForCounterpartUserMap,
          offchainTransfersByAddress = Map(
            (userForTestEthAdd, "ERC-20") -> List(
              OffchainIncomingTxData.offchain20IncomingTx,
              OffchainOutgoingTxData.offchain20OutgoingTx,
              OnchainIncomingTxData.onchain20IncomingTx,
              OnchainOutgoingTxData.onchainOutgoingTx
            )
          ),
          erc20TokensBySymbol = OffchainIncomingTxData.offchainIncomingTxEthErc20TokenBySymbolMap
            ++ OffchainOutgoingTxData.offchainOutgoingExErc20TokenBySymbolMap
            ++ OnchainIncomingTxData.onchainIncomingTxEthTokenBySymbolMap2
            ++ OnchainOutgoingTxData.onchainOutgoingTxErc20TokenBySymbolMap
            ++ PendingTxsData.erc20TokenBySymbolMap,
          twitterUsersInfoByTwitterId = PendingTxsData.twitterUserInfoByTwitterIdMapForPending1
            ++ PendingTxsData.twitterUserInfoByTwitterIdMapForPending2,
          twitterAccountsForUser = Map(
            userForTestId -> userForTestTwitterAcc
          )
            ++ OffchainIncomingTxData.offchainIncomingTxTwitterAccForCounterpartUserMap
            ++ OffchainOutgoingTxData.offchainOutgoingTxTwitterAccForCounterpartUserMap,
          pendingTransfersByIssuer = Map(
            (userForTestId, "ERC-20") -> List(
              PendingTxsData.pendingTx1,
              PendingTxsData.pendingTx2
            )
          ),
          ethereumAccountsByAddress = Map(
            userForTestEthAdd -> userForTestEthAcc
          ) ++ OffchainIncomingTxData.ethereumAccountForAddressesMap
            ++ OffchainOutgoingTxData.ethereumAccountForAddressesMap,
          usersInfoFromId = Map(
            userForTestId -> userForTestInfo
          ) ++ OffchainIncomingTxData.offchainIncomingTxCounterpartUserInfoMap
            ++ OffchainOutgoingTxData.offchainOutgoingTxCounterpartUserInfoMap,
          onchainTxsFromId = Map(
            OnchainIncomingTxData.onchainId -> OnchainIncomingTxData.onchain20Tx,
            OnchainOutgoingTxData.onchainId -> OnchainOutgoingTxData.onchainTx
          )
        )

        val txsPage
          : (Int, Int, String) => Id[ValidatedTxRetrievalResult[List[StockmindTransaction]]] =
          transactionsPage(ops)

        /*
         * Expected order:
         * completedOnchainIncomingTx
         * pendingTx2
         * completedOffchainOutgoingTx
         * pendingTx1
         * completedOnchainOutgoingTx
         * offchainIncomingTx
         */

        val expectedTransactionsList = List(
          OnchainIncomingTxData.onchainIncoming20StockmindTx,
          PendingTxsData.pendingTx2StockmindTx,
          OffchainOutgoingTxData.offchain20OutgoingStockmindTx,
          PendingTxsData.pendingTx1StockmindTx,
          OnchainOutgoingTxData.onchainOutgoingStockmindTx,
          OffchainIncomingTxData.offchain20IncomingStockmindTx
        )

        describe("we ask for an offset of 0 and a limit of 6") {
          val result = txsPage(0, 6, "ERC-20")

          it("we get the 6 transactions in the expected chronological order (descendent)") {
            expectedTransactionsList shouldBe result.right.value
          }
        }

        describe("we ask for an offset of 3 and a limit of 3") {
          val result = txsPage(3, 3, "ERC-20")

          it("We get the 4th, 5th and 6th of them") {
            val expectedTxList = List(
              PendingTxsData.pendingTx1StockmindTx,
              OnchainOutgoingTxData.onchainOutgoingStockmindTx,
              OffchainIncomingTxData.offchain20IncomingStockmindTx
            )

            result.right.value shouldBe expectedTxList
          }
        }

        describe("we ask for an offset of 0 and a limit of 3") {
          val result = txsPage(0, 3, "ERC-20")

          it("We get the 1st, 2nd and 3th") {
            val expectedTxList = List(
              OnchainIncomingTxData.onchainIncoming20StockmindTx,
              PendingTxsData.pendingTx2StockmindTx,
              OffchainOutgoingTxData.offchain20OutgoingStockmindTx
            )

            result.right.value shouldBe expectedTxList
          }
        }

        describe("we ask for an offset of 2 and a limit of 10") {
          val result = txsPage(2, 10, "ERC-20")

          it("we get from the 3rd (inclusive) to the end of them ") {
            val expectedTxList = List(
              OffchainOutgoingTxData.offchain20OutgoingStockmindTx,
              PendingTxsData.pendingTx1StockmindTx,
              OnchainOutgoingTxData.onchainOutgoingStockmindTx,
              OffchainIncomingTxData.offchain20IncomingStockmindTx
            )

            result.right.value shouldBe expectedTxList
          }
        }

        describe("We ask for an offset of 7 and a limit of 3") {
          val result = txsPage(7, 3, "ERC-20")

          it("we get an empty list") {
            result.right.value shouldBe List.empty[StockmindTransaction]
          }
        }
      }
    }
  }

  describe("No happy paths") {
    pending
  }
}

object RetrieveTransactionsOpsSpec {

  case class RetrieveTxOpsForTest(
      ethereumAccountByUser: Map[UUID, EthereumAccount] = Map.empty,
      offchainTransfersByAddress: Map[(Address, String), List[OffChainTransfer]] = Map.empty,
      ethereumTokensBySymbol: Map[String, Ethtoken] = Map.empty,
      erc20TokensBySymbol: Map[String, Erc20Token] = Map.empty,
      erc721TokensById: Map[BigInt, Erc721Token] = Map.empty,
      twitterUsersInfoByTwitterId: Map[Long, TwitterUserInfo] = Map.empty,
      twitterAccountsForUser: Map[UUID, TwitterAccount] = Map.empty,
      pendingTransfersByIssuer: Map[(UUID, String), List[PendingTransfer]] = Map.empty,
      ethereumAccountsByAddress: Map[Address, EthereumAccount] = Map.empty,
      usersInfoFromId: Map[UUID, User] = Map.empty,
      onchainTxsFromId: Map[Long, TransferEvent] = Map.empty
  ) extends RetrieveTransactionsOps[cats.Id] {
    override def ethereumAccountForUser(userId: UUID) = ethereumAccountByUser.get(userId)

    override def offchainTransfers(addressInvolved: Address, erc_type: String) =
      offchainTransfersByAddress.getOrElse((addressInvolved, erc_type), List.empty)

    override def ethereumTokenBySymbolAndType(symbol_erc_type: String) =
      ethereumTokensBySymbol.get(symbol_erc_type)

    override def erc20TokenBySymbolAndType(symbol_erc_type: String) =
      erc20TokensBySymbol.get(symbol_erc_type)

    override def erc721TokenById(id: BigInt) =
      erc721TokensById.get(id)

    override def pendingTransfersByIssuer(sourceUserId: UUID, erc_Type: String) =
      pendingTransfersByIssuer.getOrElse((sourceUserId, erc_Type), List.empty)

    override def ethereumAccountByAddress(add: Address) = ethereumAccountsByAddress.get(add)

    override def userInfoFromId(id: UUID) = usersInfoFromId.get(id)

    override def onchainTxFromId(id: Long) = onchainTxsFromId.get(id)
  }
}
