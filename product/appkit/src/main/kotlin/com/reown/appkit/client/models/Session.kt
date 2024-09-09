package com.reown.appkit.client.models

import com.reown.android.Core
import com.reown.appkit.client.Modal

sealed class Session {

    data class WalletConnectSession(
        val pairingTopic: String,
        val topic: String,
        val expiry: Long,
        val namespaces: Map<String, Modal.Model.Namespace.Session>,
        val metaData: Core.Model.AppMetaData?,
    ) : Session() {
        val redirect: String? = metaData?.redirect
    }

    data class CoinbaseSession(
        val chain: String,
        val address: String
    ) : Session()
}
