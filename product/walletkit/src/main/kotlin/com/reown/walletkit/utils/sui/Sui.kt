package com.reown.walletkit.utils.sui

import uniffi.yttrium.PulseMetadata
import uniffi.yttrium.SuiClient
import uniffi.yttrium.suiGenerateKeypair
import uniffi.yttrium.suiGetAddress
import uniffi.yttrium.suiGetPublicKey

object SuiUtils {
    private var client: SuiClient? = null

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

    fun generateKeyPair(): String = suiGenerateKeypair()
    fun getAddressFromKeyPair(keyPair: String): String = suiGetAddress(keyPair)
    fun getPublicKeyFromKeyPair(keyPair: String): String = suiGetPublicKey(keyPair)
}