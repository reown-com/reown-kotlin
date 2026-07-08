package com.walletconnect.sample.pos.printer

import com.walletconnect.pos.Pos
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal data class ReceiptData(
    val txId: String,
    val date: String,
    val displayFiat: String,
    val tokenSymbol: String?,
    val tokenAmountFormatted: String?,
    val network: String?,
    val footerOverride: String? = null
) {
    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        fun from(displayFiat: String, info: Pos.PaymentInfo?): ReceiptData {
            val tokenAmount = info?.let { paymentInfo ->
                val amount = paymentInfo.amount
                val decimals = paymentInfo.decimals
                if (amount == null || decimals == null) null
                else {
                    val value = amount.toBigDecimalOrNull() ?: return@let null
                    val divisor = java.math.BigDecimal.TEN.pow(decimals)
                    value.divide(divisor, decimals, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString()
                }
            }
            return ReceiptData(
                txId = info?.txHash ?: "",
                date = DATE_FORMAT.format(LocalDateTime.now()),
                displayFiat = displayFiat,
                tokenSymbol = info?.assetSymbol,
                tokenAmountFormatted = tokenAmount,
                network = info?.networkName
            )
        }

        fun sample() = ReceiptData(
            txId = "0xTEST0000000000000000000000000000000000000000",
            date = DATE_FORMAT.format(LocalDateTime.now()),
            displayFiat = "$1.00 USD",
            tokenSymbol = "USDC",
            tokenAmountFormatted = "1.00",
            network = "Ethereum",
            footerOverride = "Thank you for your payment!"
        )
    }
}
