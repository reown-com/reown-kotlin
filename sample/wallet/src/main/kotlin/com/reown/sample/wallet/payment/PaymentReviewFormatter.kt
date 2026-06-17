@file:JvmSynthetic

package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

internal data class PayButtonLabel(
    val total: String,
    val includesGasFee: Boolean,
)

internal object PaymentReviewFormatter {

    fun formatDisplayAmount(value: String, decimals: Int, currencyCode: String): String {
        val symbol = getCurrencySymbol(currencyCode)
        return runCatching {
            val rawValue = BigDecimal(value)
            val safeDecimals = decimals.coerceIn(0, 18)
            val divisor = BigDecimal.TEN.pow(safeDecimals)
            val formattedValue = rawValue.divide(divisor, 2, RoundingMode.HALF_UP)
            val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 2
            }
            "$symbol${numberFormat.format(formattedValue)}"
        }.getOrElse { "$symbol$value" }
    }

    fun getCurrencySymbol(currencyCode: String): String = when (currencyCode.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "CNY" -> "¥"
        "KRW" -> "₩"
        "INR" -> "₹"
        "RUB" -> "₽"
        "BRL" -> "R$"
        "CHF" -> "CHF "
        "CAD" -> "CA$"
        "AUD" -> "A$"
        else -> "$currencyCode "
    }

    /**
     * Compute the Pay button label, optionally folding the fiat gas fee into the total.
     *
     * Returns the plain payment amount when no fee estimate is available, when the
     * fee has no fiat value, or when the gas-fee currency differs from the payment
     * currency (matching the RN implementation).
     */
    fun payButtonLabel(
        info: Wallet.Model.PaymentInfo?,
        fee: TransactionFeeEstimate?,
    ): PayButtonLabel {
        val amount = info?.amount ?: return PayButtonLabel("", false)
        val currencyCode = amount.display?.assetSymbol ?: amount.unit
        val decimals = amount.display?.decimals ?: 2
        val plain = formatDisplayAmount(amount.value, decimals, currencyCode)

        val fiat = fee?.fiatValue
        val feeCurrency = fee?.fiatCurrency
        val currenciesMatch = fiat != null &&
            feeCurrency != null &&
            feeCurrency.equals(currencyCode, ignoreCase = true)

        if (!currenciesMatch) return PayButtonLabel(plain, includesGasFee = false)

        val totalLabel = runCatching {
            val rawValue = BigDecimal(amount.value)
            val safeDecimals = decimals.coerceIn(0, 18)
            val divisor = BigDecimal.TEN.pow(safeDecimals)
            val baseAmount = rawValue.divide(divisor, 6, RoundingMode.HALF_UP)
            val total = baseAmount.add(BigDecimal.valueOf(fiat!!))
                .setScale(2, RoundingMode.HALF_UP)
            val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            "${getCurrencySymbol(currencyCode)}${numberFormat.format(total)}"
        }.getOrElse { plain }

        return PayButtonLabel(totalLabel, includesGasFee = true)
    }
}
