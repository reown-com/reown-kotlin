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
    @param:Json(name = "expiresAt") val expiresAt: Long,
    @param:Json(name = "pollInMs") val pollInMs: Long?,
    @param:Json(name = "isFinal") val isFinal: Boolean,
    @param:Json(name = "gatewayUrl") val gatewayUrl: String
)

@JsonClass(generateAdapter = true)
internal data class GetPaymentStatusResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "pollInMs") val pollInMs: Long?,
    @param:Json(name = "isFinal") val isFinal: Boolean,
    @param:Json(name = "info") val info: PaymentInfoDto?,
    @param:Json(name = "failureCode") val failureCode: String? = null
)

@JsonClass(generateAdapter = true)
internal data class PaymentInfoDto(
    @param:Json(name = "optionAmount") val optionAmount: OptionAmountDto,
    @param:Json(name = "txId") val txId: String
)

@JsonClass(generateAdapter = true)
internal data class OptionAmountDto(
    @param:Json(name = "unit") val unit: String?,
    @param:Json(name = "value") val value: String?,
    @param:Json(name = "display") val display: DisplayAmountDto
)

@JsonClass(generateAdapter = true)
internal data class DisplayAmountDto(
    @param:Json(name = "assetName") val assetName: String?,
    @param:Json(name = "assetSymbol") val assetSymbol: String?,
    @param:Json(name = "decimals") val decimals: Int?,
    @param:Json(name = "iconUrl") val iconUrl: String?,
    @param:Json(name = "networkIconUrl") val networkIconUrl: String?,
    @param:Json(name = "networkName") val networkName: String?
)

@JsonClass(generateAdapter = true)
internal data class ApiErrorWrapper(
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "error") val error: ApiErrorDetails
)

@JsonClass(generateAdapter = true)
internal data class ApiErrorDetails(
    @param:Json(name = "code") val code: String,
    @param:Json(name = "message") val message: String
)

// Flat error shape used by /v1/payments and /v1/refunds (2026-02-18).
@JsonClass(generateAdapter = true)
internal data class ApiErrorFlat(
    @param:Json(name = "code") val code: String?,
    @param:Json(name = "message") val message: String?
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
    const val CANCELLED = "cancelled"
}

internal object ErrorCodes {
    // API-origin codes — values match the server's ErrorCode enum (lowercase snake_case).
    const val PAYMENT_NOT_FOUND = "payment_not_found"
    const val PAYMENT_EXPIRED = "payment_expired"
    const val INVALID_PARAMS = "invalid_params"
    const val PARAMS_VALIDATION = "params_validation"

    // Refund-specific codes from POST /v1/refunds (2026-02-18).
    const val NOT_FOUND = "not_found"
    const val ALREADY_REFUNDED = "already_refunded"
    const val PAYMENT_NOT_SUCCEEDED = "payment_not_succeeded"

    // Auth family (shared by partner API surfaces).
    const val MISSING_API_KEY = "missing_api_key"
    const val INVALID_API_KEY = "invalid_api_key"
    const val MISSING_MERCHANT_ID = "missing_merchant_id"

    // API-version validation (returned by 2026-02-18 POST /v1/refunds).
    const val INVALID_API_VERSION = "invalid_api_version"
    const val UNKNOWN_API_VERSION = "unknown_api_version"
    const val API_VERSION_DOWNGRADE = "api_version_downgrade"

    // Server family.
    const val INTERNAL_ERROR = "internal_error"
    const val BAD_GATEWAY = "bad_gateway"

    // Internal sentinels — generated client-side, never received from the API.
    const val NETWORK_ERROR = "NETWORK_ERROR"
    const val PARSE_ERROR = "PARSE_ERROR"
}

internal object FailureCodes {
    // Wire values of GetPaymentStatusResponse.failureCode when status == "failed".
    const val DECLINED_USER = "declined_user"
}

internal object RefundStatus {
    const val FULLY_REFUNDED = "fully_refunded"
}

// Transaction History Models

@JsonClass(generateAdapter = true)
internal data class TransactionHistoryResponse(
    @param:Json(name = "data") val data: List<PaymentRecord>,
    @param:Json(name = "stats") val stats: TransactionStatsDto?,
    @param:Json(name = "nextCursor") val nextCursor: String?
)

@JsonClass(generateAdapter = true)
internal data class PaymentRecord(
    @param:Json(name = "paymentId") val paymentId: String,
    @param:Json(name = "merchant") val merchant: MerchantDto?,
    @param:Json(name = "referenceId") val referenceId: String?,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "isTerminal") val isTerminal: Boolean,
    @param:Json(name = "fiatAmount") val fiatAmount: AmountWithDisplayDto?,
    @param:Json(name = "tokenAmount") val tokenAmount: AmountWithDisplayDto?,
    @param:Json(name = "buyer") val buyer: BuyerDto?,
    @param:Json(name = "transaction") val transaction: TransactionInfoDto?,
    @param:Json(name = "settlement") val settlement: SettlementDto?,
    @param:Json(name = "refund") val refund: PaymentRefundDto?,
    @param:Json(name = "createdAt") val createdAt: String?,
    @param:Json(name = "lastUpdatedAt") val lastUpdatedAt: String?,
    @param:Json(name = "settledAt") val settledAt: String?
)

@JsonClass(generateAdapter = true)
internal data class MerchantDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "iconUrl") val iconUrl: String?,
)

@JsonClass(generateAdapter = true)
internal data class PaymentRefundDto(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "fullyRefundedAt") val fullyRefundedAt: String?
)

@JsonClass(generateAdapter = true)
internal data class AmountWithDisplayDto(
    @param:Json(name = "unit") val unit: String?,
    @param:Json(name = "value") val value: String?,
    @param:Json(name = "display") val display: DisplayAmountDto?
)

@JsonClass(generateAdapter = true)
internal data class BuyerDto(
    @param:Json(name = "accountCaip10") val accountCaip10: String?,
    @param:Json(name = "accountProviderName") val accountProviderName: String?,
    @param:Json(name = "accountProviderIcon") val accountProviderIcon: String?
)

@JsonClass(generateAdapter = true)
internal data class TransactionInfoDto(
    @param:Json(name = "networkId") val networkId: String?,
    @param:Json(name = "hash") val hash: String?,
    @param:Json(name = "nonce") val nonce: Int?
)

@JsonClass(generateAdapter = true)
internal data class SettlementDto(
    @param:Json(name = "settled") val settled: Boolean,
    @param:Json(name = "txHash") val txHash: String?,
    @param:Json(name = "amount") val amount: AmountWithDisplayDto?,
)

@JsonClass(generateAdapter = true)
internal data class TransactionStatsDto(
    @param:Json(name = "totalTransactions") val totalTransactions: Int,
    @param:Json(name = "totalRevenue") val totalRevenue: List<TotalRevenueDto>?,
    @param:Json(name = "totalCustomers") val totalCustomers: Int
)

@JsonClass(generateAdapter = true)
internal data class TotalRevenueDto(
    @param:Json(name = "amount") val amount: Double,
    @param:Json(name = "currency") val currency: String
)

// Refunds

@JsonClass(generateAdapter = true)
internal data class RefundRequest(
    @param:Json(name = "paymentId") val paymentId: String
)

@JsonClass(generateAdapter = true)
internal data class RefundResponse(
    @param:Json(name = "paymentId") val paymentId: String
)
