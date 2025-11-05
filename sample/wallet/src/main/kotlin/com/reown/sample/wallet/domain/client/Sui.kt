package com.reown.sample.wallet.domain.client

import android.content.Context
import com.reown.walletkit.BuildConfig
import com.yttrium.utils.YttriumUtilsKt
import uniffi.yttrium_utils.PulseMetadata
import uniffi.yttrium_utils.suiGenerateKeypair
import uniffi.yttrium_utils.suiGetAddress
import uniffi.yttrium_utils.suiGetPublicKey
import uniffi.yttrium_utils.suiPersonalSign
import uniffi.yttrium_utils.SuiClient

object SuiUtils {
    private lateinit var client: SuiClient

    fun init(projectId: String, packageName: String, applicationContext: Context) {
        YttriumUtilsKt.initializeTls(applicationContext)

        client = SuiClient(
            projectId = projectId, pulseMetadata = PulseMetadata(
                sdkPlatform = "mobile",
                sdkVersion = "reown-kotlin-${BuildConfig.SDK_VERSION}",
                bundleId = packageName,
                url = null
            )
        )
    }

    suspend fun signAndExecuteTransaction(chaiId: String, keyPair: String, txData: ByteArray): String {
        check(::client.isInitialized) { "Initialize SuiUtils before using it." }
        return client.signAndExecuteTransaction(chaiId, keyPair, txData)
    }

    suspend fun signTransaction(chaiId: String, keyPair: String, txData: ByteArray): Pair<String, String> {
        check(::client.isInitialized) { "Initialize SuiUtils before using it." }
        val result = client.signTransaction(chaiId, keyPair, txData)
        return Pair(result.signature, result.txBytes)
    }

    fun personalSign(keyPair: String, message: ByteArray): String = suiPersonalSign(keyPair, message)
    fun generateKeyPair(): String = suiGenerateKeypair()
    fun getAddressFromPublicKey(publicKey: String): String = suiGetAddress(publicKey)
    fun getPublicKeyFromKeyPair(keyPair: String): String = suiGetPublicKey(keyPair)
}