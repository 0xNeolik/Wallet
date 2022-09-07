package com.clluc.stockmind.core

object Bootstrap {
  case class BootstrapConfigurationDependencies(
      etherSupplierAddress: String,
      etherSupplierAccountPassword: String,
      etherRefillThreshold: BigInt,
      etherAmountToRefill: BigInt,
      ethereumMasterAccount: String,
      ethereumMasterPassword: String,
      tokenFactoryAddress: String,
  )

  case class BootstrapException(unsuccessfulEthResponse: String) extends Exception
}
