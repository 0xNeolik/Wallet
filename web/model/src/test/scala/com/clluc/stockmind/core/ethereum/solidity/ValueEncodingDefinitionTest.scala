package com.clluc.stockmind.core.ethereum.solidity

import com.clluc.stockmind.core.ethereum.solidity.AbiBlock.OrderedBlock
import com.clluc.stockmind.core.ethereum.solidity.ValueEncodingDefinition.EncodingDefinition
import org.scalatest.{FlatSpec, Matchers}

class ValueEncodingDefinitionTest extends FlatSpec with Matchers {

  "AbiBlock" should "generate a list of OrderedBlock from a list of EncodingDefinition - static types" in {
    val definition: List[EncodingDefinition] = List(Uint(256, 1000).asBlocks)
    val orderedBlocks: List[OrderedBlock]    = List(Left(LiteralAbiBlock(definition.head.left.get)))
    AbiBlock.fromEncodingDefinition(definition) shouldBe orderedBlocks
  }

  it should "generate a list of OrderedBlock from a list of EncodingDefinition - dynamic types" in {
    val definition: List[EncodingDefinition] =
      List(SolidityString("dave").asBlocks, SolidityString("diane").asBlocks)
    val orderedBlocks: List[OrderedBlock] = List(
      Right(ReferenceToBlock(0)),
      Right(ReferenceToBlock(1)),
      Left(LiteralAbiBlock(definition(0).right.get(0), Some(0))),
      Left(LiteralAbiBlock(definition(0).right.get(1))),
      Left(LiteralAbiBlock(definition(1).right.get(0), Some(1))),
      Left(LiteralAbiBlock(definition(1).right.get(1)))
    )
    AbiBlock.fromEncodingDefinition(definition) shouldBe orderedBlocks
  }

  it should "generate a list of OrderedBlock from a list of EncodingDefinition - mixed types" in {
    val definition: List[EncodingDefinition] =
      List(SolidityString("dave"), Uint(256, 1000), SolidityString("diane")).map(_.asBlocks)
    val orderedBlocks: List[OrderedBlock] = List(
      Right(ReferenceToBlock(0)),
      Left(LiteralAbiBlock(definition(1).left.get)),
      Right(ReferenceToBlock(1)),
      Left(LiteralAbiBlock(definition(0).right.get(0), Some(0))),
      Left(LiteralAbiBlock(definition(0).right.get(1))),
      Left(LiteralAbiBlock(definition(2).right.get(0), Some(1))),
      Left(LiteralAbiBlock(definition(2).right.get(1)))
    )
    AbiBlock.fromEncodingDefinition(definition) shouldBe orderedBlocks
  }

  it should "resolve references to other blocks" in {
    val orderedBlocks: List[OrderedBlock] = List(
      Right(ReferenceToBlock(42)),
      Left(LiteralAbiBlock(Uint(256, 1).asBlocks.left.get, Some(42)))
    )
    val resolvedBlocks: List[LiteralAbiBlock] = List(
      LiteralAbiBlock(Uint(256, 32).asBlocks.left.get),
      LiteralAbiBlock(Uint(256, 1).asBlocks.left.get, Some(42))
    )
    AbiBlock.resolveReferences(orderedBlocks) shouldBe resolvedBlocks
  }
}
