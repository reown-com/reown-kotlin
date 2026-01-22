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

    /**
     * Payment info returned when a payment succeeds.
     */
    data class PaymentInfo(
        val assetName: String?,
        val assetSymbol: String?,
        val networkName: String?,
        val amount: String?,
        val decimals: Int?,
        val txHash: String,
        val iconUrl: String?,
        val networkIconUrl: String?
    ) {
        /**
         * Formats the amount for display using the token decimals.
         */
        fun formatAmount(): String {
            if (amount == null || decimals == null) return amount ?: ""
            val value = amount.toBigDecimalOrNull() ?: return amount
            val divisor = java.math.BigDecimal.TEN.pow(decimals)
            val formatted = value.divide(divisor, decimals, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
            return "$formatted ${assetSymbol ?: ""}"
        }
    }

    sealed interface PaymentEvent {
        data class PaymentCreated(val uri: URI, val amount: Amount, val paymentId: String) : PaymentEvent
        data object PaymentRequested : PaymentEvent
        data object PaymentProcessing : PaymentEvent
        data class PaymentSuccess(val paymentId: String, val info: PaymentInfo?) : PaymentEvent
        sealed interface PaymentError : PaymentEvent {
            data class CreatePaymentFailed(val message: String) : PaymentError
            data class PaymentFailed(val message: String) : PaymentError
            data class PaymentNotFound(val message: String) : PaymentError
            data class PaymentExpired(val message: String) : PaymentError
            data class InvalidPaymentRequest(val message: String) : PaymentError
            data class Undefined(val message: String) : PaymentError
        }
    }

    /**
     * Represents a single transaction/payment record from history.
     */
    data class Transaction(
        val paymentId: String,
        val referenceId: String?,
        val status: TransactionStatus,
        val txHash: String?,
        val fiatAmount: Int?,
        val fiatCurrency: String?,
        val tokenAmount: String?,
        val tokenSymbol: String?,
        val network: String?,
        val chainId: String?,
        val walletName: String,
        val createdAt: String?,
        val confirmedAt: String?
    ) {
        /**
         * Formats the fiat amount for display (e.g., "$10.00 USD").
         */
        fun formatFiatAmount(): String? {
            if (fiatAmount == null || fiatCurrency == null) return null
            val majorUnits = fiatAmount / 100.0
            return String.format("%.2f %s", majorUnits, fiatCurrency)
        }
    }

    /**
     * Status of a transaction.
     */
    enum class TransactionStatus(val apiValue: String) {
        REQUIRES_ACTION("requires_action"),
        PROCESSING("processing"),
        SUCCEEDED("succeeded"),
        EXPIRED("expired"),
        FAILED("failed"),
        UNKNOWN("unknown")
    }

    /**
     * Result from transaction history query.
     */
    data class TransactionHistoryResult(
        val transactions: List<Transaction>,
        val hasMore: Boolean,
        val nextCursor: String?,
        val stats: TransactionStats?
    )

    /**
     * Aggregated statistics for transaction history.
     */
    data class TransactionStats(
        val totalTransactions: Int,
        val totalCustomers: Int,
        val totalRevenue: TotalRevenue?
    )

    /**
     * Total revenue information.
     */
    data class TotalRevenue(
        val amount: Int,
        val currency: String
    ) {
        /**
         * Formats the revenue for display (e.g., "1000.00 USD").
         */
        fun format(): String {
            val majorUnits = amount / 100.0
            return String.format("%.2f %s", majorUnits, currency)
        }
    }
}

interface POSDelegate {
    fun onEvent(event: Pos.PaymentEvent)
}
