package com.reown.pos.client

import android.app.Application
import java.net.URI

object POS {

    sealed class Model {
        data class Error(val throwable: Throwable) : Model()

        data class MetaData(
            val merchantName: String,
            val description: String,
            val url: String,
            val icons: List<String>
        ) : Model()

        data class Namespace(
            val methods: List<String>,
            val chains: List<String>,
            val events: List<String>
        ) : Model()

        sealed interface PaymentEvent {
            data class QrReady(val uri: URI) : PaymentEvent
            data object Connected : PaymentEvent
            data object ConnectionRejected : PaymentEvent
            data class ConnectionFailed(val error: Throwable) : PaymentEvent
            data object PaymentRequested : PaymentEvent
            data object PaymentBroadcasted : PaymentEvent
            data class PaymentRejected(val message: String) : PaymentEvent
            data class PaymentSuccessful(val result: Any, val recipient: String) : PaymentEvent
            data class Error(val error: Throwable) : PaymentEvent
        }

        data class PaymentIntent(
            var token: Token,
            val amount: String,
            val recipient: String
        ) : Model() {
            val caip19Token = "${token.network.chainId}/${token.standard}:${token.address}"
            val caip10Receipient = "${token.network.chainId}:${recipient}"
        }

        data class Token(
            val network: Network,
            val symbol: String,
            val standard: String,
            val address: String
        ) : Model()

        data class Network(
            val name: String,
            val chainId: String
        ) : Model()
    }

    sealed class Params {
        data class Init(
            val projectId: String,
            val deviceId: String,
            val metaData: Model.MetaData,
            val application: Application
        ) : Params()
    }
}
