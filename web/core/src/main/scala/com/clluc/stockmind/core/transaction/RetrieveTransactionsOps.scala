package com.clluc.stockmind.core.transaction

import java.util.UUID
import cats.{Applicative, Functor, Monad}
import cats.data.EitherT
import cats.syntax.either._
import cats.syntax.functor._
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.ethereum.{
  Amount,
  Erc20Token,
  Erc721Token,
  EthereumAccount,
  Ethtoken,
  TransferEvent
}
import com.clluc.stockmind.core.transaction.RetrieveTransactionsOps._
import com.clluc.stockmind.core.transaction.StockmindTransaction._
import com.clluc.stockmind.core.user.User

/**
  * Type class that represents the business logic of the retrieve transactions feature.
  * At a high level of abstraction, it sequences monadically the following high level operations:
  * - Validates the parameter offset (must be greater than 0)
  * - Validates the parameter limit (must be greater than 0)
  * - Obtains the requester user completed transactions (using the off-chain repository, that contains also a counterpart
  *   of the on-chain ones).
  * - Obtains the requester user pending transactions (sent by him, not yet completed).
  * - Add the two previous lists, sort them by date and apply the limit and offset.
  *
  * Those high level operations imply the use of the abstract, primitive operations of this trait. Some of them
  * might not return valid or required data. In order to leverage monads to approach a safe implementation in a clean way
  * we need lots of auxiliary methods that return instances of either, holding either a valid value or a failure condition
  * represented as an instance of an algebraic data type (ADT). We can then apply monadic sequencing of that data using
  * flatMap (for comprehension), stating the happy path and letting the underlying data structure short-circuit execution
  * if a step fails.
  *
  * @tparam P The context under which the operations of this type class are going to be run
  */
private[transaction] trait RetrieveTransactionsOps[P[_]] {

  /*
   **********************************
   Abstract / primitive operations
   **********************************
   */
  // Due to the uniform access principle in functional programming (https://en.wikipedia.org/wiki/Uniform_access_principle)
  // we don't use verbs for functions / methods names anymore. They are values.
  def ethereumAccountForUser(userId: UUID): P[Option[EthereumAccount]]

  def offchainTransfers(addressInvolved: Address,
                        transaction_type: String): P[List[OffChainTransfer]]

  def ethereumTokenBySymbolAndType(symbol_erc_type: String): P[Option[Ethtoken]]

  def erc20TokenBySymbolAndType(symbol_erc_type: String): P[Option[Erc20Token]]

  def erc721TokenById(id: BigInt): P[Option[Erc721Token]]

  def pendingTransfersByIssuer(sourceUserId: UUID,
                               transaction_type: String): P[List[PendingTransfer]]

  def ethereumAccountByAddress(add: Address): P[Option[EthereumAccount]]

  def userInfoFromId(id: UUID): P[Option[User]]

  def onchainTxFromId(id: Long): P[Option[TransferEvent]]

  /*
   **********************************
   Derived operations (business logic) of this type class
   **********************************
   */
  // TODO Simplify this when we have tests
  // TODO Remove every implicit CatsTypeClass[P] in inner functions here
  def transactionsPage(userId: UUID, offset: Int, limit: Int, transaction_type: String)(
      implicit ev: Monad[P]): P[ValidatedTxRetrievalResult[List[StockmindTransaction]]] = {
    /* Naive, easy approach; we have to mix and sort results from two different data sources to do this.
     * We don't know how many tuples of each of those data sources we will need to fulfil this request.
     * Solution: we ask for all the offchain txs and pendings for the user, and compute in memory.
     * Doesn't scale a lot; but provides good use experience for small data sets, and it's easy to implement.
     */

    /*
   **********************************
   **********************************
   Plumbing required to make the high level logic easy to read
   Skip this section of the code unless you need to get into that detail
   The end of the section is indicated in a comment like this one, surrounder by two lines of *
   **********************************
   **********************************
     */
    /*
   **********************************
   Auxiliary derived functions to lift things
   **********************************
     */
    // Some auxiliary methods to lift our primitives to the proper contexts (higher kinds)
    def liftSomething[C[_], T, U](x: => P[C[T]])(fx: C[T] => Either[TransactionsRetrievalError, U])(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, U] =
      EitherT(x.map(fx))

    def liftOption[T](o: => P[Option[T]], ifNone: => TransactionsRetrievalError)(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, T] =
      liftSomething(o)(Either.fromOption(_, ifNone))

    def liftMapAsRight[C[_]: Functor, T, U](x: => P[C[T]])(mapFx: T => U)(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, C[U]] =
      liftSomething(x)(_.map(mapFx).asRight[TransactionsRetrievalError])

    def liftAsRight[C[_]: Functor, T](x: => P[C[T]])(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, C[T]] =
      liftMapAsRight(x)(identity)

    def liftTokenBySymbolAndType(symbol_erc_type: String)(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, Erc20Token] =
      liftOption(erc20TokenBySymbolAndType(symbol_erc_type), tokenNotSupported(symbol_erc_type))

    lazy val liftUserEthereumAccount: ValidatedTxRetrievalResultInContext[P, EthereumAccount] =
      liftOption(ethereumAccountForUser(userId), currentUserWithoutStockmindEthereumAddress(userId))

    /*
   **********************************
   Type class required to abstract over some derived operations logic
   **********************************
     */
    // We need to convert to Transaction instances both completed transactions (on and aff-chain) and pending transactions.
    // This type class will be used to implement that conversion for both transaction types.
    trait StockmindTransactionConversion[T] {
      def toTransaction(t: T): ValidatedTxRetrievalResultInContext[P, StockmindTransaction]
    }

    // This object contains the actual implementations of the TransactionViewConversion type class
    // It defines two functions to convert completed and pending transactions respectively into generic transactions.
    // After that, it defines the instances of the type class that, using those functions, will be wired via implicits
    // (Standard mechanism in Scala to wire instance of type classes)
    object ToTransactionConversions {
      // Implement a conversion from an off-chain transfer instance into a transaction (plain function)
      def completedToTransaction(offchain: OffChainTransfer)(
          implicit ev: Monad[P]): ValidatedTxRetrievalResultInContext[P, StockmindTransaction] = {

        // **************************************
        // Here auxiliary lift methods & friends to allow business logic definition to take place in a single
        // for comprehension (only needed in the conversion from a completed transaction into a Transaction entity)
        // **************************************
        def sequenceAndFlatten[T](o: Option[P[Option[T]]]): P[Option[T]] = {
          // These imports can mess up implicits resolution if put in file global scope
          // We'd rather explicit them where needed, even though that lead to some duplication
          import cats.instances.option._
          import cats.syntax.traverse._

          o.sequence.map(_.flatten)
        }

        def liftUserIdForAddress(
            address: Address): ValidatedTxRetrievalResultInContext[P, Option[UUID]] = {
          import cats.instances.option._ // Bring in Functor[Option]

          liftMapAsRight(ethereumAccountByAddress(address))(_.user)
        }

        def liftMaybeOnchainCounterpartFromMaybeId(id: Option[Long])(implicit ev: Applicative[P])
          : ValidatedTxRetrievalResultInContext[P, Option[TransferEvent]] = {
          val maybeOnchainTxP: P[Option[TransferEvent]] = sequenceAndFlatten(
            id.map(onchainTxFromId))

          import cats.instances.option._ // Bring in Functor[Option]
          liftAsRight(maybeOnchainTxP)
        }

        /*
         * Function that, given the individual attributes of a transaction entity, constructs the Transaction instance
         */
        def resolveTransaction(
            tokenDecimals: Int,
            tokenAmount: TokenAmount,
            tokenDescription: String,
            maybeOnchainTx: Option[TransferEvent],
            myEthAcc: EthereumAccount,
            maybeUserIdFrom: Option[UUID],
            maybeUserIdTo: Option[UUID],
        ): ValidatedTxRetrievalResultInContext[P, StockmindTransaction] = {
          def _buildTransaction(direction: TransactionDirection, counterparty: Counterparty)
            : ValidatedTxRetrievalResultInContext[P, StockmindTransaction] = {
            EitherT.fromEither(
              StockmindTransaction(
                id = offchain.id,
                direction = direction,
                pending = false,
                counterparty = counterparty,
                token = offchain.tokenSymbol,
                erc_type = offchain.erc_type,
                tokenDescription = tokenDescription,
                decimals = tokenDecimals,
                amount = tokenAmount,
                txHash = maybeOnchainTx.map(_.txHash.toPrefixedHexString),
                date = offchain.created
              ).asRight[TransactionsRetrievalError]
            )
          }

          def mapOnChain(mapFx: TransferEvent => Address): Option[String] = {
            maybeOnchainTx.map(mapFx).map(_.toHex).orElse(Some("0x"))
          }

          (maybeUserIdFrom, maybeUserIdTo) match {
            case (None, Some(_)) => // Inbound transfer
              val counterparty = Counterparty(
                mapOnChain(_.from),
                None
              )
              _buildTransaction(IncomingTx, counterparty)

            case (Some(_), None) => // Outbound transfer
              val counterparty = Counterparty(
                mapOnChain(_.to),
                None
              )
              _buildTransaction(OutgoingTx, counterparty)

            case (Some(_), Some(_)) => // Offchain transfer
              if (offchain.from == myEthAcc.address) { // Outgoing
                val counterparty = Counterparty(
                  Some(offchain.to.toHex),
                  None
                )
                _buildTransaction(OutgoingTx, counterparty)
              } else { // Incoming
                val counterparty = Counterparty(
                  Some(offchain.from.toHex),
                  None
                )
                _buildTransaction(IncomingTx, counterparty)
              }

            case (None, None) => // should not happen
              EitherT.fromEither(
                neitherFromNorToHaveScreenNames(offchain.from, offchain.to)
                  .asLeft[StockmindTransaction])
          }
        }

        /*
         * At last; sequence of operations required to build a Transaction from all the data of a completed one,
         * defined in just one for comprehension.
         * Monadic sequencing allows to simply put the steps one after another. The execution short circuits at any
         * step that fails.
         * Much cleaner than defensive programming using if / else & try / catch constructs.
         * Here we make use of the auxiliary functions defined above.
         */
        for {
          token <- liftTokenBySymbolAndType(offchain.tokenSymbol + "|" + offchain.erc_type)
          decimals = token.decimals
          amount   = Amount.fromRawIntegerValue(offchain.amount.value.toString, decimals)
          maybeUserIdFrom <- liftUserIdForAddress(offchain.from)
          maybeUserIdTo   <- liftUserIdForAddress(offchain.to)
          maybeOnchainTx  <- liftMaybeOnchainCounterpartFromMaybeId(offchain.onchainTransferId)
          myEthAccount    <- liftUserEthereumAccount
          tokenAmount = TokenAmount(amount.integerPart, amount.decimalPart)
          transaction <- resolveTransaction(
            decimals,
            tokenAmount,
            token.name,
            maybeOnchainTx,
            myEthAccount,
            maybeUserIdFrom,
            maybeUserIdTo
          )
        } yield transaction
      }

      /*
       * Implement a conversion from a pending transfer into a transaction (plain pure function)
       */
      def pendingToTransaction(pending: PendingTransfer)(
          implicit ev: Monad[P]): ValidatedTxRetrievalResultInContext[P, StockmindTransaction] = {

        // **************************************
        // And now the monadic sequence of step for this transformation from pending transaction to
        // Transaction instance
        // **************************************
        for {
          token <- liftTokenBySymbolAndType(pending.tokenSymbol + "|" + pending.erc_type)
          counterparty = Counterparty(
            None, // It's a pending transaction, the recipient is no solidGo user yet. Has no ethereum address
            None
          )
          amount = Amount.fromRawIntegerValue(pending.amount.toString, token.decimals)
          date   = pending.created
        } yield
          StockmindTransaction(
            id = pending.id,
            direction = OutgoingTx,
            pending = true,
            counterparty = counterparty,
            token = pending.tokenSymbol,
            erc_type = pending.erc_type,
            tokenDescription = token.name,
            decimals = token.decimals,
            amount = TokenAmount(amount.integerPart, amount.decimalPart),
            txHash = None, // Not yet completed; so no hash
            date = date
          )
      }

      /*
       * Object that defines the actual instances of the TransactionViewConversion[T] type class
       */
      object Instances {
        implicit object OffchainToTransaction
            extends StockmindTransactionConversion[OffChainTransfer] {
          override def toTransaction(t: OffChainTransfer) =
            completedToTransaction(t)
        }

        implicit object PendingToTransaction
            extends StockmindTransactionConversion[PendingTransfer] {
          override def toTransaction(t: PendingTransfer) = pendingToTransaction(t)
        }
      }
    }

    /*
   **********************************
   Some more auxiliary lift functions, required by code that is defined below
   **********************************
     */
    // TODO Try to move this with the rest of the auxiliary functions above
    def liftOffchainTransfers(
        userEthAdd: Address): ValidatedTxRetrievalResultInContext[P, List[OffChainTransfer]] = {
      import cats.instances.list._ // Bring in Functor[List]

      liftAsRight(offchainTransfers(userEthAdd, transaction_type))
    }

    lazy val liftUserOffchainTxs: ValidatedTxRetrievalResultInContext[P, List[OffChainTransfer]] =
      for {
        userEthAcc  <- liftUserEthereumAccount
        offchainTxs <- liftOffchainTransfers(userEthAcc.address)
      } yield offchainTxs

    lazy val liftPendings: ValidatedTxRetrievalResultInContext[P, List[PendingTransfer]] = {
      import cats.instances.list._ // Bring in Functor[List]

      liftAsRight(pendingTransfersByIssuer(userId, transaction_type))
    }

    def liftToTransactionView[T](ts: List[T])(
        implicit conversion: StockmindTransactionConversion[T])
      : ValidatedTxRetrievalResultInContext[P, List[StockmindTransaction]] = {
      val allAsTxs: List[P[ValidatedTxRetrievalResult[StockmindTransaction]]] =
        ts.map(conversion.toTransaction)
          .map(_.value) // For some reason using a syntax here doesn't compile

      import cats.instances.list._
      import cats.instances.either._
      import cats.syntax.traverse._

      val allTxsInsideP: P[List[ValidatedTxRetrievalResult[StockmindTransaction]]] =
        allAsTxs.sequence

      val validatedTxs: P[ValidatedTxRetrievalResult[List[StockmindTransaction]]] =
        allTxsInsideP.map(_.sequence)

      EitherT(validatedTxs)
    }

    def liftOffchainToTransactionRepresentation(
        offchainTxs: List[OffChainTransfer]
    ): ValidatedTxRetrievalResultInContext[P, List[StockmindTransaction]] = {
      import ToTransactionConversions.Instances.OffchainToTransaction

      liftToTransactionView(offchainTxs)
    }

    def liftPendingToTransactionRepresentation(
        pendings: List[PendingTransfer]
    ): ValidatedTxRetrievalResultInContext[P, List[StockmindTransaction]] = {
      import ToTransactionConversions.Instances.PendingToTransaction

      liftToTransactionView(pendings)
    }

    // Input parameters validation function
    // Only condition; offset and limit must be >= 0
    def liftValidateIntegerParameter(name: String,
                                     value: Int): ValidatedTxRetrievalResultInContext[P, Int] =
      EitherT.fromEither[P](Either.cond(value >= 0, value, paramShouldNotBeNegative(name, value)))

    /*
   **********************************
   **********************************
   End of the plumbing section. Here is the easy to read high level logic
   You can navigate deeper levels of abstraction if required just looking at the subsequent calls
   **********************************
   **********************************
     */

    // HERE IS WHERE YOU SHOULD BEGIN TO READ THIS TRAIT CODE to ease it's understanding.
    // Merges all offs-chain and pending transfers into a single list of Transaction instances
    // And then apply the offset and the take number of transactions
    // This is the highest abstraction level of the entire trait
    val validatedTransfers =
      for {
        validatedOffset <- liftValidateIntegerParameter("offset", offset)
        validatedLimit  <- liftValidateIntegerParameter("limit", limit)
        offchainTxs     <- liftUserOffchainTxs
        pendingTxs      <- liftPendings
        txsFromOffchain <- liftOffchainToTransactionRepresentation(offchainTxs)
        txsFromPending  <- liftPendingToTransactionRepresentation(pendingTxs)
        requestedTransfers = txsFromOffchain ++ txsFromPending
      } yield
        requestedTransfers
          .sortWith { (t1, t2) =>
            t2.date.isBefore(t1.date)
          }
          .slice(validatedOffset, validatedOffset + validatedLimit)

    validatedTransfers.value
  }

  def transactionsPage721(userId: UUID, offset: Int, limit: Int, transaction_type: String)(
      implicit ev: Monad[P]): P[ValidatedTxRetrievalResult[List[Stockmind721Transaction]]] = {
    /* Naive, easy approach; we have to mix and sort results from two different data sources to do this.
     * We don't know how many tuples of each of those data sources we will need to fulfil this request.
     * Solution: we ask for all the offchain txs and pendings for the user, and compute in memory.
     * Doesn't scale a lot; but provides good use experience for small data sets, and it's easy to implement.
     */

    /*
   **********************************
   **********************************
   Plumbing required to make the high level logic easy to read
   Skip this section of the code unless you need to get into that detail
   The end of the section is indicated in a comment like this one, surrounder by two lines of *
   **********************************
   **********************************
     */
    /*
   **********************************
   Auxiliary derived functions to lift things
   **********************************
     */
    // Some auxiliary methods to lift our primitives to the proper contexts (higher kinds)
    def liftSomething[C[_], T, U](x: => P[C[T]])(fx: C[T] => Either[TransactionsRetrievalError, U])(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, U] =
      EitherT(x.map(fx))

    def liftOption[T](o: => P[Option[T]], ifNone: => TransactionsRetrievalError)(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, T] =
      liftSomething(o)(Either.fromOption(_, ifNone))

    def liftMapAsRight[C[_]: Functor, T, U](x: => P[C[T]])(mapFx: T => U)(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, C[U]] =
      liftSomething(x)(_.map(mapFx).asRight[TransactionsRetrievalError])

    def liftAsRight[C[_]: Functor, T](x: => P[C[T]])(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, C[T]] =
      liftMapAsRight(x)(identity)

    def liftTokenById(id: BigInt)(
        implicit ev: Functor[P]): ValidatedTxRetrievalResultInContext[P, Erc721Token] =
      liftOption(erc721TokenById(id), tokenNotSupported(id.toString()))

    lazy val liftUserEthereumAccount: ValidatedTxRetrievalResultInContext[P, EthereumAccount] =
      liftOption(ethereumAccountForUser(userId), currentUserWithoutStockmindEthereumAddress(userId))

    /*
   **********************************
   Type class required to abstract over some derived operations logic
   **********************************
     */
    // We need to convert to Transaction instances both completed transactions (on and aff-chain) and pending transactions.
    // This type class will be used to implement that conversion for both transaction types.
    trait StockmindTransactionConversion[T] {
      def toTransaction(t: T): ValidatedTxRetrievalResultInContext[P, Stockmind721Transaction]
    }

    // This object contains the actual implementations of the TransactionViewConversion type class
    // It defines two functions to convert completed and pending transactions respectively into generic transactions.
    // After that, it defines the instances of the type class that, using those functions, will be wired via implicits
    // (Standard mechanism in Scala to wire instance of type classes)
    object ToTransactionConversions {
      // Implement a conversion from an off-chain transfer instance into a transaction (plain function)
      def completedToTransaction(offchain: OffChainTransfer)(implicit ev: Monad[P])
        : ValidatedTxRetrievalResultInContext[P, Stockmind721Transaction] = {

        // **************************************
        // Here auxiliary lift methods & friends to allow business logic definition to take place in a single
        // for comprehension (only needed in the conversion from a completed transaction into a Transaction entity)
        // **************************************
        def sequenceAndFlatten[T](o: Option[P[Option[T]]]): P[Option[T]] = {
          // These imports can mess up implicits resolution if put in file global scope
          // We'd rather explicit them where needed, even though that lead to some duplication
          import cats.instances.option._
          import cats.syntax.traverse._

          o.sequence.map(_.flatten)
        }

        def liftUserIdForAddress(
            address: Address): ValidatedTxRetrievalResultInContext[P, Option[UUID]] = {
          import cats.instances.option._ // Bring in Functor[Option]

          liftMapAsRight(ethereumAccountByAddress(address))(_.user)
        }

        def liftMaybeOnchainCounterpartFromMaybeId(id: Option[Long])(implicit ev: Applicative[P])
          : ValidatedTxRetrievalResultInContext[P, Option[TransferEvent]] = {
          val maybeOnchainTxP: P[Option[TransferEvent]] = sequenceAndFlatten(
            id.map(onchainTxFromId))

          import cats.instances.option._ // Bring in Functor[Option]
          liftAsRight(maybeOnchainTxP)
        }

        /*
         * Function that, given the individual attributes of a transaction entity, constructs the Transaction instance
         */
        def resolveTransaction(
            tokenMetadata: String,
            tokenId: BigInt,
            tokenDescription: String,
            maybeOnchainTx: Option[TransferEvent],
            myEthAcc: EthereumAccount,
            maybeUserIdFrom: Option[UUID],
            maybeUserIdTo: Option[UUID],
        ): ValidatedTxRetrievalResultInContext[P, Stockmind721Transaction] = {
          def _buildTransaction(direction: TransactionDirection, counterparty: Counterparty)
            : ValidatedTxRetrievalResultInContext[P, Stockmind721Transaction] = {
            EitherT.fromEither(
              Stockmind721Transaction(
                id = offchain.id,
                direction = direction,
                pending = false,
                counterparty = counterparty,
                token = offchain.tokenSymbol,
                erc_type = offchain.erc_type,
                tokenDescription = tokenDescription,
                meta = tokenMetadata,
                token_id = tokenId,
                txHash = maybeOnchainTx.map(_.txHash.toPrefixedHexString),
                date = offchain.created
              ).asRight[TransactionsRetrievalError]
            )
          }

          def mapOnChain(mapFx: TransferEvent => Address): Option[String] = {
            maybeOnchainTx.map(mapFx).map(_.toHex).orElse(Some("0x"))
          }

          (maybeUserIdFrom, maybeUserIdTo) match {
            case (None, Some(_)) => // Inbound transfer
              val counterparty = Counterparty(
                mapOnChain(_.from),
                None
              )
              _buildTransaction(IncomingTx, counterparty)

            case (Some(_), None) => // Outbound transfer
              val counterparty = Counterparty(
                mapOnChain(_.to),
                None
              )
              _buildTransaction(OutgoingTx, counterparty)

            case (Some(_), Some(_)) => // Offchain transfer
              if (offchain.from == myEthAcc.address) { // Outgoing
                val counterparty = Counterparty(
                  Some(offchain.to.toHex),
                  None
                )
                _buildTransaction(OutgoingTx, counterparty)
              } else { // Incoming
                val counterparty = Counterparty(
                  Some(offchain.from.toHex),
                  None
                )
                _buildTransaction(IncomingTx, counterparty)
              }

            case (None, None) => // should not happen
              EitherT.fromEither(
                neitherFromNorToHaveScreenNames(offchain.from, offchain.to)
                  .asLeft[Stockmind721Transaction])
          }
        }

        /*
         * At last; sequence of operations required to build a Transaction from all the data of a completed one,
         * defined in just one for comprehension.
         * Monadic sequencing allows to simply put the steps one after another. The execution short circuits at any
         * step that fails.
         * Much cleaner than defensive programming using if / else & try / catch constructs.
         * Here we make use of the auxiliary functions defined above.
         */
        for {
          token           <- liftTokenById(offchain.token_id.get)
          maybeUserIdFrom <- liftUserIdForAddress(offchain.from)
          maybeUserIdTo   <- liftUserIdForAddress(offchain.to)
          maybeOnchainTx  <- liftMaybeOnchainCounterpartFromMaybeId(offchain.onchainTransferId)
          myEthAccount    <- liftUserEthereumAccount
          transaction <- resolveTransaction(
            token.meta,
            token.id,
            token.name,
            maybeOnchainTx,
            myEthAccount,
            maybeUserIdFrom,
            maybeUserIdTo
          )
        } yield transaction
      }

      /*
       * Implement a conversion from a pending transfer into a transaction (plain pure function)
       */
      def pendingToTransaction(pending: PendingTransfer)(implicit ev: Monad[P])
        : ValidatedTxRetrievalResultInContext[P, Stockmind721Transaction] = {

        // **************************************
        // And now the monadic sequence of step for this transformation from pending transaction to
        // Transaction instance
        // **************************************
        for {
          token <- liftTokenById(pending.token_id.get)
          counterparty = Counterparty(
            None, // It's a pending transaction, the recipient is no solidGo user yet. Has no ethereum address
            None
          )
          date = pending.created
        } yield
          Stockmind721Transaction(
            id = pending.id,
            direction = OutgoingTx,
            pending = true,
            counterparty = counterparty,
            token = pending.tokenSymbol,
            erc_type = pending.erc_type,
            tokenDescription = token.name,
            meta = token.meta,
            token_id = token.id,
            txHash = None, // Not yet completed; so no hash
            date = date
          )
      }

      /*
       * Object that defines the actual instances of the TransactionViewConversion[T] type class
       */
      object Instances {
        implicit object OffchainToTransaction
            extends StockmindTransactionConversion[OffChainTransfer] {
          override def toTransaction(t: OffChainTransfer) =
            completedToTransaction(t)
        }

        implicit object PendingToTransaction
            extends StockmindTransactionConversion[PendingTransfer] {
          override def toTransaction(t: PendingTransfer) = pendingToTransaction(t)
        }
      }
    }

    /*
   **********************************
   Some more auxiliary lift functions, required by code that is defined below
   **********************************
     */
    // TODO Try to move this with the rest of the auxiliary functions above
    def liftOffchainTransfers(
        userEthAdd: Address): ValidatedTxRetrievalResultInContext[P, List[OffChainTransfer]] = {
      import cats.instances.list._ // Bring in Functor[List]

      liftAsRight(offchainTransfers(userEthAdd, transaction_type))
    }

    lazy val liftUserOffchainTxs: ValidatedTxRetrievalResultInContext[P, List[OffChainTransfer]] =
      for {
        userEthAcc  <- liftUserEthereumAccount
        offchainTxs <- liftOffchainTransfers(userEthAcc.address)
      } yield offchainTxs

    lazy val liftPendings: ValidatedTxRetrievalResultInContext[P, List[PendingTransfer]] = {
      import cats.instances.list._ // Bring in Functor[List]

      liftAsRight(pendingTransfersByIssuer(userId, transaction_type))
    }

    def liftToTransactionView[T](ts: List[T])(
        implicit conversion: StockmindTransactionConversion[T])
      : ValidatedTxRetrievalResultInContext[P, List[Stockmind721Transaction]] = {
      val allAsTxs: List[P[ValidatedTxRetrievalResult[Stockmind721Transaction]]] =
        ts.map(conversion.toTransaction)
          .map(_.value) // For some reason using a syntax here doesn't compile

      import cats.instances.list._
      import cats.instances.either._
      import cats.syntax.traverse._

      val allTxsInsideP: P[List[ValidatedTxRetrievalResult[Stockmind721Transaction]]] =
        allAsTxs.sequence

      val validatedTxs: P[ValidatedTxRetrievalResult[List[Stockmind721Transaction]]] =
        allTxsInsideP.map(_.sequence)

      EitherT(validatedTxs)
    }

    def liftOffchainToTransactionRepresentation(
        offchainTxs: List[OffChainTransfer]
    ): ValidatedTxRetrievalResultInContext[P, List[Stockmind721Transaction]] = {
      import ToTransactionConversions.Instances.OffchainToTransaction

      liftToTransactionView(offchainTxs)
    }

    def liftPendingToTransactionRepresentation(
        pendings: List[PendingTransfer]
    ): ValidatedTxRetrievalResultInContext[P, List[Stockmind721Transaction]] = {
      import ToTransactionConversions.Instances.PendingToTransaction

      liftToTransactionView(pendings)
    }

    // Input parameters validation function
    // Only condition; offset and limit must be >= 0
    def liftValidateIntegerParameter(name: String,
                                     value: Int): ValidatedTxRetrievalResultInContext[P, Int] =
      EitherT.fromEither[P](Either.cond(value >= 0, value, paramShouldNotBeNegative(name, value)))

    /*
   **********************************
   **********************************
   End of the plumbing section. Here is the easy to read high level logic
   You can navigate deeper levels of abstraction if required just looking at the subsequent calls
   **********************************
   **********************************
     */

    // HERE IS WHERE YOU SHOULD BEGIN TO READ THIS TRAIT CODE to ease it's understanding.
    // Merges all offs-chain and pending transfers into a single list of Transaction instances
    // And then apply the offset and the take number of transactions
    // This is the highest abstraction level of the entire trait
    val validatedTransfers =
      for {
        validatedOffset <- liftValidateIntegerParameter("offset", offset)
        validatedLimit  <- liftValidateIntegerParameter("limit", limit)
        offchainTxs     <- liftUserOffchainTxs
        pendingTxs      <- liftPendings
        txsFromOffchain <- liftOffchainToTransactionRepresentation(offchainTxs)
        txsFromPending  <- liftPendingToTransactionRepresentation(pendingTxs)
        requestedTransfers = txsFromOffchain ++ txsFromPending
      } yield
        requestedTransfers
          .sortWith { (t1, t2) =>
            t2.date.isBefore(t1.date)
          }
          .slice(validatedOffset, validatedOffset + validatedLimit)

    validatedTransfers.value
  }
}

private[transaction] object RetrieveTransactionsOps {

  // Monadic type for our validated individual operations result
  type ValidatedTxRetrievalResultInContext[P[_], T] =
    EitherT[P, TransactionsRetrievalError, T]
}
