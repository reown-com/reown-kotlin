package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentReviewFormatterTest {

    @Test
    fun `payButtonLabel folds gas into total when currencies match`() {
        val info = paymentInfo(value = "250000", decimals = 2, assetSymbol = "EUR")
        val fee = feeEstimate(fiatValue = 0.01, fiatCurrency = "EUR")

        val label = PaymentReviewFormatter.payButtonLabel(info, fee)

        assertTrue(label.includesGasFee)
        assertEquals("€2,500.01", label.total)
    }

    @Test
    fun `payButtonLabel ignores gas when currencies mismatch`() {
        val info = paymentInfo(value = "250000", decimals = 2, assetSymbol = "EUR")
        val fee = feeEstimate(fiatValue = 0.01, fiatCurrency = "USD")

        val label = PaymentReviewFormatter.payButtonLabel(info, fee)

        assertFalse(label.includesGasFee)
        assertEquals("€2,500", label.total)
    }

    @Test
    fun `payButtonLabel ignores gas when fiat value is missing`() {
        val info = paymentInfo(value = "250000", decimals = 2, assetSymbol = "EUR")
        val fee = feeEstimate(fiatValue = null, fiatCurrency = null)

        val label = PaymentReviewFormatter.payButtonLabel(info, fee)

        assertFalse(label.includesGasFee)
        assertEquals("€2,500", label.total)
    }

    @Test
    fun `payButtonLabel returns plain amount when fee is null`() {
        val info = paymentInfo(value = "250000", decimals = 2, assetSymbol = "USD")

        val label = PaymentReviewFormatter.payButtonLabel(info, fee = null)

        assertFalse(label.includesGasFee)
        assertEquals("$2,500", label.total)
    }

    @Test
    fun `payButtonLabel handles missing payment info`() {
        val label = PaymentReviewFormatter.payButtonLabel(info = null, fee = null)

        assertFalse(label.includesGasFee)
        assertEquals("", label.total)
    }

    @Test
    fun `payButtonLabel rounds small fiat gas correctly`() {
        val info = paymentInfo(value = "9999", decimals = 2, assetSymbol = "USD")
        val fee = feeEstimate(fiatValue = 0.014, fiatCurrency = "usd")

        val label = PaymentReviewFormatter.payButtonLabel(info, fee)

        assertTrue(label.includesGasFee)
        assertEquals("$100.00", label.total)
    }

    @Test
    fun `payButtonLabel matches currency case-insensitively`() {
        val info = paymentInfo(value = "5000", decimals = 2, assetSymbol = "eur")
        val fee = feeEstimate(fiatValue = 0.05, fiatCurrency = "EUR")

        val label = PaymentReviewFormatter.payButtonLabel(info, fee)

        assertTrue(label.includesGasFee)
        assertEquals("€50.05", label.total)
    }

    private fun paymentInfo(value: String, decimals: Int, assetSymbol: String): Wallet.Model.PaymentInfo {
        return Wallet.Model.PaymentInfo(
            status = Wallet.Model.PaymentStatus.REQUIRES_ACTION,
            amount = Wallet.Model.PaymentAmount(
                value = value,
                unit = assetSymbol,
                display = Wallet.Model.PaymentAmountDisplay(
                    assetSymbol = assetSymbol,
                    assetName = assetSymbol,
                    decimals = decimals,
                    iconUrl = null,
                    networkName = null,
                    networkIconUrl = null,
                ),
            ),
            expiresAt = 0L,
            merchant = Wallet.Model.MerchantInfo(name = "Test", iconUrl = null),
        )
    }

    private fun feeEstimate(
        fiatValue: Double?,
        fiatCurrency: String?,
    ): TransactionFeeEstimate {
        return TransactionFeeEstimate(
            display = "0.012",
            nativeDisplay = "0.0012 POL",
            fiatValue = fiatValue,
            fiatCurrency = fiatCurrency,
            chainId = "eip155:137",
            nativeSymbol = "POL",
        )
    }
}
