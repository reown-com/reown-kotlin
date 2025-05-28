package com.reown.walletkit.utils.sui

import uniffi.yttrium.PulseMetadata
import uniffi.yttrium.SuiClient
import uniffi.yttrium.suiGenerateKeypair
import uniffi.yttrium.suiGetAddress
import uniffi.yttrium.suiGetPublicKey
import uniffi.yttrium.suiPersonalSign
import uniffi.yttrium.suiSignTransaction

object SuiUtils {
    private lateinit var client: SuiClient

    fun init(projectId: String, packageName: String) {
        client = SuiClient(
            projectId = projectId, pulseMetadata = PulseMetadata(
                sdkPlatform = "mobile",
                sdkVersion = "reown-kotlin-${com.reown.walletkit.BuildConfig.SDK_VERSION}",
                bundleId = packageName,
                url = null
            )
        )
    }

    suspend fun signAndExecuteTransaction(chaiId: String, keyPair: String, txData: ByteArray): String {
        check(::client.isInitialized) { "Initialize SuiUtils before using it." }
        return client.signAndExecuteTransaction(chaiId, keyPair, txData)
    }

    fun signTransaction(keyPair: String, txData: ByteArray): String = suiSignTransaction(keyPair, txData)
    fun personalSign(keyPair: String, message: ByteArray): String = suiPersonalSign(keyPair, message)
    fun generateKeyPair(): String = suiGenerateKeypair()
    fun getAddressFromPublicKey(publicKey: String): String = suiGetAddress(publicKey)
    fun getPublicKeyFromKeyPair(keyPair: String): String = suiGetPublicKey(keyPair)
}