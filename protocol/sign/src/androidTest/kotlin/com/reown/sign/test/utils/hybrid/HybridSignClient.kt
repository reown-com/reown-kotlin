package com.reown.sign.test.utils.hybrid

import com.reown.android.Core
import com.reown.sign.client.Sign
import com.reown.sign.test.utils.TestClient
import com.reown.sign.test.utils.globalOnError
import com.reown.sign.test.utils.proposalNamespaces
import timber.log.Timber

val HybridSignClient = TestClient.Hybrid.signClient

val hybridClientConnect = { pairing: Core.Model.Pairing ->
    val connectParams = Sign.Params.ConnectParams(sessionNamespaces = proposalNamespaces, properties = null, pairing = pairing)
    HybridSignClient.connect(
        connectParams,
        onSuccess = { url -> Timber.d("HybridDappClient: connect onSuccess, url: $url") },
        onError = ::globalOnError
    )
}