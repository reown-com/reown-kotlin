package com.walletconnect.pos

import java.net.URI

object Pos {

    sealed class Model {

        data class Amount(
            val unit: String,
            val value: String
        ) : Model() {

            fun format(): String {
                val currency = unit.substringAfter("/", "")
                val majorUnits = (value.toLongOrNull() ?: 0L) / 100.0
                return String.format("%.2f %s", majorUnits, currency)
            }
        }

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
