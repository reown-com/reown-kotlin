package com.walletconnect.pos.api

import com.walletconnect.pos.Pos

internal fun mapErrorCodeToPaymentError(code: String, message: String): Pos.PaymentEvent.PaymentError {
    return when (code) {
        ErrorCodes.PAYMENT_NOT_FOUND -> Pos.PaymentEvent.PaymentError.PaymentNotFound(message)
        ErrorCodes.PAYMENT_EXPIRED -> Pos.PaymentEvent.PaymentError.PaymentExpired(message)
        ErrorCodes.INVALID_REQUEST -> Pos.PaymentEvent.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.PaymentEvent.PaymentError.Undefined(message)
    }
}

internal fun mapCreatePaymentError(code: String, message: String): Pos.PaymentEvent.PaymentError {
    return when (code) {
        ErrorCodes.INVALID_REQUEST -> Pos.PaymentEvent.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.PaymentEvent.PaymentError.CreatePaymentFailed(message)
    }
}

internal fun mapStatusToPaymentEvent(status: String, paymentId: String): Pos.PaymentEvent {
    return when (status) {
        PaymentStatus.REQUIRES_ACTION -> Pos.PaymentEvent.PaymentRequested
        PaymentStatus.PROCESSING -> Pos.PaymentEvent.PaymentProcessing
        PaymentStatus.SUCCEEDED -> Pos.PaymentEvent.PaymentSuccess(paymentId)
        PaymentStatus.EXPIRED -> Pos.PaymentEvent.PaymentError.PaymentExpired("Payment has expired")
        PaymentStatus.FAILED -> Pos.PaymentEvent.PaymentError.PaymentFailed("Payment failed") //TODO: add error message?
        else -> Pos.PaymentEvent.PaymentError.Undefined("Unknown payment status: $status")
    }
}