package com.walletconnect.pos.api

import com.walletconnect.pos.Pos

internal fun mapErrorCodeToPaymentError(code: String, message: String): Pos.Model.PaymentError {
    return when (code) {
        ErrorCodes.PAYMENT_NOT_FOUND -> Pos.Model.PaymentError.PaymentNotFound(message)
        ErrorCodes.PAYMENT_EXPIRED -> Pos.Model.PaymentError.PaymentExpired(message)
        ErrorCodes.INVALID_REQUEST -> Pos.Model.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.Model.PaymentError.Generic(message)
    }
}

internal fun mapCreatePaymentError(code: String, message: String): Pos.Model.PaymentError {
    return when (code) {
        ErrorCodes.INVALID_REQUEST -> Pos.Model.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.Model.PaymentError.CreatePaymentFailed(message)
    }
}

internal fun mapStatusToPaymentEvent(status: String, paymentId: String): Pos.Model.PaymentEvent {
    return when (status) {
        PaymentStatus.REQUIRES_ACTION -> Pos.Model.PaymentEvent.PaymentRequested
        PaymentStatus.PROCESSING -> Pos.Model.PaymentEvent.PaymentProcessing
        PaymentStatus.SUCCEEDED -> Pos.Model.PaymentEvent.PaymentSuccess(paymentId)
        PaymentStatus.EXPIRED -> Pos.Model.PaymentEvent.PaymentError(
            Pos.Model.PaymentError.PaymentExpired("Payment has expired")
        )
        PaymentStatus.FAILED -> Pos.Model.PaymentEvent.PaymentError(
            Pos.Model.PaymentError.PaymentFailed("Payment failed")
        )
        else -> Pos.Model.PaymentEvent.PaymentError(
            Pos.Model.PaymentError.Generic("Unknown payment status: $status")
        )
    }
}

internal fun isTerminalStatus(status: String): Boolean {
    return status in listOf(
        PaymentStatus.SUCCEEDED,
        PaymentStatus.EXPIRED,
        PaymentStatus.FAILED
    )
}

internal fun isTerminalError(code: String): Boolean {
    return code in listOf(
        ErrorCodes.PAYMENT_NOT_FOUND,
        ErrorCodes.PAYMENT_EXPIRED
    )
}

internal fun buildPaymentUri(paymentId: String): String {
    return "https://walletconnect.com/pay/$paymentId"
}
