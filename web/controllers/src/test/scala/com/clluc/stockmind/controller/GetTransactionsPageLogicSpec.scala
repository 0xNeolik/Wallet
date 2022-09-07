package com.clluc.stockmind.controller

import java.util.UUID

import akka.actor.Cancellable
import akka.stream.{Attributes, ClosedShape, Graph, Materializer}
import cats.data.State
import com.clluc.stockmind.controller.RetrieveTransactionsController.TransactionView
import com.clluc.stockmind.core.ethereum.solidity.Address
import com.clluc.stockmind.core.transaction.StockmindTransaction.ValidatedTxRetrievalResult
import com.clluc.stockmind.core.transaction.{
  Counterparty,
  OutgoingTx,
  Stockmind721Transaction,
  StockmindTransaction,
  TokenAmount
}
import io.circe.{Json, ParsingFailure, Printer}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalatest.{EitherValues, FunSpec, Matchers, OptionValues}
import play.api.http.Status._
import play.api.mvc.Result

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Random

class GetTransactionsPageLogicSpec
    extends FunSpec
    with Matchers
    with EitherValues
    with OptionValues {

  // Define a generic fixture based on the State monad
  // First, define a data structure to keep the parameters used to invoke the primary port logic
  case class FindTransactionsPageParameters(userId: UUID, offset: Int, numOfTxs: Int)

  // In case effects are called (because input parameters validations have passed) the state will hold a value
  // Some(...). If no effects are called, that value will be None
  type TestState[A] = State[Option[FindTransactionsPageParameters], A]

  // Give a test instance for our GetTransactionsPageLogic[P[_]] trait based on the previously defined state
  // transformation (TestState[A])
  class Fixture(businessLogicResult: ValidatedTxRetrievalResult[List[StockmindTransaction]],
                businessLogicResult721: ValidatedTxRetrievalResult[List[Stockmind721Transaction]])
      extends GetTransactionsPageLogic[TestState] {
    override def callTransactionsPageOnPrimaryPort(userId: UUID, offset: Int, numOfTxs: Int) =
      State { _ =>
        (Some(
           FindTransactionsPageParameters(
             userId,
             offset,
             numOfTxs
           )),
         businessLogicResult)
      }

    override def call721TransactionsPageOnPrimaryPort(userId: UUID, offset: Int, numOfTxs: Int) =
      State { _ =>
        (Some(
           FindTransactionsPageParameters(
             userId,
             offset,
             numOfTxs
           )),
         businessLogicResult721)
      }
  }

  // Define some sample data to fake the business logic execution result
  private val olderThanParameterValue = 1538489609171L
  val olderThanValue                  = new DateTime(new java.util.Date(olderThanParameterValue))

  // Both what we get from the primary port call
  val validTxsList = List(
    StockmindTransaction(
      id = 10,
      direction = OutgoingTx,
      pending = false,
      counterparty = Counterparty(
        ethaddr = Some(Address.default.value),
        None
      ),
      token = "XXX",
      erc_type = "ERC-20",
      tokenDescription = "Some token",
      decimals = 8,
      amount = TokenAmount(
        "XXX",
        "10"
      ),
      txHash = None,
      date = olderThanValue
    )
  )

  val valid721TxsList = List(
    Stockmind721Transaction(
      id = 10,
      direction = OutgoingTx,
      pending = false,
      counterparty = Counterparty(
        ethaddr = Some(Address.default.value),
        None
      ),
      token = "XXX",
      erc_type = "ERC-721",
      tokenDescription = "Some token",
      meta = "metadata",
      token_id =
        BigInt("71120786848863412851373030999642871879808768922518165984257232620739138279176"),
      txHash = None,
      date = olderThanValue
    )
  )

  // And what we expect our controller to produce
  val validTxsViewList = List(
    TransactionView(
      id = "10",
      direction = "send",
      pending = false,
      counterparty = Counterparty(
        ethaddr = Some(Address.default.value),
        None
      ),
      token = "XXX",
      decimals = 8,
      amount = TokenAmount(
        "XXX",
        "10"
      ),
      txHash = None,
      date = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss").print(olderThanValue)
    )
  )

  // Define some primary port result fixture that returns the previously defined values
  val logic = new Fixture(Right(validTxsList), Right(valid721TxsList))

  // Fix parameters on transactions page logic that we are not going to change using currying
  // and partial application of functions
  def genString = Random.nextString(10)

  val testUserId = UUID.randomUUID()

  // Aliases just to improve code readability below
  type Limit  = Seq[String]
  type Offset = Seq[String]
  type UserId = UUID

  // Finally define the functions we will reuse in tests over and over again
  val txsPage: (Offset, Limit) => TestState[Result] = {

    val txsPageCurryfied: (UserId) => (Offset, Limit) => TestState[Result] = {
      (userId) => (off, lim) =>
        logic.transactionsPage(off, lim, userId)
    }

    txsPageCurryfied(testUserId)
  }

  // Some utility functions and data structures to work with Play Results
  // A Fake materializer needed to extract the body from the Play Result
  // Copied from the Play testing helpers source. Just for tnis small bunch of lines
  // is not worth to add a heavy dependency.
  object NoMaterializer extends Materializer {
    override def withNamePrefix(name: String): Materializer =
      throw new UnsupportedOperationException("NoMaterializer cannot be named")
    override def materialize[Mat](runnable: Graph[ClosedShape, Mat]): Mat =
      throw new UnsupportedOperationException("NoMaterializer cannot materialize")
    override def materialize[Mat](runnable: Graph[ClosedShape, Mat],
                                  initialAttributes: Attributes): Mat =
      throw new UnsupportedOperationException("NoMaterializer cannot materialize")

    override def executionContext: ExecutionContextExecutor =
      throw new UnsupportedOperationException("NoMaterializer does not provide an ExecutionContext")

    def scheduleOnce(delay: FiniteDuration, task: Runnable): Cancellable =
      throw new UnsupportedOperationException("NoMaterializer cannot schedule a single event")

    def schedulePeriodically(initialDelay: FiniteDuration,
                             interval: FiniteDuration,
                             task: Runnable): Cancellable =
      throw new UnsupportedOperationException("NoMaterializer cannot schedule a repeated event")
  }

  // Utility functions to test specific cases fast
  // Run the function using a state transformation execution context (the state monad as the P[_])
  def txsPageStateAndResult(off: Offset,
                            lim: Limit): (Option[FindTransactionsPageParameters], Result) =
    txsPage(off, lim).run(None).value

  // Retrieve the Play result body as an UTF-8 encoded result
  def bodyAsText(result: Result): String = {
    import scala.concurrent.duration._

    Await.result(result.body.consumeData(NoMaterializer), 1.second).utf8String
  }

  // The following one assumes that either one of the limit and offset parameters (or both) are invalid.
  // It asserts what should happen in this case; as part of it, verifying the the message we get in the
  // response body is as expected.
  def badRequestTest(limit: Limit)(offset: Offset)(expectedMsg: String) = {

    val (state, result) = txsPageStateAndResult(offset, limit)

    it("We get a bad request") {
      result.header.status shouldBe BAD_REQUEST
    }

    it("The error message is as expected") {
      bodyAsText(result) shouldBe expectedMsg
    }

    it("No business logic is called") {
      state shouldBe empty
    }
  }

  // Utility method to ease test cases where only one parameter (either offset or limit) is invalid
  // The testFx receives that invalid parameter, an expected error message and make the corresponding
  // assertions.
  def oneParameterInvalidTest(testFx: Seq[String] => String => Unit, parameterName: String) = {
    describe("We pass an empty string") {
      testFx(Seq(""))(
        s"Query string parameter $parameterName (value: ) has incorrect format: Not an integer")
    }

    describe("We pass an empty list") {
      testFx(Seq.empty[String])(s"Query string parameter $parameterName doesn't contain any value")
    }

    describe("We pass not numeric value") {
      testFx(Seq("Hey"))(
        s"Query string parameter $parameterName (value: Hey) has incorrect format: Not an integer")
    }

    describe("We pass more than one valid value") {
      testFx(Seq("1", "2"))(s"Query string parameter $parameterName contains multiple values: 1, 2")
    }

    describe("We pass valid values mixed with no valid ones") {
      testFx(Seq("1", "pepe"))(
        s"Query string parameter $parameterName contains multiple values: 1, pepe")
    }

    describe("We pass a negative number") {
      testFx(Seq("-3"))(
        s"Query string parameter $parameterName (value: -3) disobeys constraint: Cannot be negative")
    }
  }

  // Last, define test cases
  describe("Passing invalid parameters") {
    describe("Passing an invalid offset parameter") {
      val validLimit = Seq("10")
      val _badOffsetTest: Seq[String] => String => Unit =
        offset => msg => badRequestTest(validLimit)(offset)(msg)

      oneParameterInvalidTest(_badOffsetTest, "offset")
    }

    describe("Passing an invalid limit parameter") {
      val validOffset = Seq("2")
      val _badLimitTest: Seq[String] => String => Unit =
        limit => msg => badRequestTest(limit)(validOffset)(msg)

      oneParameterInvalidTest(_badLimitTest, "limit")
    }

    /*describe("Passing both invalid offset and limit parameters") {
      badRequestTest(Seq("-3"))(Seq("Hey"))(
        """Query string parameter offset (value: Hey) has incorrect format: Not an integer
          |Query string parameter limit (value: -3) disobeys constraint: Cannot be negative""".stripMargin
      )
    }*/
  }

  describe("Passing valid parameters") { // not happy path results can only occur in case of data integrity issues; so for now we don't consider them in these tests
    val (state, result) = txsPageStateAndResult(Seq("1"), Seq("5"))

    it("We get the expected JSON in the body") {
      import io.circe.parser.parse
      import io.circe.syntax._
      import io.circe.generic.auto._

      val jsonResultAsText                         = bodyAsText(result)
      val jsonResult: Either[ParsingFailure, Json] = parse(jsonResultAsText)
      val expectedJsonResult: Json                 = validTxsViewList.asJson

      def prettyJson(json: Json): String = {
        val jsonPrinter = Printer(preserveOrder = true, dropNullKeys = true, indent = "")
        json.pretty(jsonPrinter)
      }

      prettyJson(jsonResult.right.value) shouldBe prettyJson(expectedJsonResult)
    }

    it("We get an Ok response") {
      result.header.status shouldBe OK
    }

    it("The primary port is called") {
      state.value shouldBe FindTransactionsPageParameters(testUserId, 1, 5)
    }
  }
}
