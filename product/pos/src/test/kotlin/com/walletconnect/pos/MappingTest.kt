package com.walletconnect.pos

import com.walletconnect.pos.api.ErrorCodes
import com.walletconnect.pos.api.FailureCodes
import com.walletconnect.pos.api.PaymentRecord
import com.walletconnect.pos.api.PaymentRefundDto
import com.walletconnect.pos.api.PaymentStatus
import com.walletconnect.pos.api.mapCreatePaymentError
import com.walletconnect.pos.api.mapErrorCodeToPaymentError
import com.walletconnect.pos.api.mapRefundErrorCode
import com.walletconnect.pos.api.mapStatusToPaymentEvent
import com.walletconnect.pos.api.toTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    fun `mapStatusToPaymentEvent - failed with declined_user failureCode returns DeclinedUser`() {
        val result = mapStatusToPaymentEvent(
            status = PaymentStatus.FAILED,
            paymentId = "pay_123",
            failureCode = FailureCodes.DECLINED_USER
        )
        assertSame(Pos.PaymentEvent.PaymentError.DeclinedUser, result)
    }

    @Test
    fun `mapStatusToPaymentEvent - failed with unknown failureCode falls back to PaymentFailed`() {
        val result = mapStatusToPaymentEvent(
            status = PaymentStatus.FAILED,
            paymentId = "pay_123",
            failureCode = "some_future_code"
        )
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
    fun `mapErrorCodeToPaymentError - INVALID_PARAMS returns InvalidPaymentRequest`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.INVALID_PARAMS, "Invalid")
        assertTrue(result is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest)
        assertEquals("Invalid", (result as Pos.PaymentEvent.PaymentError.InvalidPaymentRequest).message)
    }

    @Test
    fun `mapErrorCodeToPaymentError - PARAMS_VALIDATION returns InvalidPaymentRequest`() {
        val result = mapErrorCodeToPaymentError(ErrorCodes.PARAMS_VALIDATION, "Validation failed")
        assertTrue(result is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest)
        assertEquals("Validation failed", (result as Pos.PaymentEvent.PaymentError.InvalidPaymentRequest).message)
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
    fun `mapCreatePaymentError - INVALID_PARAMS returns InvalidPaymentRequest`() {
        val result = mapCreatePaymentError(ErrorCodes.INVALID_PARAMS, "Invalid amount")
        assertTrue(result is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest)
        assertEquals("Invalid amount", (result as Pos.PaymentEvent.PaymentError.InvalidPaymentRequest).message)
    }

    @Test
    fun `mapCreatePaymentError - PARAMS_VALIDATION returns InvalidPaymentRequest`() {
        val result = mapCreatePaymentError(ErrorCodes.PARAMS_VALIDATION, "Validation failed")
        assertTrue(result is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest)
        assertEquals("Validation failed", (result as Pos.PaymentEvent.PaymentError.InvalidPaymentRequest).message)
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

    @Test(expected = IllegalStateException::class)
    fun `Amount format - throws exception for invalid value`() {
        val amount = Pos.Amount("iso4217/USD", "invalid")
        amount.format()
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
        assertEquals("payment_not_found", ErrorCodes.PAYMENT_NOT_FOUND)
        assertEquals("payment_expired", ErrorCodes.PAYMENT_EXPIRED)
        assertEquals("invalid_params", ErrorCodes.INVALID_PARAMS)
        assertEquals("params_validation", ErrorCodes.PARAMS_VALIDATION)
    }

    @Test
    fun `FailureCodes constants have correct values`() {
        assertEquals("declined_user", FailureCodes.DECLINED_USER)
    }

    @Test
    fun `mapRefundErrorCode - not_found returns PaymentNotFound`() {
        val result = mapRefundErrorCode(ErrorCodes.NOT_FOUND, "Not Found")
        assertTrue(result is Pos.RefundError.PaymentNotFound)
        assertEquals("Not Found", result.message)
    }

    @Test
    fun `mapRefundErrorCode - payment_not_found returns PaymentNotFound`() {
        val result = mapRefundErrorCode(ErrorCodes.PAYMENT_NOT_FOUND, "Payment not found")
        assertTrue(result is Pos.RefundError.PaymentNotFound)
    }

    @Test
    fun `mapRefundErrorCode - payment_not_succeeded returns PaymentNotSucceeded`() {
        val result = mapRefundErrorCode(ErrorCodes.PAYMENT_NOT_SUCCEEDED, "Not refundable")
        assertTrue(result is Pos.RefundError.PaymentNotSucceeded)
    }

    @Test
    fun `mapRefundErrorCode - params_validation returns InvalidParams`() {
        val result = mapRefundErrorCode(ErrorCodes.PARAMS_VALIDATION, "Validation failed")
        assertTrue(result is Pos.RefundError.InvalidParams)
    }

    @Test
    fun `mapRefundErrorCode - invalid_params returns InvalidParams`() {
        val result = mapRefundErrorCode(ErrorCodes.INVALID_PARAMS, "Bad input")
        assertTrue(result is Pos.RefundError.InvalidParams)
    }

    @Test
    fun `mapRefundErrorCode - auth codes return Unauthorized`() {
        assertTrue(mapRefundErrorCode(ErrorCodes.MISSING_API_KEY, "x") is Pos.RefundError.Unauthorized)
        assertTrue(mapRefundErrorCode(ErrorCodes.INVALID_API_KEY, "x") is Pos.RefundError.Unauthorized)
        assertTrue(mapRefundErrorCode(ErrorCodes.MISSING_MERCHANT_ID, "x") is Pos.RefundError.Unauthorized)
    }

    @Test
    fun `mapRefundErrorCode - NETWORK_ERROR returns Network`() {
        val result = mapRefundErrorCode(ErrorCodes.NETWORK_ERROR, "Timed out")
        assertTrue(result is Pos.RefundError.Network)
        assertEquals("Timed out", result.message)
    }

    @Test
    fun `mapRefundErrorCode - unknown code returns Unknown with original code`() {
        val result = mapRefundErrorCode("bad_gateway", "Bad Gateway")
        assertTrue(result is Pos.RefundError.Unknown)
        val unknown = result as Pos.RefundError.Unknown
        assertEquals("bad_gateway", unknown.code)
        assertEquals("Bad Gateway", unknown.message)
    }

    @Test
    fun `toTransaction - refund field maps to isRefunded and refundedAt`() {
        val record = paymentRecordFixture(
            refund = PaymentRefundDto(status = "fully_refunded", fullyRefundedAt = "2026-05-20T10:11:12Z")
        )
        val tx = record.toTransaction()
        assertTrue(tx.isRefunded)
        assertEquals("2026-05-20T10:11:12Z", tx.refundedAt)
    }

    @Test
    fun `toTransaction - missing refund field leaves isRefunded false`() {
        val tx = paymentRecordFixture(refund = null).toTransaction()
        assertEquals(false, tx.isRefunded)
        assertEquals(null, tx.refundedAt)
    }

    @Test
    fun `toTransaction - refund presence wins even without timestamp`() {
        // Spec marks `fullyRefundedAt` optional. Presence of the refund object alone
        // is enough to consider the payment refunded.
        val tx = paymentRecordFixture(
            refund = PaymentRefundDto(status = "fully_refunded", fullyRefundedAt = null)
        ).toTransaction()
        assertTrue(tx.isRefunded)
        assertEquals(null, tx.refundedAt)
    }

    private fun paymentRecordFixture(refund: PaymentRefundDto?): PaymentRecord =
        PaymentRecord(
            paymentId = "pay_ABC123",
            merchantId = "acme-store-1",
            referenceId = "ORDER-123",
            status = PaymentStatus.SUCCEEDED,
            isTerminal = true,
            fiatAmount = null,
            tokenAmount = null,
            buyer = null,
            transaction = null,
            settlement = null,
            refund = refund,
            createdAt = "2026-05-20T09:00:00Z",
            lastUpdatedAt = "2026-05-20T09:01:00Z",
            settledAt = "2026-05-20T09:01:00Z",
        )

    @Test
    fun `Refund ErrorCodes constants have correct values`() {
        assertEquals("not_found", ErrorCodes.NOT_FOUND)
        assertEquals("already_refunded", ErrorCodes.ALREADY_REFUNDED)
        assertEquals("payment_not_succeeded", ErrorCodes.PAYMENT_NOT_SUCCEEDED)
        assertEquals("missing_api_key", ErrorCodes.MISSING_API_KEY)
        assertEquals("invalid_api_key", ErrorCodes.INVALID_API_KEY)
        assertEquals("missing_merchant_id", ErrorCodes.MISSING_MERCHANT_ID)
    }
}
