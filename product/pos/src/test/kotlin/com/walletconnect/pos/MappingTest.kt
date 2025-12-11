package com.walletconnect.pos

import com.walletconnect.pos.api.ErrorCodes
import com.walletconnect.pos.api.PaymentStatus
import com.walletconnect.pos.api.isTerminalError
import com.walletconnect.pos.api.mapCreatePaymentError
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
        assertEquals(Pos.PaymentEvent.PaymentRequested, result)
    }

    @Test
    fun `mapStatusToPaymentEvent - processing returns PaymentProcessing`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.PROCESSING, "pay_123")
        assertEquals(Pos.PaymentEvent.PaymentProcessing, result)
    }

    @Test
    fun `mapStatusToPaymentEvent - succeeded returns PaymentSuccess with paymentId`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.SUCCEEDED, "pay_123")
        assertTrue(result is Pos.PaymentEvent.PaymentSuccess)
        assertEquals("pay_123", (result as Pos.PaymentEvent.PaymentSuccess).paymentId)
    }

    @Test
    fun `mapStatusToPaymentEvent - succeeded preserves exact paymentId`() {
        val paymentId = "wcp_payment_7XJkF2nPqR9vL5mT3hYwZ6aB4cD8eG1j"
        val result = mapStatusToPaymentEvent(PaymentStatus.SUCCEEDED, paymentId)
        assertEquals(paymentId, (result as Pos.PaymentEvent.PaymentSuccess).paymentId)
    }

    @Test
    fun `mapStatusToPaymentEvent - expired returns PaymentError PaymentExpired`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.EXPIRED, "pay_123")
        assertTrue(result is Pos.PaymentEvent.PaymentError.PaymentExpired)
    }

    @Test
    fun `mapStatusToPaymentEvent - failed returns PaymentError PaymentFailed`() {
        val result = mapStatusToPaymentEvent(PaymentStatus.FAILED, "pay_123")
        assertTrue(result is Pos.PaymentEvent.PaymentError.PaymentFailed)
    }

    @Test
    fun `mapStatusToPaymentEvent - unknown status returns PaymentError Undefined`() {
        val result = mapStatusToPaymentEvent("unknown_status", "pay_123")
        assertTrue(result is Pos.PaymentEvent.PaymentError.Undefined)
    }

    @Test
    fun `mapStatusToPaymentEvent - empty status returns PaymentError Undefined`() {
        val result = mapStatusToPaymentEvent("", "pay_123")
        assertTrue(result is Pos.PaymentEvent.PaymentError.Undefined)
        assertTrue((result as Pos.PaymentEvent.PaymentError.Undefined).message.contains("Unknown"))
    }

    @Test
    fun `mapStatusToPaymentEvent - case sensitive status check`() {
        val result = mapStatusToPaymentEvent("SUCCEEDED", "pay_123")
        assertTrue(result is Pos.PaymentEvent.PaymentError.Undefined)
    }

    @Test
    fun `mapErrorCodeToPaymentError - PAYMENT_NOT_FOUND returns PaymentNotFound`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.PAYMENT_NOT_FOUND, "Not found")
        assertTrue(result is Pos.PaymentEvent.PaymentError.PaymentNotFound)
        assertEquals("Not found", (result as Pos.PaymentEvent.PaymentError.PaymentNotFound).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - PAYMENT_EXPIRED returns PaymentExpired`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.PAYMENT_EXPIRED, "Expired")
        assertTrue(result is Pos.PaymentEvent.PaymentError.PaymentExpired)
        assertEquals("Expired", (result as Pos.PaymentEvent.PaymentError.PaymentExpired).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - INVALID_REQUEST returns InvalidPaymentRequest`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.INVALID_REQUEST, "Invalid")
        assertTrue(result is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest)
        assertEquals("Invalid", (result as Pos.PaymentEvent.PaymentError.InvalidPaymentRequest).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - unknown code returns Undefined`() {
        val result = mapErrorCodeToPaymentError("UNKNOWN_CODE", "Unknown error")
        assertTrue(result is Pos.PaymentEvent.PaymentError.Undefined)
        assertEquals("Unknown error", (result as Pos.PaymentEvent.PaymentError.Undefined).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - empty code returns Undefined`() {
        val result = mapErrorCodeToPaymentError("", "Some error")
        assertTrue(result is Pos.PaymentEvent.PaymentError.Undefined)
        assertEquals("Some error", (result as Pos.PaymentEvent.PaymentError.Undefined).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - preserves detailed error message`() {
        val detailedMessage = "Payment with ID pay_123 was not found in the system"
        val result = mapErrorCodeToPaymentError(ErrorCodes.PAYMENT_NOT_FOUND, detailedMessage)
        assertEquals(detailedMessage, (result as Pos.PaymentEvent.PaymentError.PaymentNotFound).message)
    }

    @Test
    fun `mapCreatePaymentError - INVALID_REQUEST returns InvalidPaymentRequest`() {
        val result = mapCreatePaymentError(ErrorCodes.INVALID_REQUEST, "Invalid amount")
        assertTrue(result is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest)
        assertEquals("Invalid amount", (result as Pos.PaymentEvent.PaymentError.InvalidPaymentRequest).message)
    }

    @Test
    fun `mapCreatePaymentError - unknown code returns CreatePaymentFailed`() {
        val result = mapCreatePaymentError("SOME_ERROR", "Something went wrong")
        assertTrue(result is Pos.PaymentEvent.PaymentError.CreatePaymentFailed)
        assertEquals("Something went wrong", (result as Pos.PaymentEvent.PaymentError.CreatePaymentFailed).message)
    }

    @Test
    fun `mapCreatePaymentError - network error returns CreatePaymentFailed`() {
        val result = mapCreatePaymentError("NETWORK_ERROR", "Connection timeout")
        assertTrue(result is Pos.PaymentEvent.PaymentError.CreatePaymentFailed)
        assertEquals("Connection timeout", (result as Pos.PaymentEvent.PaymentError.CreatePaymentFailed).message)
    }

    @Test
    fun `mapCreatePaymentError - empty message preserved`() {
        val result = mapCreatePaymentError("ERROR", "")
        assertTrue(result is Pos.PaymentEvent.PaymentError.CreatePaymentFailed)
        assertEquals("", (result as Pos.PaymentEvent.PaymentError.CreatePaymentFailed).message)
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
    fun `isTerminalError - INVALID_REQUEST is terminal`() {
        assertTrue(isTerminalError(ErrorCodes.INVALID_REQUEST))
    }

    @Test
    fun `isTerminalError - COMPLIANCE_FAILED is terminal`() {
        assertTrue(isTerminalError(ErrorCodes.COMPLIANCE_FAILED))
    }

    @Test
    fun `isTerminalError - unknown error is not terminal`() {
        assertFalse(isTerminalError("UNKNOWN_ERROR"))
    }

    @Test
    fun `isTerminalError - NETWORK_ERROR is not terminal`() {
        assertFalse(isTerminalError("NETWORK_ERROR"))
    }

    @Test
    fun `isTerminalError - empty string is not terminal`() {
        assertFalse(isTerminalError(""))
    }

    @Test
    fun `Amount format - USD formats correctly`() {
        val amount = Pos.Amount("iso4217/USD", "1000")
        assertEquals("10.00 USD", amount.format())
    }

    @Test
    fun `Amount format - EUR formats correctly`() {
        val amount = Pos.Amount("iso4217/EUR", "1500")
        assertEquals("15.00 EUR", amount.format())
    }

    @Test
    fun `Amount format - handles zero value`() {
        val amount = Pos.Amount("iso4217/USD", "0")
        assertEquals("0.00 USD", amount.format())
    }

    @Test
    fun `Amount format - handles small amounts`() {
        val amount = Pos.Amount("iso4217/USD", "1")
        assertEquals("0.01 USD", amount.format())
    }

    @Test
    fun `Amount format - handles large amounts`() {
        val amount = Pos.Amount("iso4217/USD", "1000000")
        assertEquals("10000.00 USD", amount.format())
    }

    @Test
    fun `Amount format - handles invalid value gracefully`() {
        val amount = Pos.Amount("iso4217/USD", "invalid")
        assertEquals("0.00 USD", amount.format())
    }

    @Test
    fun `Amount format - handles missing currency prefix`() {
        val amount = Pos.Amount("USD", "1000")
        assertEquals("10.00 ", amount.format())
    }

    @Test
    fun `Amount format - handles GBP currency`() {
        val amount = Pos.Amount("iso4217/GBP", "5000")
        assertEquals("50.00 GBP", amount.format())
    }

    @Test
    fun `Amount format - handles JPY currency`() {
        val amount = Pos.Amount("iso4217/JPY", "10000")
        assertEquals("100.00 JPY", amount.format())
    }

    @Test
    fun `PaymentStatus constants have correct values`() {
        assertEquals("requires_action", PaymentStatus.REQUIRES_ACTION)
        assertEquals("processing", PaymentStatus.PROCESSING)
        assertEquals("succeeded", PaymentStatus.SUCCEEDED)
        assertEquals("expired", PaymentStatus.EXPIRED)
        assertEquals("failed", PaymentStatus.FAILED)
    }

    @Test
    fun `ErrorCodes constants have correct values`() {
        assertEquals("PAYMENT_NOT_FOUND", ErrorCodes.PAYMENT_NOT_FOUND)
        assertEquals("PAYMENT_EXPIRED", ErrorCodes.PAYMENT_EXPIRED)
        assertEquals("INVALID_REQUEST", ErrorCodes.INVALID_REQUEST)
        assertEquals("COMPLIANCE_FAILED", ErrorCodes.COMPLIANCE_FAILED)
    }
}
