package com.reown.sign.test.utils

import com.reown.android.BuildConfig
import com.reown.android.Core
import com.reown.sign.client.Sign
import junit.framework.TestCase.fail
import timber.log.Timber

internal fun globalOnError(error: Sign.Model.Error) {
    Timber.e("globalOnError: ${error.throwable.stackTraceToString()}")
    fail(error.throwable.message)
}

internal fun globalOnError(error: Core.Model.Error) {
    Timber.e("globalOnError: ${error.throwable.stackTraceToString()}")
    fail(error.throwable.message)
}


const val sessionNamespaceKey = "eip155"
val sessionChains = listOf("eip155:1", "eip155:10")
val sessionAccounts =
    listOf("eip155:1:0xab16a96d359ec26a11e2c2b3d8f8b8942d5bfcdb", "eip155:10:0x9CAaB7E1D1ad6eaB4d6a7f479Cb8800da551cbc0")
val sessionMethods = listOf("personal_sign", "wallet_getAssets")
val sessionEvents = listOf("someEvent")

val sessionNamespace =
    Sign.Model.Namespace.Session(sessionChains, sessionAccounts, sessionMethods, sessionEvents)
val proposalNamespace = Sign.Model.Namespace.Proposal(sessionChains, sessionMethods, sessionEvents)

val proposalNamespaces: Map<String, Sign.Model.Namespace.Proposal> =
    mapOf(sessionNamespaceKey to proposalNamespace)
val sessionNamespaces: Map<String, Sign.Model.Namespace.Session> =
    mapOf(sessionNamespaceKey to sessionNamespace)
val scopedProperties: Map<String, String> = mapOf(
    "eip155" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=${BuildConfig.PROJECT_ID}&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\"]}]}"
)