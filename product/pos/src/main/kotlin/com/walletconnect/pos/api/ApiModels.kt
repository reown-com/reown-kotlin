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
    @param:Json(name = "pollInMs") val pollInMs: Long,
    @param:Json(name = "gatewayUrl") val gatewayUrl: String
)

@JsonClass(generateAdapter = true)
internal data class GetPaymentStatusResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "pollInMs") val pollInMs: Long?,
    @param:Json(name = "isFinal") val isFinal: Boolean,
    @param:Json(name = "info") val info: PaymentInfoDto?
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
    @param:Json(name = "status") val status: String,
    @param:Json(name = "error") val error: ApiErrorDetails
)

@JsonClass(generateAdapter = true)
internal data class ApiErrorDetails(
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
    @param:Json(name = "merchantId") val merchantId: String?,
    @param:Json(name = "referenceId") val referenceId: String?,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "isTerminal") val isTerminal: Boolean,
    @param:Json(name = "fiatAmount") val fiatAmount: AmountWithDisplayDto?,
    @param:Json(name = "tokenAmount") val tokenAmount: AmountWithDisplayDto?,
    @param:Json(name = "buyer") val buyer: BuyerDto?,
    @param:Json(name = "transaction") val transaction: TransactionInfoDto?,
    @param:Json(name = "settlement") val settlement: SettlementDto?,
    @param:Json(name = "createdAt") val createdAt: String?,
    @param:Json(name = "lastUpdatedAt") val lastUpdatedAt: String?,
    @param:Json(name = "settledAt") val settledAt: String?
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
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "txHash") val txHash: String?
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
