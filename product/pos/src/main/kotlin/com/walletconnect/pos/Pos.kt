package com.walletconnect.pos

import java.net.URI
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
        val amount: Double,
        val currency: String
    ) {
        /**
         * Formats the revenue for display (e.g., "19.49 USD").
         */
        fun format(): String {
            return String.format("%.2f %s", amount, currency)
        }
    }

    /**
     * Represents a time range for filtering transactions.
     *
     * @property startTime The start of the time range (inclusive)
     * @property endTime The end of the time range (inclusive)
     */
    data class DateRange(
        val startTime: Instant,
        val endTime: Instant
    ) {
        init {
            require(!endTime.isBefore(startTime)) { "endTime must not be before startTime" }
        }
    }

    /**
     * Convenience factory for creating common date ranges.
     *
     * **Important**: All date boundaries are calculated in UTC timezone, not the device's local timezone.
     * This means "today" refers to the current UTC calendar day, which may differ from the merchant's
     * local date depending on their timezone. For example, a merchant at 11 PM UTC-8 would see
     * transactions from the next UTC day.
     */
    object DateRanges {
        /**
         * Returns a DateRange for today (from midnight UTC to now).
         *
         * Note: "Today" is defined as the current UTC calendar day, not the device's local date.
         */
        fun today(): DateRange {
            val now = Instant.now()
            val startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
            return DateRange(startOfDay, now)
        }

        /**
         * Returns a DateRange for the last N days including today (in UTC).
         *
         * @param days Number of days to include (must be positive)
         */
        fun lastDays(days: Int): DateRange {
            require(days > 0) { "days must be positive" }
            val now = Instant.now()
            val startOfPeriod = LocalDate.now(ZoneOffset.UTC)
                .minusDays((days - 1).toLong())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
            return DateRange(startOfPeriod, now)
        }

        /**
         * Returns a DateRange for this week (Monday 00:00 UTC to now).
         *
         * Week starts on Monday per ISO-8601. Uses the most recent Monday,
         * which is the same day if today is Monday.
         */
        fun thisWeek(): DateRange {
            val now = Instant.now()
            val today = LocalDate.now(ZoneOffset.UTC)
            // Calculate days since Monday (Monday=1, Sunday=7) to get previous/same Monday
            val daysFromMonday = (today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
            val monday = today.minusDays(daysFromMonday)
            val startOfWeek = monday.atStartOfDay(ZoneOffset.UTC).toInstant()
            return DateRange(startOfWeek, now)
        }

        /**
         * Returns a DateRange for this month (1st of month 00:00 UTC to now).
         */
        fun thisMonth(): DateRange {
            val now = Instant.now()
            val firstOfMonth = LocalDate.now(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
            return DateRange(firstOfMonth, now)
        }
    }
}

interface POSDelegate {
    fun onEvent(event: Pos.PaymentEvent)
}
