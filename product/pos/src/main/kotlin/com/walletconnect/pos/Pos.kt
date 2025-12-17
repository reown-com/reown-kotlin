package com.walletconnect.pos

import java.net.URI

object Pos {

    data class Amount(
        val unit: String,
        val value: String
    ) {

        fun format(): String {
            val currency = unit.substringAfter("/", "")
            val amount = value.toLongOrNull() ?: throw IllegalStateException("Invalid amount value: $value")
            val majorUnits = amount / 100.0
            return String.format("%.2f %s", majorUnits, currency)
        }
    }

    sealed interface PaymentEvent {
        data class PaymentCreated(val uri: URI, val amount: Amount, val paymentId: String) : PaymentEvent
        data object PaymentRequested : PaymentEvent
        data object PaymentProcessing : PaymentEvent
        data class PaymentSuccess(val paymentId: String) : PaymentEvent
        sealed interface PaymentError : PaymentEvent {
            data class CreatePaymentFailed(val message: String) : PaymentError
            data class PaymentFailed(val message: String) : PaymentError
            data class PaymentNotFound(val message: String) : PaymentError
            data class PaymentExpired(val message: String) : PaymentError
            data class InvalidPaymentRequest(val message: String) : PaymentError
            data class Undefined(val message: String) : PaymentError
        }
    }
}

interface POSDelegate {
    fun onEvent(event: Pos.PaymentEvent)
}
