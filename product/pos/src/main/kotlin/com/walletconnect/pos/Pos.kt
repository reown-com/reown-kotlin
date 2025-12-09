package com.walletconnect.pos

import java.net.URI

object Pos {

    sealed class Model {
        data class Amount(
            val unit: String,
            val value: String
        ) : Model()

        sealed interface PaymentEvent {
            data class PaymentCreated(
                val uri: URI,
                val amount: Amount,
                val paymentId: String
            ) : PaymentEvent

            data object PaymentRequested : PaymentEvent
            data object PaymentProcessing : PaymentEvent
            data class PaymentSuccess(val paymentId: String) : PaymentEvent
            data class PaymentError(val error: Model.PaymentError) : PaymentEvent
        }

        sealed interface PaymentError {
            data class CreatePaymentFailed(val message: String) : PaymentError
            data class PaymentFailed(val message: String) : PaymentError
            data class PaymentNotFound(val message: String) : PaymentError
            data class PaymentExpired(val message: String) : PaymentError
            data class InvalidPaymentRequest(val message: String) : PaymentError
            data class Generic(val message: String) : PaymentError
        }
    }
}

interface POSDelegate {
    fun onEvent(event: Pos.Model.PaymentEvent)
}
