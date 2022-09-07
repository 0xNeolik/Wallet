package com.clluc.stockmind.controller

import com.clluc.stockmind.controller.SendTransactionController.{
  parseRequestContent,
  SendTransactionPostData
}
import org.scalatest.{FlatSpec, Matchers}

class SendTransactionControllerParseRequestBodySpec extends FlatSpec with Matchers {
  "A proper JSON content with only mandatory info" should "be properly parsed" in {
    val inputJson = """{
                      |"destination":"ARandomDestination",
                      |"tokenSymbol":"ARandomTokenSymbol",
                      |"amount":"ARandomAmount"
                      |}""".stripMargin

    val parsingResult = parseRequestContent(inputJson)

    parsingResult shouldBe Right(
      SendTransactionPostData(
        "ARandomDestination",
        "ARandomTokenSymbol",
        "ARandomAmount",
        None
      )
    )
  }

  "A proper JSON content with mandatory and optional info" should "be properly parsed" in {
    val inputJson = """{
                      |  "destination":"ARandomDestination",
                      |  "tokenSymbol":"ARandomTokenSymbol",
                      |  "amount":"ARandomAmount",
                      |  "metaInf": {
                      |    "Att1":"Something",
                      |    "Att2":"Something else"
                      |  }
                      |}""".stripMargin

    val parsingResult = parseRequestContent(inputJson)

    parsingResult shouldBe Right(
      SendTransactionPostData(
        "ARandomDestination",
        "ARandomTokenSymbol",
        "ARandomAmount",
        Some(
          Map(
            "Att1" -> "Something",
            "Att2" -> "Something else"
          )
        )
      )
    )
  }

  "A wrong JSON content without destination info" should "give us a parsing error" in {
    val inputJson = """{
                      |  "tokenSymbol":"ARandomTokenSymbol",
                      |  "amount":"ARandomAmount",
                      |  "metaInf": {
                      |    "Att1":"Something",
                      |    "Att2":"Something else"
                      |  }
                      |}""".stripMargin

    val parsingResult = parseRequestContent(inputJson)

    parsingResult should be('left)
  }

  "A wrong JSON content without token info" should "give us a parsing error" in {
    val inputJson = """{
                      |  "destination":"ARandomDestination",
                      |  "amount":"ARandomAmount",
                      |  "metaInf": {
                      |    "Att1":"Something",
                      |    "Att2":"Something else"
                      |  }
                      |}""".stripMargin

    val parsingResult = parseRequestContent(inputJson)

    parsingResult should be('left)
  }

  "A wrong JSON content without amount info" should "give us a parsing error" in {
    val inputJson = """{
                      |  "destination":"ARandomDestination",
                      |  "tokenSymbol":"ARandomTokenSymbol",
                      |  "metaInf": {
                      |    "Att1":"Something",
                      |    "Att2":"Something else"
                      |  }
                      |}""".stripMargin

    val parsingResult = parseRequestContent(inputJson)

    parsingResult should be('left)
  }

  "A wrong JSON content with some extra, not expected data" should "be parsed ignoring the extra data" in {
    val inputJson = """{
                      |  "destination":"ARandomDestination",
                      |  "tokenSymbol":"ARandomTokenSymbol",
                      |  "amount":"ARandomAmount",
                      |  "metaInf": {
                      |    "Att1":"Something",
                      |    "Att2":"Something else"
                      |  },
                      |  "extraInfo":"ThisShouldBeIgnored"
                      |}""".stripMargin

    val parsingResult = parseRequestContent(inputJson)

    parsingResult shouldBe Right(
      SendTransactionPostData(
        "ARandomDestination",
        "ARandomTokenSymbol",
        "ARandomAmount",
        Some(
          Map(
            "Att1" -> "Something",
            "Att2" -> "Something else"
          )
        )
      )
    )
  }
}
