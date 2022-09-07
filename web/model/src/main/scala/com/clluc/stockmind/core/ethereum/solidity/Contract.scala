package com.clluc.stockmind.core.ethereum.solidity

import com.clluc.stockmind.core.ethereum.solidity.AbiBlock.OrderedBlock
import com.typesafe.scalalogging.LazyLogging

private[stockmind] sealed trait Contract extends LazyLogging {
  val name: String
  val parameters: List[ValueType]
  def canonical: String = parameters.map(_.canonical).mkString(s"$name(", ",", ")")
  def encode: String
  def toHex: String = s"0x$encode"
  protected def hashing: Array[String] = {
    logger.trace(s"##################### Hashing ${canonical}")
    val result = Keccak.encode(canonical.getBytes).map("%02x".format(_))
    logger.trace(s"##################### Hashing result: ${result.take(4).mkString}")
    result
  }
}

private[stockmind] case class Function(override val name: String,
                                       override val parameters: List[ValueType] = List.empty)
    extends Contract {
  override def encode: String = {
    val abiBlocks: List[OrderedBlock] =
      AbiBlock.fromEncodingDefinition(ValueEncodingDefinition.fromValues(parameters))
    val resolvedAbiBlocks = AbiBlock.resolveReferences(abiBlocks)
    val blocksContent     = resolvedAbiBlocks.map(_.content.content).mkString
    functionSelector + blocksContent
  }
  private def functionSelector = hashing.take(4).mkString
}

private[stockmind] case class Event(override val name: String,
                                    override val parameters: List[ValueType])
    extends Contract {
  override def encode: String = hashing.mkString
}
