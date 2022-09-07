package com.clluc.stockmind.adapter.postgres

import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import Generators._

class PostgresTransferMetadataAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  override def afterAll() = TableCleaner.clean()

  val transferMetadataDao = PostgresTransferMetadataAdapter(testTransactor)
  val offchainTransferDao = new PostgresOffChainTransferAdapter(testTransactor)
  val erc20TokenDao = new PostgresErc20InfoAdapter(testTransactor)

  behavior of "PostgresAppConfigAdapter"

  it should "Write and then read a couple of meta info fields for a tokens transaction" in {
    val token = genErc20Token.sample.get
    val tx = genOffchainTransfer(token.symbol).sample.get

    val metaInf = Map(
      "att1" -> "value1",
      "att2" -> "value2"
    )

    for {
      _ <- erc20TokenDao.createEthereumToken(token)
      t <- offchainTransferDao.create(tx)
      _ <- transferMetadataDao.saveMetaInf(t.id, metaInf)
      meta <- transferMetadataDao.readMetaInf(t.id)
    } yield meta shouldBe metaInf
  }
}
