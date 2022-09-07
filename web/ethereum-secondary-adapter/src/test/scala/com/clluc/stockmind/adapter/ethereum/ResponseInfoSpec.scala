package com.clluc.stockmind.adapter.ethereum

import com.clluc.stockmind.core.ethereum.JsonRpcResponse.UnexpectedEthereumResponse
import com.clluc.stockmind.core.ethereum.{Block, EthereumHash, LoggedEvent, Transaction}
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class ResponseInfoSpec extends FlatSpec with Matchers with EitherValues {

  // Note: All JSON documents that emulate Ethereum JSON RPC responses are
  // copied from actual calls.

  implicit class ResponseInfoString(s: String) {
    def asResponseInfo(status: Int = 200) = ResponseInfo(s, status)
  }

  "A simple result field" should "be properly parsed" in {
    val resp     = """{"jsonrpc":"2.0","result":"0xae9ecad8785000","id":42}""".asResponseInfo()
    val expected = "0xae9ecad8785000"
    resp.result.right.value shouldBe expected
  }

  "An address result field" should "be properly parsed" in {
    val resp = """{"jsonrpc":"2.0","result":"0x8c3558e24731be9171a51ecede19d6b3abd85e4f","id":42}"""
      .asResponseInfo()
    val expected = Address("8c3558e24731be9171a51ecede19d6b3abd85e4f")
    resp.address.right.value shouldBe expected
  }

  "A numeric result field" should "be properly parsed" in {
    val resp     = """{"jsonrpc":"2.0","result":"0x4e20","id":42}""".asResponseInfo()
    val expected = BigInt(20000)
    resp.balance.right.value shouldBe expected
  }

  it should "be parsed even when there are extra zeros" in {
    val resp =
      """{"jsonrpc":"2.0","result":"0x0000000000000000000000000000000000000000000000000000000000004e20","id":42}"""
        .asResponseInfo()
    val expected = BigInt(20000)
    resp.balance.right.value shouldBe expected
  }

  "A full block info response" should "be properly read to extract transaction data" in {

    // Response from following request in Ropsten network
    //{
    //  "method": "eth_getBlockByNumber",
    //  "params": ["0x1f7f28", true],
    //  "id": 1,
    //  "jsonrpc": "2.0"
    //}
    val resp =
      """
        |{
        |    "jsonrpc": "2.0",
        |    "result": {
        |        "author": "0x0290f96ba3471352c494f03c9e90d43ebfcdb4e9",
        |        "difficulty": "0x5ff035ea",
        |        "extraData": "0xd5830108028650617269747986312e32312e30826c69",
        |        "gasLimit": "0x47b784",
        |        "gasUsed": "0x476356",
        |        "hash": "0xf64bb43d413b8c21b6b99c540bac8f07649b7880aa8b09ddb62cb2b1d7cefbce",
        |        "logsBloom": "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
        |        "miner": "0x0290f96ba3471352c494f03c9e90d43ebfcdb4e9",
        |        "mixHash": "0x72fa76a8edbe608074d7db309e05ef09e42643c1c00a4944b95c85ae241eb5a8",
        |        "nonce": "0x3ec04105e5e79396",
        |        "number": "0x1f7f28",
        |        "parentHash": "0xf1926668fb671acf77462e16f2e9716db6d7577495b4385290849437a4972212",
        |        "receiptsRoot": "0xb6868d015e77bd3c227427f40ff806dd171f22ad73b35fb8b6561c9bba658767",
        |        "sealFields": [
        |            "0xa072fa76a8edbe608074d7db309e05ef09e42643c1c00a4944b95c85ae241eb5a8",
        |            "0x883ec04105e5e79396"
        |        ],
        |        "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
        |        "size": "0x63f",
        |        "stateRoot": "0x095c3c01c62dd1a616aa608aec62a7321c0ce02da6b0fce65b90a7f6561883a3",
        |        "timestamp": "0x5a09d583",
        |        "totalDifficulty": "0x17d501fad82635",
        |        "transactions": [
        |            {
        |                "blockHash": "0xf64bb43d413b8c21b6b99c540bac8f07649b7880aa8b09ddb62cb2b1d7cefbce",
        |                "blockNumber": "0x1f7f28",
        |                "chainId": null,
        |                "condition": null,
        |                "creates": null,
        |                "from": "0x0718197b9ac69127381ed0c4b5d0f724f857c4d1",
        |                "gas": "0x186a0",
        |                "gasPrice": "0x2540be400",
        |                "hash": "0x1b088e43f2ba57b8d84824ab0846d594b60dc6fda3bf076aa11be6ac97273b34",
        |                "input": "0xfedc2a280000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000007726f707374656e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000900000000000000000000000033f41405ee989f8a64ae3032b94fb6003db053b800000000000000000000000025e1f8c062af1e29e3a45862f52adacae52ac3de00000000000000000000000055ac7548c227ced26e386b24c0eaefb74b5a3248000000000000000000000000b72a2863dba36a743f4c1dd8b59597a4ce9f5b570000000000000000000000009a2b2c516b23773e5e16e6075f632fe168fcd4fc0000000000000000000000000e102f0aad2dc56b5c07f4ddee7c96d7753a2027000000000000000000000000cd754f715bef4d8fd01c43bb69b7d795428bc280000000000000000000000000ee4a098f0d29dec9d84cb267bbf285a500392d79000000000000000000000000cbe3ccc3a1da5ac6b7a9f3d5115faa26ce2c8009",
        |                "nonce": "0x6188",
        |                "publicKey": "0x9a2e59037441799aba28f2953aeaea584eb3d01e221d8838ccde52fb7083b907e3502d3c315adce3b9a9b3e8bd2f0e05aed78de86560d776a98d1cb7110d54f0",
        |                "r": "0x4ce87d443bf6e21f3d974387abff33a7df48137a1648b11a36224f8f54c25e04",
        |                "raw": "0xf9024d8261888502540be400830186a094d9fb98acf05136196e168a3914c4b151e223629680b901e4fedc2a280000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000007726f707374656e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000900000000000000000000000033f41405ee989f8a64ae3032b94fb6003db053b800000000000000000000000025e1f8c062af1e29e3a45862f52adacae52ac3de00000000000000000000000055ac7548c227ced26e386b24c0eaefb74b5a3248000000000000000000000000b72a2863dba36a743f4c1dd8b59597a4ce9f5b570000000000000000000000009a2b2c516b23773e5e16e6075f632fe168fcd4fc0000000000000000000000000e102f0aad2dc56b5c07f4ddee7c96d7753a2027000000000000000000000000cd754f715bef4d8fd01c43bb69b7d795428bc280000000000000000000000000ee4a098f0d29dec9d84cb267bbf285a500392d79000000000000000000000000cbe3ccc3a1da5ac6b7a9f3d5115faa26ce2c80091ca04ce87d443bf6e21f3d974387abff33a7df48137a1648b11a36224f8f54c25e04a0039f612f76fb55f77c7582d0f725fbdf2ccd320e9716ee2a09d6679315a5331b",
        |                "s": "0x39f612f76fb55f77c7582d0f725fbdf2ccd320e9716ee2a09d6679315a5331b",
        |                "standardV": "0x1",
        |                "to": "0xd9fb98acf05136196e168a3914c4b151e2236296",
        |                "transactionIndex": "0x0",
        |                "v": "0x1c",
        |                "value": "0x0"
        |            },
        |            {
        |                "blockHash": "0xf64bb43d413b8c21b6b99c540bac8f07649b7880aa8b09ddb62cb2b1d7cefbce",
        |                "blockNumber": "0x1f7f28",
        |                "chainId": null,
        |                "condition": null,
        |                "creates": null,
        |                "from": "0x77f71e7c2be802cc7f531d177647e07163c9c756",
        |                "gas": "0x44218a",
        |                "gasPrice": "0xee6b2805",
        |                "hash": "0xf30dff21dc194c964dbb7fbb26190da29c37f9166caf9ce1fd04dd2f03403f12",
        |                "input": "0x",
        |                "nonce": "0x1eba",
        |                "publicKey": "0x7f9b7c25907cce9a215e7dcc7441796d25a84c04ec5f8b7fefc14c4e4ab1cbde748f7845c4531840069d0ee724750e26f7ebdeca5c0a5e139241733c5d89fe98",
        |                "r": "0xc73aca529b5e2d2f5c11406c0ab797dd74bde9ca4340f9793c353ca17359474c",
        |                "raw": "0xf866821eba84ee6b28058344218a94675828c833a33c6f808adcc6e08e397c8da855ac80801ca0c73aca529b5e2d2f5c11406c0ab797dd74bde9ca4340f9793c353ca17359474ca07836a0808a650b7d9d2ed6d2aa3ee0d9df8e5e3e07086ad49a5ecec9e7401f9f",
        |                "s": "0x7836a0808a650b7d9d2ed6d2aa3ee0d9df8e5e3e07086ad49a5ecec9e7401f9f",
        |                "standardV": "0x1",
        |                "to": "0x675828c833a33c6f808adcc6e08e397c8da855ac",
        |                "transactionIndex": "0x1",
        |                "v": "0x1c",
        |                "value": "0x0"
        |            },
        |            {
        |                "blockHash": "0xf64bb43d413b8c21b6b99c540bac8f07649b7880aa8b09ddb62cb2b1d7cefbce",
        |                "blockNumber": "0x1f7f28",
        |                "chainId": "0x3",
        |                "condition": null,
        |                "creates": null,
        |                "from": "0xbbf5029fd710d227630c8b7d338051b8e76d50b3",
        |                "gas": "0xc350",
        |                "gasPrice": "0xee6b2800",
        |                "hash": "0x2d58520e596231da6342babbd509a28b272a9436fcb1b2516fca762e3d81a832",
        |                "input": "0x",
        |                "nonce": "0x2d814",
        |                "publicKey": "0x2631a3db289ec82bb8b7eb856e4a4153330db1f22ddabfe091f6eaee2aab851856f75aaf8d7d183b6839affde043298d6fd8eab5aa83c1b47efa36dd181448c1",
        |                "r": "0xd041334d2b2ce3c42918260c129d86724b848436d3a54f6b31451af047d802f0",
        |                "raw": "0xf8668302d81484ee6b280082c3509400000000000000000000000000000000000000140d8029a0d041334d2b2ce3c42918260c129d86724b848436d3a54f6b31451af047d802f0a0756a988f374da8ec760e6ce70f2c288503bc14880b6deafc3de63a45d14dc234",
        |                "s": "0x756a988f374da8ec760e6ce70f2c288503bc14880b6deafc3de63a45d14dc234",
        |                "standardV": "0x0",
        |                "to": "0x0000000000000000000000000000000000000014",
        |                "transactionIndex": "0x2",
        |                "v": "0x29",
        |                "value": "0xd"
        |            },
        |            {
        |                "blockHash": "0xf64bb43d413b8c21b6b99c540bac8f07649b7880aa8b09ddb62cb2b1d7cefbce",
        |                "blockNumber": "0x1f7f28",
        |                "chainId": "0x3",
        |                "condition": null,
        |                "creates": "0x2f477c6a896ac93c651a72304e46d2f84a690673",
        |                "from": "0x27bb11b5ff3295272b9e13a648d565cfd57c589d",
        |                "gas": "0x16924",
        |                "gasPrice": "0x77359400",
        |                "hash": "0x20ff5117010b989095c48e8ef700ceeff9065f8841dd87fca3748c2e01aee5bc",
        |                "input": "0x60606040523415600e57600080fd5b609380601b6000396000f30060606040523415600e57600080fd5b7ffd55d4456e7e5dcc9519b5525583c43cf9c7213c0d06a41c488aff5b65319f3660405180807f48656c6c6f20576f726c64210000000000000000000000000000000000000000815250602001905060405180910390a10000a165627a7a72305820981300f9ffd84263f6ac53c10de1f84b2d71f478c0871f52ee22cca8cda6d3ae0029",
        |                "nonce": "0x1",
        |                "publicKey": "0x078f58a99179d357e635f3717c610bd6c1337172714ce58cc6be24dacb6cbb082b6536ff84095f47397bbebf97ea0429fd690af33b4063b2e4f5e0b2bb5cbcce",
        |                "r": "0x8730ca6f123ec966f3862cc428bd332efd7011f6504bbf70e368e54b253a0f52",
        |                "raw": "0xf8ff018477359400830169248080b8ae60606040523415600e57600080fd5b609380601b6000396000f30060606040523415600e57600080fd5b7ffd55d4456e7e5dcc9519b5525583c43cf9c7213c0d06a41c488aff5b65319f3660405180807f48656c6c6f20576f726c64210000000000000000000000000000000000000000815250602001905060405180910390a10000a165627a7a72305820981300f9ffd84263f6ac53c10de1f84b2d71f478c0871f52ee22cca8cda6d3ae00292aa08730ca6f123ec966f3862cc428bd332efd7011f6504bbf70e368e54b253a0f52a067e2154022ef82c9bd5c06df0eae12a7f9744a4b6f7d62230403910466d9ad23",
        |                "s": "0x67e2154022ef82c9bd5c06df0eae12a7f9744a4b6f7d62230403910466d9ad23",
        |                "standardV": "0x1",
        |                "to": null,
        |                "transactionIndex": "0x3",
        |                "v": "0x2a",
        |                "value": "0x0"
        |            }
        |        ],
        |        "transactionsRoot": "0xdc0cee9f8f03e184af65ea43d900f75e76b3fa3288426345b2b0365c3cd15f18",
        |        "uncles": []
        |    },
        |    "id": 1
        |}
      """.stripMargin
        .asResponseInfo()

    val expected = List(
      Transaction(
        hash = EthereumHash("1b088e43f2ba57b8d84824ab0846d594b60dc6fda3bf076aa11be6ac97273b34"),
        from = Address("0718197b9ac69127381ed0c4b5d0f724f857c4d1"),
        to = Some(Address("d9fb98acf05136196e168a3914c4b151e2236296")),
        blockNumber = Block(2064168),
        txIndex = 0,
        value = Uint(256, 0L)
      ),
      Transaction(
        hash = EthereumHash("f30dff21dc194c964dbb7fbb26190da29c37f9166caf9ce1fd04dd2f03403f12"),
        from = Address("77f71e7c2be802cc7f531d177647e07163c9c756"),
        to = Some(Address("675828c833a33c6f808adcc6e08e397c8da855ac")),
        blockNumber = Block(2064168),
        txIndex = 1,
        value = Uint(256, 0L)
      ),
      Transaction(
        hash = EthereumHash("2d58520e596231da6342babbd509a28b272a9436fcb1b2516fca762e3d81a832"),
        from = Address("bbf5029fd710d227630c8b7d338051b8e76d50b3"),
        to = Some(Address("0000000000000000000000000000000000000014")),
        blockNumber = Block(2064168),
        txIndex = 2,
        value = Uint(256, 13L)
      ),
      Transaction(
        hash = EthereumHash("20ff5117010b989095c48e8ef700ceeff9065f8841dd87fca3748c2e01aee5bc"),
        from = Address("27bb11b5ff3295272b9e13a648d565cfd57c589d"),
        to = None,
        blockNumber = Block(2064168),
        txIndex = 3,
        value = Uint(256, 0L)
      )
    )

    resp.transactions.right.value shouldBe Some(expected)
  }

  "A get logged events response" should "be properly parsed" in {
    val resp =
      """{"jsonrpc":"2.0",
        |"result":[
        |{"address":"0x8c3558e24731be9171a51ecede19d6b3abd85e4f","blockHash":"0x548f4ac3894a313f6a13c52462d52c49529117904c9dc4dc945ade6f49157475","blockNumber":"0x12f952","data":"0x0000000000000000000000000000000000000000000000000000000000004e20","logIndex":"0x7",
        |"topics":["0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef","0x000000000000000000000000bab4ce625324b7dc24ebca66794653380042452d","0x0000000000000000000000007a1d06d3f253135d7cbe55aa6d2c22442b1e17db"],
        |"transactionHash":"0x8fa2f3adc2e0124ef012af1096c83a63d9553be606765048d849932dd13eb0f7","transactionIndex":"0x8","transactionLogIndex":"0x0","type":"mined"},
        |{"address":"0x8c3558e24731be9171a51ecede19d6b3abd85e4f","blockHash":"0xce5ea1fab4b22f50c84ca093da12524534a2ba59fde3c9b1e536f6baadf6339a","blockNumber":"0x12f954","data":"0x0000000000000000000000000000000000000000000000000000000000004e20","logIndex":"0x0",
        |"topics":["0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef","0x0000000000000000000000007a1d06d3f253135d7cbe55aa6d2c22442b1e17db","0x000000000000000000000000ca58e62ade4339fc132757e5b972721c26354e64"],
        |"transactionHash":"0xc43bdbf4959e1c89dce0c2297531f0c2d14e07bbb8049050049bc5c809fa0ea5","transactionIndex":"0xf","transactionLogIndex":"0x0","type":"mined"}],"id":42}""".stripMargin
        .asResponseInfo()

    val expected = List(
      LoggedEvent(
        Address("8c3558e24731be9171a51ecede19d6b3abd85e4f"),
        Block(1243474),
        List(
          "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
          "0x000000000000000000000000bab4ce625324b7dc24ebca66794653380042452d",
          "0x0000000000000000000000007a1d06d3f253135d7cbe55aa6d2c22442b1e17db"
        ),
        "0x0000000000000000000000000000000000000000000000000000000000004e20",
        EthereumHash("8fa2f3adc2e0124ef012af1096c83a63d9553be606765048d849932dd13eb0f7"),
        8,
        EthereumHash("548f4ac3894a313f6a13c52462d52c49529117904c9dc4dc945ade6f49157475")
      ),
      LoggedEvent(
        Address("8c3558e24731be9171a51ecede19d6b3abd85e4f"),
        Block(1243476),
        List(
          "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
          "0x0000000000000000000000007a1d06d3f253135d7cbe55aa6d2c22442b1e17db",
          "0x000000000000000000000000ca58e62ade4339fc132757e5b972721c26354e64"
        ),
        "0x0000000000000000000000000000000000000000000000000000000000004e20",
        EthereumHash("c43bdbf4959e1c89dce0c2297531f0c2d14e07bbb8049050049bc5c809fa0ea5"),
        15,
        EthereumHash("ce5ea1fab4b22f50c84ca093da12524534a2ba59fde3c9b1e536f6baadf6339a")
      )
    )

    resp.events.right.value shouldBe expected
  }

  "Non valid JSON responses" should "return an error" in {
    val resp = "Something that is not JSON".asResponseInfo()
    resp.result.left.value shouldBe UnexpectedEthereumResponse(resp.body, resp.status)
  }

  "Responses with non-200 HTTP codes" should "be valid within the 2xx group" in {
    val expected = "0xae9ecad8785000"
    val resp201  = """{"jsonrpc":"2.0","result":"0xae9ecad8785000","id":42}""".asResponseInfo(201)
    resp201.result.right.value shouldBe expected
    val resp299 = """{"jsonrpc":"2.0","result":"0xae9ecad8785000","id":42}""".asResponseInfo(299)
    resp299.result.right.value shouldBe expected
  }

  it should "be invalid with non-2xx codes" in {
    val resp199 = """{"jsonrpc":"2.0","result":"0xae9ecad8785000","id":42}""".asResponseInfo(199)
    resp199.result.left.value shouldBe UnexpectedEthereumResponse(resp199.body, resp199.status)
    val resp300 = """{"jsonrpc":"2.0","result":"0xae9ecad8785000","id":42}""".asResponseInfo(300)
    resp300.result.left.value shouldBe UnexpectedEthereumResponse(resp300.body, resp300.status)
    val resp404 = """{"jsonrpc":"2.0","result":"0xae9ecad8785000","id":42}""".asResponseInfo(404)
    resp404.result.left.value shouldBe UnexpectedEthereumResponse(resp404.body, resp404.status)
  }

}
