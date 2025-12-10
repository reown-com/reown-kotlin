package com.walletconnect.pos

import com.walletconnect.pos.api.ErrorCodes
import com.walletconnect.pos.api.PaymentStatus
import com.walletconnect.pos.api.buildPaymentUri
import com.walletconnect.pos.api.formatAmount
import com.walletconnect.pos.api.isTerminalError
import com.walletconnect.pos.api.isTerminalStatus
import com.walletconnect.pos.api.mapErrorCodeToPaymentError
import com.walletconnect.pos.api.mapStatusToPaymentEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingTest {

    @Test
    fun `mapStatusToPaymentEvent - requires_action returns PaymentRequested`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.REQUIRES_ACTION, "pay_123")
        assertEquals(Pos.Model.PaymentEvent.PaymentRequested, result)
    }

    @Test
    fun `mapStatusToPaymentEvent - processing returns PaymentProcessing`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.PROCESSING, "pay_123")
        assertEquals(Pos.Model.PaymentEvent.PaymentProcessing, result)
    }

    @Test
    fun `mapStatusToPaymentEvent - succeeded returns PaymentSuccess with paymentId`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.SUCCEEDED, "pay_123")
        assertTrue(result is Pos.Model.PaymentEvent.PaymentSuccess)
        assertEquals("pay_123", (result as Pos.Model.PaymentEvent.PaymentSuccess).paymentId)
    }

    @Test
    fun `mapStatusToPaymentEvent - expired returns PaymentError with PaymentExpired`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.EXPIRED, "pay_123")
        assertTrue(result is Pos.Model.PaymentEvent.PaymentError)
        val error = (result as Pos.Model.PaymentEvent.PaymentError).error
        assertTrue(error is Pos.Model.PaymentError.PaymentExpired)
    }

    @Test
    fun `mapStatusToPaymentEvent - failed returns PaymentError with PaymentFailed`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.FAILED, "pay_123")
        assertTrue(result is Pos.Model.PaymentEvent.PaymentError)
        val error = (result as Pos.Model.PaymentEvent.PaymentError).error
        assertTrue(error is Pos.Model.PaymentError.PaymentFailed)
    }

    @Test
    fun `mapStatusToPaymentEvent - unknown status returns PaymentError with Generic`() {
        val result = mapStatusToPaymentEvent("unknown_status", "pay_123")
        assertTrue(result is Pos.Model.PaymentEvent.PaymentError)
        val error = (result as Pos.Model.PaymentEvent.PaymentError).error
        assertTrue(error is Pos.Model.PaymentError.Generic)
    }

    @Test
    fun `mapErrorCodeToPaymentError - PAYMENT_NOT_FOUND returns PaymentNotFound`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.PAYMENT_NOT_FOUND, "Not found")
        assertTrue(result is Pos.Model.PaymentError.PaymentNotFound)
        assertEquals("Not found", (result as Pos.Model.PaymentError.PaymentNotFound).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - PAYMENT_EXPIRED returns PaymentExpired`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.PAYMENT_EXPIRED, "Expired")
        assertTrue(result is Pos.Model.PaymentError.PaymentExpired)
        assertEquals("Expired", (result as Pos.Model.PaymentError.PaymentExpired).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - INVALID_REQUEST returns InvalidPaymentRequest`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.INVALID_REQUEST, "Invalid")
        assertTrue(result is Pos.Model.PaymentError.InvalidPaymentRequest)
        assertEquals("Invalid", (result as Pos.Model.PaymentError.InvalidPaymentRequest).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - unknown code returns Generic`() {
        val result = mapErrorCodeToPaymentError("UNKNOWN_CODE", "Unknown error")
        assertTrue(result is Pos.Model.PaymentError.Generic)
        assertEquals("Unknown error", (result as Pos.Model.PaymentError.Generic).message)
    }

    @Test
    fun `isTerminalStatus - succeeded is terminal`() {
        assertTrue(isTerminalStatus(PaymentStatus.SUCCEEDED))
    }

    @Test
    fun `isTerminalStatus - expired is terminal`() {
        assertTrue(isTerminalStatus(PaymentStatus.EXPIRED))
    }

    @Test
    fun `isTerminalStatus - failed is terminal`() {
        assertTrue(isTerminalStatus(PaymentStatus.FAILED))
    }

    @Test
    fun `isTerminalStatus - requires_action is not terminal`() {
        assertFalse(isTerminalStatus(PaymentStatus.REQUIRES_ACTION))
    }

    @Test
    fun `isTerminalStatus - processing is not terminal`() {
        assertFalse(isTerminalStatus(PaymentStatus.PROCESSING))
    }

    @Test
    fun `isTerminalError - PAYMENT_NOT_FOUND is terminal`() {
        assertTrue(isTerminalError(ErrorCodes.PAYMENT_NOT_FOUND))
    }

    @Test
    fun `isTerminalError - PAYMENT_EXPIRED is terminal`() {
        assertTrue(isTerminalError(ErrorCodes.PAYMENT_EXPIRED))
    }

    @Test
    fun `isTerminalError - INVALID_REQUEST is not terminal`() {
        assertFalse(isTerminalError(ErrorCodes.INVALID_REQUEST))
    }

    @Test
    fun `formatAmount - USD formats correctly`() {
        val result = formatAmount("iso4217/USD", "1000")
        assertEquals("$10.00 USD", result)
    }

    @Test
    fun `formatAmount - EUR formats correctly`() {
        val result = formatAmount("iso4217/EUR", "1500")
        assertEquals("€15.00 EUR", result)
    }

    @Test
    fun `formatAmount - GBP formats correctly`() {
        val result = formatAmount("iso4217/GBP", "2500")
        assertEquals("£25.00 GBP", result)
    }

    @Test
    fun `formatAmount - unknown currency formats with code`() {
        val result = formatAmount("iso4217/JPY", "10000")
        assertEquals("100.00 JPY", result)
    }

    @Test
    fun `buildPaymentUri - builds correct URI`() {
        val result = buildPaymentUri("pay_abc123")
        assertEquals("https://walletconnect.com/pay/pay_abc123", result)
    }
}
