package com.clluc.stockmind.core

import com.clluc.stockmind.core.ethereum.solidity.{Address, Uint}

case class SignUpConfiguration(
    ethStartingBalance: BigInt,
    ethGasAddress: Address,
    ethGasPassword: String,
    mobileAuthUrl: String,
    sldSupplier: Address,
    sldSupplierPassword: String,
    sldWelcomeAmount: Uint
)
