package com.walletconnect.pos.internal

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

//todo: remove?
//internal fun formatAmount(unit: String, value: String): POS.Model.Amount {
//    val currency = unit.substringAfter("/", "")
//    val valueNum = value.toLongOrNull() ?: 0L
//
//    // For ISO 4217 currencies, convert from minor units (cents) to major units
//    val majorUnits = valueNum / 100.0
//    return POS.Model.Amount()
//
//    //TODO: just return as an Amount object
//    return when (currency.uppercase()) {
//        "USD" -> String.format("$%.2f USD", majorUnits)
//        "EUR" -> String.format("€%.2f EUR", majorUnits)
//        else -> String.format("%.2f %s", majorUnits, currency)
//    }
//}

internal fun buildPaymentUri(paymentId: String): String {
    return "https://walletconnect.com/pay/$paymentId"
}
