package modules

import java.util.Properties

import akka.actor.ActorSystem
import com.clluc.stockmind.adapter.ethereum._
import com.clluc.stockmind.adapter.postgres._
import com.clluc.stockmind.adapter.twitter.{TwitterAdapter => SecondaryTwitterAdapter}
import com.clluc.stockmind.port.secondary
import secondary.{TwitterAccountPort => SecondaryTwitterAccountPort}
import com.clluc.stockmind.core.Bootstrap.BootstrapConfigurationDependencies
import com.clluc.stockmind.core.auth.{CasInfoAdapter, Oauth1InfoAdapter, SocialAuthAdapter}
import com.clluc.stockmind.core.ethereum.TokenFactoryContract
import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}
import com.clluc.stockmind.core.signup.EthereumAccountOperationsAdapter
import com.clluc.stockmind.core.tokens.TokensAdapter
import com.clluc.stockmind.core.transaction.{RetrieveTransactionsAdapter, SendTransactionAdapter}
import com.clluc.stockmind.core.twitter.TwitterAdapter
import com.clluc.stockmind.core.users.UserAdapter
import com.clluc.stockmind.core.{BootstrapAdapter, SignUpConfiguration}
import com.clluc.stockmind.port.primary.{
  CasInfoPort => PrimaryCasInfoPort,
  Oauth1InfoPort => PrimaryOAuth1InfoPort,
  TwitterPort => PrimaryTwitterPort,
  UserPort => PrimaryUserPort,
  _
}
import com.clluc.stockmind.port.secondary.{
  CasInfoPort => SecondaryCasInfoPort,
  Oauth1InfoPort => SecondaryOAuth1InfoPort,
  TwitterPort => SecondaryTwitterPort,
  UsersRepositoryPort => SecondaryUserPort,
  _
}
import com.google.inject.Provides
import com.google.inject.name.Named
import com.typesafe.config.{Config, ConfigFactory}
import com.clluc.stockmind.core.Cipher
import org.joda.time.{DateTime, DateTimeZone}
import doobie.imports._
import fs2.interop.cats._
import net.codingwell.scalaguice.ScalaModule
//import javax.inject.Inject

import scala.concurrent.ExecutionContext

class PortsAdaptersModule() extends ScalaModule {

  private val conf: Config       = ConfigFactory.load()
  override def configure(): Unit = {}

  @Provides
  implicit lazy val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  @Provides
  def transactor(): Transactor[IOLite] = {

    // TODO This is not production ready; as it doesn't use a connection pool. just a plain DB connection that is created and closed
    // over and over again.

    val url      = conf.getString("database.url")
    val user     = conf.getString("database.user")
    val password = conf.getString("database.pass")
    val ssl      = conf.getString("database.ssl")

    val properties = new Properties()
    properties.setProperty("user", user)
    properties.setProperty("password", password)
    ssl match {
      case "enabled" =>
        properties.setProperty("ssl", "true")
      case "selfsigned" =>
        properties.setProperty("ssl", "true")
        properties.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory")
      case _ =>
    }

    DriverManagerTransactor[IOLite]("org.postgresql.Driver", url, properties)
  }

  @Provides
  def bootstrapConfigurationDependencies(): BootstrapConfigurationDependencies =
    BootstrapConfigurationDependencies(
      etherSupplierAddress = conf.getString("eth.supplier.account"),
      etherSupplierAccountPassword = conf.getString("eth.supplier.password"),
      etherRefillThreshold = BigInt(conf.getString("eth.supplier.amount.threshold")),
      etherAmountToRefill = BigInt(conf.getString("eth.supplier.amount.refill")),
      ethereumMasterAccount = conf.getString("eth.master.account"),
      ethereumMasterPassword = conf.getString("eth.master.password"),
      tokenFactoryAddress = conf.getString("eth.factory.address")
    )

  def nowFx = DateTime.now

  @Provides
  def bootstrapPort(
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      erc20TransferEventPort: Erc20TransferEventPort,
      ethereumAccountPort: EthereumAccountPort,
      appConfigPort: AppConfigPort,
      offchainTransferPort: OffChainTransferPort,
      inboundTransferPort: InboundTransferPort,
      outboundTransferPort: OutboundTransferPort,
      ethereumClientPort: EthereumClientPort,
      conf: BootstrapConfigurationDependencies,
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ): BootstrapPort = {

    BootstrapAdapter(
      erc20InfoPort,
      erc721InfoPort,
      erc20TransferEventPort,
      ethereumAccountPort,
      appConfigPort,
      offchainTransferPort,
      inboundTransferPort,
      outboundTransferPort,
      ethereumClientPort,
      conf,
      nowFx
    )(actorSystem, executionContext)
  }

  @Provides
  def licensePort(
      //licensePort: LicensePort
  ): LicensePort = {

    Cipher(
      conf.getString("license")
    )
  }

  @Provides
  def provideAppConfigPort(tx: Transactor[IOLite]): AppConfigPort =
    PostgresAppConfigAdapter(tx)

  @Provides
  def provideAuthTokenPort(clock: () => DateTime, tx: Transactor[IOLite]): AuthTokenPort =
    PostgresAuthTokenAdapter(clock, tx)

  @Provides
  def provideOauth1InfoPort(tx: Transactor[IOLite]): SecondaryOAuth1InfoPort =
    PostgresOauth1InfoAdapter(tx)

  @Provides
  def provideCasInfoPort(tx: Transactor[IOLite]): SecondaryCasInfoPort =
    PostgresCasInfoAdapter(tx)

  @Provides
  def provideSecondaryUserPort(tx: Transactor[IOLite]): SecondaryUserPort =
    PostgresUsersRepositoryAdapter(tx)

  @Provides
  def provideErc20InfoPort(tx: Transactor[IOLite]): Erc20InfoPort =
    PostgresErc20InfoAdapter(tx)

  @Provides
  def provideErc721InfoPort(tx: Transactor[IOLite]): Erc721InfoPort =
    PostgresErc721InfoAdapter(tx)

  @Provides
  def provideEthereumAccountPort(tx: Transactor[IOLite]): EthereumAccountPort =
    PostgresEthereumAccountAdapter(tx)

  @Provides
  def provideTwitterAccountPort(tx: Transactor[IOLite]): TwitterAccountPort =
    PostgresTwitterAccountAdapter(tx)

  @Provides
  def provideOffchainTransferPort(tx: Transactor[IOLite]): OffChainTransferPort =
    PostgresOffChainTransferAdapter(tx)

  @Provides
  def provideInboundTransferPort(tx: Transactor[IOLite]): InboundTransferPort =
    PostgresInboundTransferAdapter(tx)

  @Provides
  def provideOutboundTransferPort(tx: Transactor[IOLite]): OutboundTransferPort =
    PostgresOutboundTransferAdapter(tx)

  @Provides
  def provideEthereumClientPort(): EthereumClientPort = {
    val ethNode = conf.getString("eth.node")
    DefaultEthereumClientAdapter(ethNode)
  }

  @Provides
  def provideOffChainBalancePort(
      tx: Transactor[IOLite],
      ec: ExecutionContext
  ): OffchainBalancePort =
    new PostgresOffchainBalanceAdapter(tx)(ec)

  @Provides
  def provideAccountOpsPort(ethereumAccountPort: EthereumAccountPort,
                            ethereumClientPort: EthereumClientPort): EthereumAccountOperationsPort =
    EthereumAccountOperationsAdapter(ethereumAccountPort, ethereumClientPort)

  @Provides
  @Named("consumer-key")
  def provideConsumerKey(): String = {
    conf.getString("silhouette.twitter.consumerKey")
  }

  @Provides
  @Named("consumer-secret")
  def provideConsumerSecret(): String = {
    conf.getString("silhouette.twitter.consumerSecret")
  }

  @Provides
  @Named("stockmind-url")
  def provideStockmindUrl(): String =
    conf.getString("stockmind.url")

  @Provides
  @Named("masterAccountAddress")
  def masterAccountAddress(): Address =
    Address.decode(conf.getString("eth.master.account"))

  @Provides
  @Named("masterAccountPassword")
  def masterAccountPassword(): String =
    conf.getString("eth.master.password")

  @Provides
  @Named("tokenFactoryAddress")
  def tokenFactoryAddress(): Address = Address.decode(conf.getString("eth.factory.address"))

  @Provides
  @Named("auth0Method")
  def auth0Method(): String =
    conf.getString("auth0.auth.method")

  @Provides
  def provideTimestampFx(): () => DateTime =
    () => new DateTime(DateTimeZone.UTC)

  @Provides
  def twitterPort(@Named("consumer-key") key: String,
                  @Named("consumer-secret") secret: String): SecondaryTwitterPort =
    SecondaryTwitterAdapter(key, secret)

  @Provides
  def pendingTransferPort(tx: Transactor[IOLite]): PendingTransferPort =
    PostgresPendingTransferAdapter(tx)

  @Provides
  def erc20TransferEventPort(tx: Transactor[IOLite]): Erc20TransferEventPort =
    PostgresErc20TransferEventAdapter(tx)

  @Provides
  def notValidWithdrawDestinationAddresses(
      @Named("masterAccountAddress") masterAccountAddress: Address): List[Address] =
    List(
      Address.decode(conf.getString("eth.gas.account")),
      Address.decode(conf.getString("eth.sld.account")),
      Address.decode(conf.getString("eth.supplier.account")),
      masterAccountAddress
    )

  @Provides
  def transactionPort(
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      twitterAccountPort: TwitterAccountPort,
      ethereumAccountPort: EthereumAccountPort,
      offchainBalancePort: OffchainBalancePort,
      twitterPort: SecondaryTwitterPort,
      userPort: SecondaryUserPort,
      oauth1InfoPort: SecondaryOAuth1InfoPort,
      pendingTransferPort: PendingTransferPort,
      offchainTransferPort: OffChainTransferPort,
      timestampFx: () => DateTime,
      ethereumClientPort: EthereumClientPort,
      outboundTransferPort: OutboundTransferPort,
      settleTransferPort: SettleTransferPort,
      actorSystem: ActorSystem,
      executionContext: ExecutionContext,
      notValidDestinationAddresses: List[Address],
      metaInfPort: TransactionMetaInfPort,
      @Named("stockmind-url") stockmindUrl: String
  ): SendTransactionPort =
    new SendTransactionAdapter(
      erc20InfoPort,
      erc721InfoPort,
      twitterAccountPort,
      ethereumAccountPort,
      offchainBalancePort,
      twitterPort,
      userPort,
      oauth1InfoPort,
      pendingTransferPort,
      offchainTransferPort,
      ethereumClientPort,
      outboundTransferPort,
      settleTransferPort,
      metaInfPort,
      notValidDestinationAddresses: List[Address],
      stockmindUrl
    )(
      actorSystem,
      executionContext
    )

  @Provides
  def transactionMetaInfPort(tx: Transactor[IOLite],
                             executionContext: ExecutionContext): TransactionMetaInfPort =
    PostgresTransferMetadataAdapter(tx)(executionContext)

  @Provides
  def settleTransferPort(tx: Transactor[IOLite],
                         executionContext: ExecutionContext): SettleTransferPort =
    PostgresSettleTransferAdapter(
      tx
    )(executionContext)

  @Provides
  def primaryUserPort(
      ethereumAccountPort: EthereumAccountPort,
      balancePort: OffchainBalancePort,
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      secondaryUserPort: SecondaryUserPort
  ): PrimaryUserPort =
    UserAdapter(
      ethereumAccountPort,
      balancePort,
      erc20InfoPort,
      erc721InfoPort,
      secondaryUserPort
    )

  @Provides
  def primaryGetTransactionsPort(
      ethereumAccountPort: EthereumAccountPort,
      offchainTransferPort: OffChainTransferPort,
      pendingTransferPort: PendingTransferPort,
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      erc20TransferEventPort: Erc20TransferEventPort,
      userPort: SecondaryUserPort,
      executionContext: ExecutionContext
  ): RetrieveTransactionsPort =
    RetrieveTransactionsAdapter(
      ethereumAccountPort,
      offchainTransferPort,
      pendingTransferPort,
      erc20InfoPort,
      erc721InfoPort,
      erc20TransferEventPort,
      userPort
    )(executionContext)

  @Provides
  def socialAuthPortConfiguration(): SignUpConfiguration =
    SignUpConfiguration(
      conf.getInt("eth.new.account.finney") * BigInt("1000000000000000"),
      Address.decode(conf.getString("eth.gas.account")),
      conf.getString("eth.gas.password"),
      conf.getString("mobile.authRedirectUrl"),
      Address.decode(conf.getString("eth.sld.account")),
      conf.getString("eth.sld.password"),
      Uint(256, BigInt(conf.getString("eth.sld.gift")))
    )

  @Provides
  def socialAuthPort(
      userPort: SecondaryUserPort,
      twitterPort: SecondaryTwitterPort,
      twitterAccountPort: TwitterAccountPort,
      ethereumClientPort: EthereumClientPort,
      configuration: SignUpConfiguration,
      erc20InfoPort: Erc20InfoPort,
  ): SocialAuthPort =
    SocialAuthAdapter(
      userPort,
      twitterPort,
      twitterAccountPort,
      ethereumClientPort,
      configuration,
      erc20InfoPort,
    )

  @Provides
  def primaryTwitterPort(
      secondaryTwitterPort: SecondaryTwitterPort,
      secondaryTwitterAccountPort: SecondaryTwitterAccountPort,
      executionContext: ExecutionContext
  ): PrimaryTwitterPort =
    TwitterAdapter(
      secondaryTwitterPort,
      secondaryTwitterAccountPort
    )(executionContext)

  @Provides
  def primaryOAuth1InfoPort(
      port: SecondaryOAuth1InfoPort
  ): PrimaryOAuth1InfoPort =
    Oauth1InfoAdapter(port)

  @Provides
  def primaryCasInfoPort(port: SecondaryCasInfoPort): PrimaryCasInfoPort = CasInfoAdapter(port)

  @Provides
  def tokenFactoryContract(
      @Named("tokenFactoryAddress") factoryAddress: Address,
      @Named("masterAccountAddress") masterAddress: Address
  ): TokenFactoryContract =
    TokenFactoryContract(factoryAddress, masterAddress)

  @Provides
  def primaryTokensPort(
      erc20InfoPort: Erc20InfoPort,
      erc721InfoPort: Erc721InfoPort,
      ethereumAccountPort: EthereumAccountPort,
      ethereumClientPort: EthereumClientPort,
      tokenFactoryContract: TokenFactoryContract,
      @Named("masterAccountPassword") masterAccountPassword: String
  ): TokensPort =
    new TokensAdapter(
      erc20InfoPort,
      erc721InfoPort,
      ethereumAccountPort,
      ethereumClientPort,
      tokenFactoryContract,
      masterAccountPassword
    )
}
