package com.reown.pos.client

import com.reown.android.CoreInterface
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

        data class PaymentEvent(val event: String) : Model()

        sealed interface PaymentResult {
            data class Success(val txHash: String, val receipt: String) : PaymentResult
            data class Failure(val error: PosError) : PaymentResult
        }

        sealed interface PosError {
            data object Timeout : PosError
            data object RejectedByUser : PosError
            data class Backend(val code: Int, val message: String) : PosError
            data object Network : PosError
            data object WalletUnsupported : PosError
            data class Unknown(val cause: Throwable?) : PosError
        }

        data class PaymentIntent(val token: String, val amount: String, val chainId: String, val recipient: String) : Model()
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
