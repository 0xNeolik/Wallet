package com.clluc.stockmind.core.actor

import java.util.UUID

import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.testkit.{TestActorRef, TestProbe}
import com.clluc.stockmind.core.actor.ActorTestUtils.ActorTest
import com.clluc.stockmind.core.actor.BlockWatcherActor.{MostRecentBlock, ParseBlock}
import com.clluc.stockmind.core.ethereum.{
  Block,
  EthereumAccount,
  EthereumHash,
  Transaction => Transaction
}
import com.clluc.stockmind.core.ethereum.JsonRpcResponse.EthereumResponse
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}

import scala.concurrent.{ExecutionContextExecutor, Future}

class BlockWatcherActorTest extends ActorTest("BlockWatcherActorTest") {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val materializer                               = ActorMaterializer()

  trait Fixture {

    val transactionProcessor = TestProbe()
    val gasSupplier          = TestProbe()

    val startingBlock = Block(100)

    val ethhash          = EthereumHash("20ff5117010b989095c48e8ef700ceeff9065f8841dd87fca3748c2e01aee5bc")
    val origin           = Address("27bb11b5ff3295272b9e13a648d565cfd57c589d")
    val destination      = Address("d9fb98acf05136196e168a3914c4b151e2236296")
    val nextBlockToParse = Block(100)
    val zeroVal          = Uint(256, 0L)
    val nonzeroVal       = Uint(256, 1L)

    val contractCreationTx =
      Transaction(
        hash = ethhash,
        from = origin,
        to = None,
        blockNumber = nextBlockToParse,
        txIndex = 0,
        value = zeroVal
      )

    val nonzeroContractCreationTx =
      Transaction(
        hash = ethhash,
        from = origin,
        to = None,
        blockNumber = nextBlockToParse,
        txIndex = 0,
        value = nonzeroVal
      )

    val zeroTx =
      Transaction(
        hash = ethhash,
        from = origin,
        to = Some(destination),
        blockNumber = nextBlockToParse,
        txIndex = 0,
        value = zeroVal
      )

    val nonzeroTx =
      Transaction(
        hash = ethhash,
        from = origin,
        to = Some(destination),
        blockNumber = nextBlockToParse,
        txIndex = 0,
        value = nonzeroVal
      )

    def getTransactions(
        returnedTxs: List[Transaction] = List()
    ): Block => Future[EthereumResponse[Option[List[Transaction]]]] = { _ =>
      Future.successful(Right(Some(returnedTxs)))
    }

    def findAddress(found: Boolean): Address => Future[Option[EthereumAccount]] = { addr =>
      Future.successful {
        if (found) {
          Some(EthereumAccount(UUID.randomUUID(), addr, "hunter2"))
        } else {
          None
        }
      }
    }

    def pretendToStoreBlock(b: Block): Future[Block] = Future.successful(b)

    def getWatcherActor(
        getTransactions: Block => Future[EthereumResponse[Option[List[Transaction]]]] = _ => ???,
        findAddress: Address => Future[Option[EthereumAccount]] = _ => ???,
        storeLastProcessedBlock: Block => Future[Block] = _ => ???,
    ) =
      TestActorRef(
        new BlockWatcherActor(
          startingBlock = startingBlock,
          getTransactions = getTransactions,
          findAddress = findAddress,
          storeLastProcessedBlock = storeLastProcessedBlock,
          masterAddress = Address.default,
          eventProcessor = transactionProcessor.ref,
          gasSupplier.ref,
        ))

  }

  behavior of "Actor construction"

  it should "have a 'props' helper" in {
    val props = BlockWatcherActor.props(
      Block(1),
      _ => ???,
      _ => ???,
      _ => ???,
      Address.default,
      TestProbe().ref,
      TestProbe().ref,
    )

    props shouldBe a[Props]
    BlockWatcherActor.getClass.getSimpleName should startWith(props.actorClass().getSimpleName)
  }

  behavior of "Most recent block updating"

  it should "do nothing if it receives a most recent block older or equal than the one it knows about" in new Fixture {
    val watcher      = getWatcherActor()
    val watcherActor = watcher.underlyingActor

    watcher ! MostRecentBlock(Block(99))
    watcherActor.mostRecentBlock shouldBe startingBlock
    watcher ! MostRecentBlock(Block(100))
    watcherActor.mostRecentBlock shouldBe startingBlock
  }

  it should "update the most recent block and start processing when receiving a newer block" in new Fixture {
    val watcher      = getWatcherActor(getTransactions = getTransactions())
    val watcherActor = watcher.underlyingActor

    val newerBlock = Block(101)

    watcher ! MostRecentBlock(newerBlock)
    watcherActor.mostRecentBlock shouldBe newerBlock
  }

  behavior of "Block parsing"

  it should "do nothing when asked to parse a block newer than the most recent block" in new Fixture {
    val watcher      = getWatcherActor()
    val watcherActor = watcher.underlyingActor

    watcher ! ParseBlock(Block(101))
    watcherActor.lastProcessedBlock shouldBe startingBlock
  }

  it should "parse a block and send the appropriate transactions to the processor and the gas supplier" in new Fixture {

    val watcher = getWatcherActor(
      getTransactions =
        getTransactions(List(contractCreationTx, nonzeroContractCreationTx, nonzeroTx, zeroTx)),
      findAddress = findAddress(true),
      storeLastProcessedBlock = pretendToStoreBlock
    )

    watcher ! ParseBlock(nextBlockToParse)
    transactionProcessor.expectMsg(nonzeroTx)
    gasSupplier.expectMsg(nonzeroTx)
  }

}
