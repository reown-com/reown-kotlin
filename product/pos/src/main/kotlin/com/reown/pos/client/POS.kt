package com.reown.pos.client

import java.net.URI

/**
 * Basic POS models and parameters as specified in the documentation
 */
object POS {

    /**
     * Basic POS models
     */
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
            data object ConnectedRejected : PaymentEvent
            data class ConnectionFailed(val error: Throwable) : PaymentEvent
            data object PaymentRequested : PaymentEvent
            data object PaymentBroadcasted : PaymentEvent
            data class PaymentRejected(val error: PosError.RejectedByUser) : PaymentEvent
            data class PaymentSuccessful(val txHash: String, val receipt: String) : PaymentEvent
            data class Error(val error: PosError.General) : PaymentEvent
        }

        sealed interface PosError {
            data object Timeout : PosError
            data class RejectedByUser(val message: String) : PosError
            data class Backend(val code: Int, val message: String) : PosError
            data object Network : PosError
            data object WalletUnsupported : PosError
            data class General(val cause: Throwable?) : PosError
        }

        data class PaymentIntent(
            val token: String,
            val amount: String,
            val chainId: String,
            val recipient: String
        ) : Model()
    }

    /**
     * Basic POS parameters
     */
    sealed class Params {
        data class Init(
            val projectId: String,
            val deviceId: String,
            val metaData: Model.MetaData,
            val application: Any
        ) : Params()
    }
}
