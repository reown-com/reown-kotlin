package com.reown.sign.test.utils.dapp

import com.reown.android.Core
import com.reown.sign.client.Sign
import com.reown.sign.test.utils.TestClient
import com.reown.sign.test.utils.globalOnError
import com.reown.sign.test.utils.proposalNamespaces
import com.reown.sign.test.utils.sessionChains
import com.reown.sign.test.utils.sessionMethods
import com.reown.util.bytesToHex
import com.reown.util.randomBytes
import timber.log.Timber

val DappSignClient = TestClient.Dapp.signClient

val DappSignClientLinkMode = TestClient.DappLinkMode.signClientLinkMode

val dappClientConnect = { pairing: Core.Model.Pairing ->
    val connectParams = Sign.Params.Connect(namespaces = proposalNamespaces, optionalNamespaces = null, properties = null, pairing = pairing)
    DappSignClient.connect(
        connectParams,
        onSuccess = { url -> Timber.d("DappClient: connect onSuccess, url: $url") },
        onError = ::globalOnError
    )
}

fun dappClientAuthenticate(onPairing: (String) -> Unit) {
    val authenticateParams = Sign.Params.Authenticate(
        chains = listOf("eip155:1", "eip155:137"),
        domain = "sample.dapp",
        uri = "https://react-auth-dapp.vercel.app/",
        nonce = randomBytes(12).bytesToHex(),
        exp = null,
        nbf = null,
        statement = "Sign in with wallet.",
        requestId = null,
        resources = null,
        methods = listOf("personal_sign", "eth_signTypedData_v4", "eth_sign"),
        expiry = null
    )
    DappSignClient.authenticate(
        authenticateParams,
        onSuccess = { pairingUrl ->
            Timber.d("DappClient: on sent authenticate success: $pairingUrl")
            onPairing(pairingUrl)
        },
        onError = ::globalOnError
    )
}

fun dappClientAuthenticateLinkMode(onPairing: (String) -> Unit) {
    val authenticateParams = Sign.Params.Authenticate(
        chains = listOf("eip155:1", "eip155:137"),
        domain = "sample.dapp",
        uri = "https://react-auth-dapp.vercel.app/",
        nonce = randomBytes(12).bytesToHex(),
        exp = null,
        nbf = null,
        statement = "Sign in with wallet.",
        requestId = null,
        resources = null,
        methods = listOf("personal_sign", "eth_signTypedData_v4", "eth_sign"),
        expiry = null
    )
    DappSignClientLinkMode.authenticate(
        authenticateParams,
        "https://web3modal-laboratory-git-chore-kotlin-assetlinks-walletconnect1.vercel.app/wallet",
        onSuccess = { pairingUrl ->
            Timber.d("DappClient: on sent authenticate success: $pairingUrl")
            onPairing(pairingUrl)
        },
        onError = ::globalOnError
    )
}

val dappClientSendRequest = { topic: String ->
    DappSignClient.request(
        Sign.Params.Request(topic, sessionMethods.first(), "[\"dummy\"]", sessionChains.first()),
        onSuccess = { _: Sign.Model.SentRequest -> Timber.d("Dapp: requestOnSuccess") },
        onError = ::globalOnError
    )
}


