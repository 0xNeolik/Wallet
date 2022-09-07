package com.clluc.stockmind.port.secondary

import com.clluc.stockmind.core.ethereum.Block

import scala.concurrent.{ExecutionContext, Future}

trait AppConfigPort {
  def getBlock(key: String)(implicit ec: ExecutionContext): Future[Block]
  def setBlock(key: String, block: Block)(implicit ec: ExecutionContext): Future[Block]
}
