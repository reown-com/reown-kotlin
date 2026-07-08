package com.walletconnect.pos.api

import com.squareup.moshi.Moshi
import com.walletconnect.pos.Pos

/**
 * Parses an HTTP error response body into [ApiErrorDetails].
 *
 * The API returns error bodies in one of two shapes:
 *  - wrapped: `{"status":"error","error":{"code":"...","message":"..."}}`
 *  - flat:    `{"code":"...","message":"..."}` (e.g. the 401 `invalid_api_key` response)
 *
 * Both are attempted. Returns `null` when the body is blank or matches neither shape,
 * letting the caller fall back to the HTTP status line. This is important because the
 * HTTP reason-phrase is empty under HTTP/2, so the body is the only source of a message.
 */
internal fun parseApiErrorBody(moshi: Moshi, errorBody: String?): ApiErrorDetails? {
    if (errorBody.isNullOrBlank()) return null

    runCatching { moshi.adapter(ApiErrorWrapper::class.java).fromJson(errorBody)?.error }
        .getOrNull()?.let { return it }

    runCatching { moshi.adapter(ApiErrorDetails::class.java).fromJson(errorBody) }
        .getOrNull()?.let { if (it.message.isNotBlank()) return it }

    return null
}

internal fun mapErrorCodeToPaymentError(code: String, message: String): Pos.PaymentEvent.PaymentError {
    return when (code) {
        ErrorCodes.PAYMENT_NOT_FOUND -> Pos.PaymentEvent.PaymentError.PaymentNotFound(message)
        ErrorCodes.PAYMENT_EXPIRED -> Pos.PaymentEvent.PaymentError.PaymentExpired(message)
        ErrorCodes.INVALID_PARAMS,
        ErrorCodes.PARAMS_VALIDATION -> Pos.PaymentEvent.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.PaymentEvent.PaymentError.Undefined(message)
    }
}

internal fun mapCreatePaymentError(code: String, message: String): Pos.PaymentEvent.PaymentError {
    return when (code) {
        ErrorCodes.INVALID_PARAMS,
        ErrorCodes.PARAMS_VALIDATION -> Pos.PaymentEvent.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.PaymentEvent.PaymentError.CreatePaymentFailed(message)
    }
}

internal fun mapStatusToPaymentEvent(
    status: String,
    paymentId: String,
    info: PaymentInfoDto? = null,
    failureCode: String? = null
): Pos.PaymentEvent {
    return when (status) {
        PaymentStatus.REQUIRES_ACTION -> Pos.PaymentEvent.PaymentRequested
        PaymentStatus.PROCESSING -> Pos.PaymentEvent.PaymentProcessing
        PaymentStatus.SUCCEEDED -> Pos.PaymentEvent.PaymentSuccess(paymentId, info?.toPaymentInfo())
        PaymentStatus.EXPIRED -> Pos.PaymentEvent.PaymentError.PaymentExpired("Payment has expired")
        PaymentStatus.FAILED -> when (failureCode) {
            FailureCodes.DECLINED_USER -> Pos.PaymentEvent.PaymentError.DeclinedUser
            else -> Pos.PaymentEvent.PaymentError.PaymentFailed("Payment failed")
        }
        PaymentStatus.CANCELLED -> Pos.PaymentEvent.PaymentError.PaymentCancelled("Payment cancelled")
        else -> Pos.PaymentEvent.PaymentError.Undefined("Unknown payment status: $status")
    }
}

internal fun PaymentInfoDto.toPaymentInfo(): Pos.PaymentInfo {
    val display = optionAmount.display
    return Pos.PaymentInfo(
        assetName = display.assetName,
        assetSymbol = display.assetSymbol,
        networkName = display.networkName,
        amount = optionAmount.value,
        decimals = display.decimals,
        txHash = txId,
        iconUrl = display.iconUrl,
        networkIconUrl = display.networkIconUrl
    )
}

// Transaction History Mapping Functions

internal fun PaymentRecord.toTransaction(): Pos.Transaction {
    return Pos.Transaction(
        paymentId = paymentId,
        referenceId = referenceId,
        status = mapToTransactionStatus(status),
        txHash = transaction?.hash,
        fiatAmount = fiatAmount?.value?.toLongOrNull(),
        fiatCurrency = extractCurrencyCode(fiatAmount?.unit),
        tokenAmount = tokenAmount?.value,
        tokenSymbol = tokenAmount?.display?.assetSymbol,
        tokenDecimals = tokenAmount?.display?.decimals,
        tokenLogo = tokenAmount?.display?.iconUrl,
        network = tokenAmount?.display?.networkName,
        chainId = transaction?.networkId,
        walletName = buyer?.accountProviderName ?: "Unknown",
        createdAt = createdAt,
        confirmedAt = settledAt
    )
}

internal fun mapToTransactionStatus(status: String): Pos.TransactionStatus {
    return when (status.lowercase()) {
        PaymentStatus.REQUIRES_ACTION -> Pos.TransactionStatus.REQUIRES_ACTION
        PaymentStatus.PROCESSING -> Pos.TransactionStatus.PROCESSING
        PaymentStatus.SUCCEEDED -> Pos.TransactionStatus.SUCCEEDED
        PaymentStatus.EXPIRED -> Pos.TransactionStatus.EXPIRED
        PaymentStatus.FAILED -> Pos.TransactionStatus.FAILED
        PaymentStatus.CANCELLED -> Pos.TransactionStatus.CANCELLED
        else -> Pos.TransactionStatus.UNKNOWN
    }
}

internal fun extractCurrencyCode(currency: String?): String? {
    if (currency == null) return null
    // Handle CAIP format like "iso4217/USD" or plain "USD"
    return if (currency.contains("/")) {
        currency.substringAfter("/")
    } else {
        currency
    }
}

internal fun TransactionStatsDto?.toTransactionStats(): Pos.TransactionStats? {
    if (this == null) return null
    return Pos.TransactionStats(
        totalTransactions = totalTransactions,
        totalCustomers = totalCustomers,
        totalRevenue = totalRevenue?.firstOrNull()?.let {
            Pos.TotalRevenue(
                amount = it.amount,
                currency = extractCurrencyCode(it.currency) ?: it.currency
            )
        }
    )
}
