package com.clluc.stockmind.core.tokens

import java.sql.SQLException

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import TokenCreationOps.ErrorConstructors
import com.clluc.stockmind.core.Generators
import com.clluc.stockmind.core.ethereum.Block
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.UnexpectedEthereumResponse
import com.clluc.stockmind.core.ethereum.solidity.{Address}
import com.clluc.stockmind.port.secondary.{Erc20InfoPort, Erc721InfoPort, EthereumClientPort}
import scala.concurrent.Future

class TokenCreationOpsImplTest
    extends AsyncFlatSpec
    with Matchers
    with EitherValues
    with AsyncMockFactory {

  val errors = ErrorConstructors

  // Fixture traits don't work with async tests.

  val loggedEvent = Generators.genLoggedEvent.sample.get

  val infoPort       = mock[Erc20InfoPort]
  val erc721InfoPort = mock[Erc721InfoPort]
  val ethClientPort  = mock[EthereumClientPort]

  val ops = new TokenCreationOpsImpl(infoPort, erc721InfoPort, ethClientPort)

  behavior of "getTokenAddress"

  it should "extract the token address from a LoggedEvent" in {
    val address = Generators.genAddress.sample.get
    val event   = loggedEvent.copy(data = s"0x$address")

    ops.getTokenAddress(event).value.map { result =>
      result.right.value.value shouldBe address
    }
  }

  it should "fail with the appropriate error if the token address cannot be extracted" in {
    val event = loggedEvent.copy(data = "0x1")
    val error = errors.cannotDecodeAddress("0x1")

    ops.getTokenAddress(event).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "getTokenOwnerAddress"

  it should "extract the token owner address from a LoggedEvent" in {
    val address = Generators.genAddress.sample.get
    val event = {
      val topics = loggedEvent.topics.updated(1, s"0x$address")
      loggedEvent.copy(topics = topics)
    }
    ops.getTokenOwnerAddress(event).value.map { result =>
      result.right.value.value shouldBe address
    }
  }

  it should "fail with the appropriate error if the token owner address cannot be extracted" in {
    val event = {
      val topics = loggedEvent.topics.updated(1, "0x1")
      loggedEvent.copy(topics = topics)
    }
    val error = errors.cannotDecodeAddress("0x1")

    ops.getTokenOwnerAddress(event).value.map { result =>
      result.left.value shouldBe error
    }
  }

  behavior of "getTokenBirthBlock"

  it should "extract the token birth Block" in {
    val block = Block(Generators.genNumber.sample.get)
    val event = loggedEvent.copy(block = block)

    ops.getTokenBirthBlock(event).value.map { result =>
      result.right.value shouldBe block
    }
  }

  behavior of "getTokenName"

  it should "extract the token name from the token contract" in {
    val address = Address(Generators.genAddress.sample.get)
    val encodedResponse = "0x" +
      "0000000000000000000000000000000000000000000000000000000000000020" +
      "0000000000000000000000000000000000000000000000000000000000000003" +
      "6162630000000000000000000000000000000000000000000000000000000000"
    val name = "abc"

    (ethClientPort.callMethod _).expects(*).returning(Future(Right(encodedResponse)))

    ops.getTokenName(address).value.map { result =>
      result.right.value shouldBe name
    }
  }

  it should "fail with the appropriate error if the Ethereum client returns an error" in {
    val address = Address(Generators.genAddress.sample.get)

    (ethClientPort.callMethod _)
      .expects(*)
      .returning(Future(Left(UnexpectedEthereumResponse("fail", 500))))

    ops.getTokenName(address).value.map { result =>
      result.left.value shouldBe errors.ethereumClientError(500, "fail")
    }
  }

  it should "fail with the appropriate error if the response cannot be interpreted" in {
    val address = Address(Generators.genAddress.sample.get)

    (ethClientPort.callMethod _).expects(*).returning(Future(Right("woops")))

    ops.getTokenName(address).value.map { result =>
      result.left.value shouldBe errors.cannotDecodeResult("")
    }
  }

  behavior of "getTokenSymbol"

  it should "extract the token symbol from the token contract" in {
    val address = Address(Generators.genAddress.sample.get)
    val encodedResponse = "0x" +
      "0000000000000000000000000000000000000000000000000000000000000020" +
      "0000000000000000000000000000000000000000000000000000000000000003" +
      "6162630000000000000000000000000000000000000000000000000000000000"
    val symbol = "abc"

    (ethClientPort.callMethod _).expects(*).returning(Future(Right(encodedResponse)))

    ops.getTokenSymbol(address).value.map { result =>
      result.right.value shouldBe symbol
    }
  }

  it should "fail with the appropriate error if the Ethereum client returns an error" in {
    val address = Address(Generators.genAddress.sample.get)

    (ethClientPort.callMethod _)
      .expects(*)
      .returning(Future(Left(UnexpectedEthereumResponse("fail", 500))))

    ops.getTokenSymbol(address).value.map { result =>
      result.left.value shouldBe errors.ethereumClientError(500, "fail")
    }
  }

  it should "fail with the appropriate error if the response cannot be interpreted" in {
    val address = Address(Generators.genAddress.sample.get)

    (ethClientPort.callMethod _).expects(*).returning(Future(Right("woops")))

    ops.getTokenSymbol(address).value.map { result =>
      result.left.value shouldBe errors.cannotDecodeResult("")
    }
  }

  behavior of "getTokenDecimals"

  it should "extract the token symbol from the token contract" in {
    val address = Address(Generators.genAddress.sample.get)
    val encodedResponse = "0x" +
      "0000000000000000000000000000000000000000000000000000000000000012"
    val decimals = 18

    (ethClientPort.callMethod _).expects(*).returning(Future(Right(encodedResponse)))

    ops.getTokenDecimals(address).value.map { result =>
      result.right.value shouldBe decimals
    }
  }

  it should "fail with the appropriate error if the Ethereum client returns an error" in {
    val address = Address(Generators.genAddress.sample.get)

    (ethClientPort.callMethod _)
      .expects(*)
      .returning(Future(Left(UnexpectedEthereumResponse("fail", 500))))

    ops.getTokenDecimals(address).value.map { result =>
      result.left.value shouldBe errors.ethereumClientError(500, "fail")
    }
  }

  it should "fail with the appropriate error if the response cannot be interpreted" in {
    val address = Address(Generators.genAddress.sample.get)

    (ethClientPort.callMethod _).expects(*).returning(Future(Right("0xnope")))

    ops.getTokenDecimals(address).value.map { result =>
      result.left.value shouldBe errors.cannotDecodeResult("nope")
    }
  }

  behavior of "writeErc20Token"

  it should "write a erc20 token in the DB" in {
    val token = Generators.genEthereumToken.sample.get

    (infoPort.createEthereumToken _).expects(token).returning(Future(token))

    ops.writeErc20Token(token).value.map { result =>
      result.right.value shouldBe token
    }
  }

  behavior of "writeErc721Token"

  it should "write a erc721 token in the DB" in {
    //val token    = Generators.genEthereumToken721.sample.get
    val ethtoken = Generators.genEthToken.sample.get

    (erc721InfoPort.create721CollectionToken _)
      .expects(ethtoken)
      .returning(Future(ethtoken))
    ops.writeErc721Token(ethtoken).value.map { result =>
      result.right.value shouldBe ethtoken
    }
  }

  it should "fail with the appropriate error if the token cannot be written" in {
    val token     = Generators.genEthereumToken.sample.get
    val dbFailure = Future.failed(new SQLException("db fail"))
    val error     = errors.databaseError("db fail")

    (infoPort.createEthereumToken _).expects(token).returning(dbFailure)

    ops.writeErc20Token(token).value.map { result =>
      result.left.value shouldBe error
    }
  }

}
