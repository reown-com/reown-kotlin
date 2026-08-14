package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentContentTest {

    // ----- getResultContent: success -----

    @Test
    fun `success uses the dynamic summary as the title with a Done close action`() {
        val content = getResultContent(
            isSuccess = true,
            errorType = null,
            successSummary = "You've paid 5.00 USDT to Acme",
        )

        assertEquals("You've paid 5.00 USDT to Acme", content.title)
        assertNull(content.description)
        assertEquals("pay-result-success-icon", content.iconTestId)
        assertEquals("Done", content.actionLabel)
        assertEquals(ResultActionKind.CLOSE, content.actionKind)
    }

    @Test
    fun `success falls back to the default title when no summary is given`() {
        assertEquals("Payment confirmed", getResultContent(true, null).title)
        assertEquals("Payment confirmed", getResultContent(true, null, successSummary = " ").title)
    }

    // ----- getResultContent: errors -----

    @Test
    fun `insufficient funds maps to its title, description, icon and close action`() {
        val content = getResultContent(false, PaymentErrorType.INSUFFICIENT_FUNDS)

        assertEquals("Not enough funds in your wallet", content.title)
        assertTrue(content.description!!.contains("Add funds, or pay"))
        assertEquals("pay-result-insufficient-funds-icon", content.iconTestId)
        assertEquals("Got it", content.actionLabel)
        assertEquals(ResultActionKind.CLOSE, content.actionKind)
    }

    @Test
    fun `expired and cancelled route to the scan-QR action`() {
        val expired = getResultContent(false, PaymentErrorType.EXPIRED)
        assertEquals("Payment request expired", expired.title)
        assertEquals("pay-result-expired-icon", expired.iconTestId)
        assertEquals("Scan a new QR code", expired.actionLabel)
        assertEquals(ResultActionKind.SCAN_QR, expired.actionKind)

        val cancelled = getResultContent(false, PaymentErrorType.CANCELLED)
        assertEquals("Payment request cancelled", cancelled.title)
        assertEquals("pay-result-cancelled-icon", cancelled.iconTestId)
        assertEquals("Scan a new QR code", cancelled.actionLabel)
        assertEquals(ResultActionKind.SCAN_QR, cancelled.actionKind)
    }

    @Test
    fun `not found maps to its title, description and close action`() {
        val content = getResultContent(false, PaymentErrorType.NOT_FOUND)

        assertEquals("Payment request not found", content.title)
        assertTrue(content.description!!.contains("isn't valid"))
        assertEquals("pay-result-error-icon", content.iconTestId)
        assertEquals("Close", content.actionLabel)
        assertEquals(ResultActionKind.CLOSE, content.actionKind)
    }

    @Test
    fun `generic uses the raw error message as the description, then a default`() {
        assertEquals(
            "boom",
            getResultContent(false, PaymentErrorType.GENERIC, rawErrorMessage = "boom").description,
        )
        assertTrue(
            getResultContent(false, PaymentErrorType.GENERIC).description!!.contains("No funds were moved"),
        )
    }

    @Test
    fun `an error with no classified type is treated as generic`() {
        val content = getResultContent(false, errorType = null)

        assertEquals("Payment didn't go through", content.title)
        assertEquals("pay-result-error-icon", content.iconTestId)
        assertEquals(ResultActionKind.CLOSE, content.actionKind)
    }

    // ----- getLoadingContent -----

    @Test
    fun `loading returns the step default with no note when nothing is being set up`() {
        assertEquals(
            PaymentLoadingContent("Preparing your payment…"),
            getLoadingContent(LoadingStep.PREPARING),
        )
        assertEquals(
            PaymentLoadingContent("Confirming your payment…"),
            getLoadingContent(LoadingStep.CONFIRMING),
        )
    }

    @Test
    fun `loading returns the token-setup message and note when a token is being set up`() {
        assertEquals(
            PaymentLoadingContent(
                message = "Setting up USDT",
                note = "This usually takes a few seconds. Future USDT payments will skip this step.",
            ),
            getLoadingContent(LoadingStep.CONFIRMING, setupTokenSymbol = "USDT"),
        )
    }

    @Test
    fun `the setup token takes precedence over the step default`() {
        assertEquals(
            "Setting up USDC",
            getLoadingContent(LoadingStep.PREPARING, setupTokenSymbol = "USDC").message,
        )
    }
}
