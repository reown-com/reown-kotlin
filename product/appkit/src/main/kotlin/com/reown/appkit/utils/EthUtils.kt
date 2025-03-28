package com.reown.appkit.utils

object EthUtils {
    const val walletSwitchEthChain = "wallet_switchEthereumChain"
    const val walletAddEthChain = "wallet_addEthereumChain"

    val ethRequiredMethods = listOf(
        "personal_sign",
        "eth_signTypedData",
        "eth_sendTransaction",
    )


    val ethOptionalMethods = listOf(walletSwitchEthChain, walletAddEthChain, "wallet_getAssets") //todo: remove me

    val ethMethods = ethRequiredMethods + ethOptionalMethods

    const val chainChanged = "chainChanged"
    const val accountsChanged = "accountsChanged"

    val ethEvents = listOf(chainChanged, accountsChanged)
}
