package com.clluc.stockmind.core.ethereum.solidity

import com.clluc.stockmind.core.ethereum.solidity.ValueEncodingDefinition.EncodingDefinition
import org.scalatest.{FlatSpec, Matchers}

class ValueTypeTest extends FlatSpec with Matchers {

  "A StaticType" should "be transformed into blocks" in {
    val uint                               = Uint(256, 1000)
    val blockContent                       = ("0" * 61) + "3e8"
    val expectedResult: EncodingDefinition = Left(ParamBlock(blockContent))
    uint.asBlocks shouldBe expectedResult
  }

  "A DynamicType" should "be transformed into blocks - one content block" in {
    val dave                = SolidityString("dave")
    val headerContent       = ("0" * 63) + "4"
    val paddedEncodedString = "64617665" + ("0" * 56)
    val expectedResult      = Right(List(ParamBlock(headerContent), ParamBlock(paddedEncodedString)))
    dave.asBlocks shouldBe expectedResult
  }

  "A DynamicType" should "be transformed into blocks - multiple content blocks" in {
    val dave               = SolidityString("dave" * 9)
    val headerContent      = ("0" * 62) + "24" // 0x24 == 36
    val firstBlockContent  = "64617665" * 8
    val secondBlockContent = "64617665" + ("0" * 56)
    val expectedResult = Right(
      List(ParamBlock(headerContent),
           ParamBlock(firstBlockContent),
           ParamBlock(secondBlockContent)))
    dave.asBlocks shouldBe expectedResult
  }

}
