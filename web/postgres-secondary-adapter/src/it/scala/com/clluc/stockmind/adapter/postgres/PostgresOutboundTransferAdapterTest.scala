package com.clluc.stockmind.adapter.postgres

import com.clluc.stockmind.core.ethereum.EthereumHash
import com.clluc.stockmind.core.transaction.{OffChainTransfer, OutboundTransfer}
import org.postgresql.util.PSQLException
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class PostgresOutboundTransferAdapterTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  val erc20InfoAdapter = new PostgresErc20InfoAdapter(testTransactor)
  val offchainTransferAdapter = new PostgresOffChainTransferAdapter(testTransactor)
  val outboundTransferAdapter = new PostgresOutboundTransferAdapter(testTransactor)

  override def afterAll() = TableCleaner.clean()

  behavior of "PostgresOutboundTransferAdapter"

  import Generators._

  private def writeTransfer: Future[OffChainTransfer] = {
    val _token = genErc20Token.sample.get
    val _transfer = genOffchainTransfer(_token.symbol).sample.get
    for {
      _ <- erc20InfoAdapter.createEthereumToken(_token)
      transfer <- offchainTransferAdapter.create(_transfer)
    } yield transfer
  }

  it should "not create an OutboundTransfer if offchain_transfer_id does not match a known transfer" in {
    val txHash = EthereumHash(genEthHash.sample.get)
    val ot = OutboundTransfer(txHash, 123123L)
    recoverToSucceededIf[PSQLException] {
      outboundTransferAdapter.create(ot)
    }
  }

  it should "create an OutboundTransfer" in {
    val txHash = EthereumHash(genEthHash.sample.get)
    for {
      offchain <- writeTransfer
      outbound <- outboundTransferAdapter.create(OutboundTransfer(txHash, offchain.id))
    } yield succeed
  }

  it should "not create an OutboundTransfer with a duplicate tx_hash" in {
    val txHash = EthereumHash(genEthHash.sample.get)
    recoverToSucceededIf[PSQLException] {
      for {
        offchain1 <- writeTransfer
        offchain2 <- writeTransfer
        outbound1 <- outboundTransferAdapter.create(OutboundTransfer(txHash, offchain1.id))
        outbound2 <- outboundTransferAdapter.create(OutboundTransfer(txHash, offchain2.id))
      } yield outbound2
    }
  }

  it should "not find a non-existent OutboundTransfer" in {
    val txHash = EthereumHash(genEthHash.sample.get)
    for {
      ot <- outboundTransferAdapter.findByTxHash(txHash)
    } yield ot shouldBe None
  }

  it should "find an OutboundTransfer" in {
    val txHash = EthereumHash(genEthHash.sample.get)
    for {
      offchain <- writeTransfer
      outbound <- outboundTransferAdapter.create(OutboundTransfer(txHash, offchain.id))
      found <- outboundTransferAdapter.findByTxHash(txHash)
    } yield found shouldBe Some(outbound)
  }

}
