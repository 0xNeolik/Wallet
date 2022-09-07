package com.clluc.stockmind.core.ethereum.solidity

private[solidity] case class ParamBlock(content: String) {
  require(content.length == 64)
}

private[ethereum] object ValueEncodingDefinition {

  type StaticEncoding     = ParamBlock
  type DynamicEncoding    = List[ParamBlock]
  type EncodingDefinition = Either[StaticEncoding, DynamicEncoding]

  def fromValues(values: List[ValueType]): List[EncodingDefinition] =
    values.map(_.asBlocks)
}

private[ethereum] sealed trait AbiBlock
private[ethereum] case class LiteralAbiBlock(content: ParamBlock, id: Option[Int] = None)
    extends AbiBlock
private[ethereum] case class ReferenceToBlock(blockId: Int) extends AbiBlock

private[ethereum] object AbiBlock {

  type OrderedBlock = Either[LiteralAbiBlock, ReferenceToBlock]

  import ValueEncodingDefinition.EncodingDefinition

  def fromEncodingDefinition(definition: List[EncodingDefinition]): List[OrderedBlock] = {
    val (staticBlocks, dynamicBlocks, _) =
      definition.foldLeft((Nil, Nil, 0): (List[OrderedBlock], List[LiteralAbiBlock], Int)) {
        case ((staticBlocks, dynamicBlocks, refCount), encoding) =>
          encoding match {
            case Left(staticEncoding) =>
              val literalBlock = Left(LiteralAbiBlock(staticEncoding))
              (staticBlocks :+ literalBlock, dynamicBlocks, refCount)
            case Right(dynamicEncoding) =>
              val (headerBlock, contentBlocks) = (dynamicEncoding.head, dynamicEncoding.tail)
              val dynamicHeader                = LiteralAbiBlock(headerBlock, Some(refCount))
              val dynamicContent               = contentBlocks.map(block => LiteralAbiBlock(block))
              (staticBlocks :+ Right(ReferenceToBlock(refCount)),
               dynamicBlocks ++ (dynamicHeader :: dynamicContent),
               refCount + 1)
          }
      }
    staticBlocks ++ dynamicBlocks.map(Left(_))
  }

  def resolveReferences(orderedBlocks: List[OrderedBlock]): List[LiteralAbiBlock] = {
    orderedBlocks.map {
      case Left(literal) =>
        literal
      case Right(reference) =>
        val pointedBlock = orderedBlocks.indexWhere {
          _.left.map(_.id.contains(reference.blockId)).left.getOrElse(false)
        }
        LiteralAbiBlock(Uint.apply(256, pointedBlock * 32).asBlocks.left.get)
    }
  }
}
