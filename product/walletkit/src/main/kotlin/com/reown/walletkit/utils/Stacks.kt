package com.reown.walletkit.utils

import com.reown.walletkit.BuildConfig
import uniffi.yttrium.PulseMetadata
import uniffi.yttrium.StacksClient
import uniffi.yttrium.TransferStxRequest
import uniffi.yttrium.stacksGenerateWallet
import uniffi.yttrium.stacksGetAddress
import uniffi.yttrium.stacksSignMessage

object Stacks {
    private lateinit var client: StacksClient

    fun init(projectId: String, packageName: String) {
        client = StacksClient(
            projectId = projectId, pulseMetadata = PulseMetadata(
                sdkPlatform = "mobile",
                sdkVersion = "reown-kotlin-${BuildConfig.SDK_VERSION}",
                bundleId = packageName,
                url = null
            )
        )
    }

    fun generateWallet(): String = stacksGenerateWallet()
    fun getAddress(wallet: String, version: String): String = stacksGetAddress(wallet, version)
    fun signMessage(wallet: String, message: String): String = stacksSignMessage(wallet, message)

    suspend fun transferStx(wallet: String, network: String, recipient: String, amount: String, memo: String, sender: String): Pair<String, String> {
        check(::client.isInitialized) { "Initialize SuiUtils before using it." }
        val result = client.transferStx(wallet, network, TransferStxRequest(recipient = recipient, amount = amount.toULong(), memo = memo, sender = sender))
        return Pair(result.txid, result.transaction)
    }

    object Version {
        const val mainnetP2PKH = "mainnet-p2pkh"
        const val mainnetP2SH = "mainnet-p2sh"
        const val testnetP2PKH = "testnet-p2pkh"
        const val testnetP2SH = "testnet-p2sh"
    }
}