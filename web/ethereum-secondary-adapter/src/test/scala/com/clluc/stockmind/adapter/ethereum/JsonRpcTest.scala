package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.{EthCall, EthFilter, EthTransaction, SignableTransaction}
import io.circe.Printer
import org.scalatest.{FlatSpec, Matchers}

class JsonRpcTest extends FlatSpec with Matchers {

  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe.shapes._

  trait Fixtures {
    val address  = "0x73f1CbAa49aDF6377Fee3e0e8B6E5aEDb2211175"
    val password = "hunter2"
    val data     = "0x7c80fe55c4b039ad1d31b6b31e04db1b5d89dc0a4e89bb999939bd74e8aac934"
    val block    = "0x1f40"
  }

  private val printer = Printer(preserveOrder = true, dropNullKeys = true, indent = "")

  behavior of "JsonRpc"

  it should "generate a personal_newAccount request" in new Fixtures {
    val request = JsonRpc.newAccount(password)

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"personal_newAccount","params":["$password"],"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a eth_blockNumber request" in new Fixtures {
    val request = JsonRpc.blockNumber()
    // 'params' is encoded to an empty object instead of as an empty array.
    // Geth has no problems with this as for now.
    val expectedJson = s"""{"jsonrpc":"2.0","method":"eth_blockNumber","params":{},"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a eth_sign request" in new Fixtures {
    val message = "hola"
    val request = JsonRpc.signMessage(address, message)

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"eth_sign","params":["$address","$message"],"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a personal_sendTransaction" in new Fixtures {

    val request = JsonRpc.sendTransaction(
      SignableTransaction(
        EthTransaction(address, Some(address), data),
        password
      )
    )

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"personal_sendTransaction","params":[{"from":"$address","to":"$address","data":"$data","value":"0x0"},"$password"],"id":"42"}"""
    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a personal_signAndSendTransaction" in new Fixtures {

    val request = JsonRpc.signAndSendTransaction(
      SignableTransaction(
        EthTransaction(address, Some(address), data),
        password
      )
    )

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"personal_signAndSendTransaction","params":[{"from":"$address","to":"$address","data":"$data","value":"0x0"},"$password"],"id":"42"}"""
    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a eth_getLogs request" in new Fixtures {
    val request = JsonRpc.getLogs(EthFilter(block, "latest", List(address), List(List(data))))

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"eth_getLogs","params":[{"fromBlock":"$block","toBlock":"latest","address":["$address"],"topics":[["$data"]]}],"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a eth_call request" in new Fixtures {
    val request = JsonRpc.callMethod(EthCall(address, data))

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"eth_call","params":[{"to":"$address","data":"$data"},"latest"],"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a eth_getBalance request" in new Fixtures {
    val request = JsonRpc.getBalance(address)

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"eth_getBalance","params":["$address","latest"],"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

  it should "generate a eth_getBlockByNumber request" in new Fixtures {
    val request = JsonRpc.getBlockByNumber(block)

    val expectedJson =
      s"""{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":["$block",true],"id":"42"}"""

    request.asJson.pretty(printer) shouldBe expectedJson
  }

}
