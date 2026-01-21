package com.walletconnect.pay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayErrorsTest {

    // PayError tests
    @Test
    fun `PayError Http should hold correct message`() {
        val error = Pay.PayError.Http("Network connection failed")

        assertEquals("Network connection failed", error.message)
        assertTrue(error is Pay.PayError)
        assertTrue(error is Exception)
    }

    @Test
    fun `PayError Api should hold correct message`() {
        val error = Pay.PayError.Api("API rate limit exceeded")

        assertEquals("API rate limit exceeded", error.message)
        assertTrue(error is Pay.PayError)
    }

    @Test
    fun `PayError Timeout should be singleton with fixed message`() {
        val error1 = Pay.PayError.Timeout
        val error2 = Pay.PayError.Timeout

        assertTrue(error1 === error2)
        assertEquals("Timeout: polling exceeded maximum duration", error1.message)
    }

    // GetPaymentOptionsError tests
    @Test
    fun `GetPaymentOptionsError InvalidPaymentLink should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.InvalidPaymentLink("Invalid URL format")

        assertEquals("Invalid URL format", error.message)
        assertTrue(error is Pay.GetPaymentOptionsError)
        assertTrue(error is Exception)
    }

    @Test
    fun `GetPaymentOptionsError PaymentExpired should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.PaymentExpired("Payment has expired")

        assertEquals("Payment has expired", error.message)
    }

    @Test
    fun `GetPaymentOptionsError PaymentNotFound should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.PaymentNotFound("Payment ID not found")

        assertEquals("Payment ID not found", error.message)
    }

    @Test
    fun `GetPaymentOptionsError InvalidRequest should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.InvalidRequest("Missing required field")

        assertEquals("Missing required field", error.message)
    }

    @Test
    fun `GetPaymentOptionsError OptionNotFound should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.OptionNotFound("Option does not exist")

        assertEquals("Option does not exist", error.message)
    }

    @Test
    fun `GetPaymentOptionsError PaymentNotReady should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.PaymentNotReady("Payment is still processing")

        assertEquals("Payment is still processing", error.message)
    }

    @Test
    fun `GetPaymentOptionsError InvalidAccount should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.InvalidAccount("Invalid account address")

        assertEquals("Invalid account address", error.message)
    }

    @Test
    fun `GetPaymentOptionsError ComplianceFailed should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.ComplianceFailed("Compliance check failed")

        assertEquals("Compliance check failed", error.message)
    }

    @Test
    fun `GetPaymentOptionsError Http should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.Http("Server error 500")

        assertEquals("Server error 500", error.message)
    }

    @Test
    fun `GetPaymentOptionsError InternalError should hold correct message`() {
        val error = Pay.GetPaymentOptionsError.InternalError("Unexpected internal error")

        assertEquals("Unexpected internal error", error.message)
    }

    // GetPaymentRequestError tests
    @Test
    fun `GetPaymentRequestError OptionNotFound should hold correct message`() {
        val error = Pay.GetPaymentRequestError.OptionNotFound("Option not available")

        assertEquals("Option not available", error.message)
        assertTrue(error is Pay.GetPaymentRequestError)
        assertTrue(error is Exception)
    }

    @Test
    fun `GetPaymentRequestError PaymentNotFound should hold correct message`() {
        val error = Pay.GetPaymentRequestError.PaymentNotFound("Payment not found")

        assertEquals("Payment not found", error.message)
    }

    @Test
    fun `GetPaymentRequestError InvalidAccount should hold correct message`() {
        val error = Pay.GetPaymentRequestError.InvalidAccount("Account mismatch")

        assertEquals("Account mismatch", error.message)
    }

    @Test
    fun `GetPaymentRequestError Http should hold correct message`() {
        val error = Pay.GetPaymentRequestError.Http("Connection timeout")

        assertEquals("Connection timeout", error.message)
    }

    @Test
    fun `GetPaymentRequestError FetchError should hold correct message`() {
        val error = Pay.GetPaymentRequestError.FetchError("Failed to fetch data")

        assertEquals("Failed to fetch data", error.message)
    }

    @Test
    fun `GetPaymentRequestError InternalError should hold correct message`() {
        val error = Pay.GetPaymentRequestError.InternalError("Internal processing error")

        assertEquals("Internal processing error", error.message)
    }

    // ConfirmPaymentError tests
    @Test
    fun `ConfirmPaymentError PaymentNotFound should hold correct message`() {
        val error = Pay.ConfirmPaymentError.PaymentNotFound("Cannot find payment")

        assertEquals("Cannot find payment", error.message)
        assertTrue(error is Pay.ConfirmPaymentError)
        assertTrue(error is Exception)
    }

    @Test
    fun `ConfirmPaymentError PaymentExpired should hold correct message`() {
        val error = Pay.ConfirmPaymentError.PaymentExpired("Payment window expired")

        assertEquals("Payment window expired", error.message)
    }

    @Test
    fun `ConfirmPaymentError InvalidOption should hold correct message`() {
        val error = Pay.ConfirmPaymentError.InvalidOption("Selected option is invalid")

        assertEquals("Selected option is invalid", error.message)
    }

    @Test
    fun `ConfirmPaymentError InvalidSignature should hold correct message`() {
        val error = Pay.ConfirmPaymentError.InvalidSignature("Signature verification failed")

        assertEquals("Signature verification failed", error.message)
    }

    @Test
    fun `ConfirmPaymentError RouteExpired should hold correct message`() {
        val error = Pay.ConfirmPaymentError.RouteExpired("Payment route has expired")

        assertEquals("Payment route has expired", error.message)
    }

    @Test
    fun `ConfirmPaymentError Http should hold correct message`() {
        val error = Pay.ConfirmPaymentError.Http("HTTP 503 Service Unavailable")

        assertEquals("HTTP 503 Service Unavailable", error.message)
    }

    @Test
    fun `ConfirmPaymentError InternalError should hold correct message`() {
        val error = Pay.ConfirmPaymentError.InternalError("Unknown internal error")

        assertEquals("Unknown internal error", error.message)
    }

    @Test
    fun `ConfirmPaymentError UnsupportedMethod should hold correct message`() {
        val error = Pay.ConfirmPaymentError.UnsupportedMethod("Method not supported")

        assertEquals("Method not supported", error.message)
    }

    // Error hierarchy tests
    @Test
    fun `all error types should be throwable`() {
        val errors = listOf(
            Pay.PayError.Http("test"),
            Pay.PayError.Api("test"),
            Pay.PayError.Timeout,
            Pay.GetPaymentOptionsError.InvalidPaymentLink("test"),
            Pay.GetPaymentOptionsError.PaymentExpired("test"),
            Pay.GetPaymentRequestError.OptionNotFound("test"),
            Pay.ConfirmPaymentError.PaymentNotFound("test")
        )

        errors.forEach { error ->
            assertTrue("${error::class.simpleName} should be throwable", error is Throwable)
            assertNotNull("${error::class.simpleName} should have a message", error.message)
        }
    }

    @Test
    fun `error types can be caught as their sealed class parent`() {
        val httpError: Pay.PayError = Pay.PayError.Http("test")
        val apiError: Pay.PayError = Pay.PayError.Api("test")
        val timeoutError: Pay.PayError = Pay.PayError.Timeout

        // All can be assigned to PayError
        val errors: List<Pay.PayError> = listOf(httpError, apiError, timeoutError)
        assertEquals(3, errors.size)
    }
}
