package com.walletconnect.pos.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Amount(
    @param:Json(name = "unit") val unit: String,
    @param:Json(name = "value") val value: String
)

@JsonClass(generateAdapter = true)
internal data class CreatePaymentRequest(
    @param:Json(name = "referenceId") val referenceId: String? = null,
    @param:Json(name = "amount") val amount: Amount
)

@JsonClass(generateAdapter = true)
internal data class CreatePaymentResponse(
    @param:Json(name = "paymentId") val paymentId: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "amount") val amount: Amount,
    @param:Json(name = "expiresAt") val expiresAt: Long,
    @param:Json(name = "pollInMs") val pollInMs: Long,
    @param:Json(name = "gatewayUrl") val gatewayUrl: String
)

@JsonClass(generateAdapter = true)
internal data class GetPaymentStatusResponse(
    @param:Json(name = "paymentId") val paymentId: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "pollInMs") val pollInMs: Long
)

@JsonClass(generateAdapter = true)
internal data class ApiErrorResponse(
    @param:Json(name = "code") val code: String,
    @param:Json(name = "message") val message: String
)

internal sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
}

internal object PaymentStatus {
    const val REQUIRES_ACTION = "requires_action"
    const val PROCESSING = "processing"
    const val SUCCEEDED = "succeeded"
    const val EXPIRED = "expired"
    const val FAILED = "failed"
}

internal object ErrorCodes {
    const val PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND"
    const val PAYMENT_EXPIRED = "PAYMENT_EXPIRED"
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val COMPLIANCE_FAILED = "COMPLIANCE_FAILED"
    const val NETWORK_ERROR = "NETWORK_ERROR"
    const val PARSE_ERROR = "PARSE_ERROR"
}
