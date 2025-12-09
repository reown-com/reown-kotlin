package com.walletconnect.pos

import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class POSClientTest {

    @After
    fun tearDown() {
        PosClient.shutdown()
    }

    @Test
    fun `init - succeeds with valid parameters`() {
        PosClient.init(apiKey = "test-api-key", deviceId = "test-device")
        // No exception means success
    }

    @Test
    fun `init - throws on blank apiKey`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PosClient.init(apiKey = "", deviceId = "test-device")
        }
        assertTrue(exception.message?.contains("apiKey") == true)
    }

    @Test
    fun `init - throws on blank deviceId`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PosClient.init(apiKey = "test-api-key", deviceId = "")
        }
        assertTrue(exception.message?.contains("deviceId") == true)
    }

    @Test
    fun `createPaymentIntent - throws when not initialized`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.createPaymentIntent(
                amount = Pos.Model.Amount(unit = "iso4217/USD", value = "1000"),
                referenceId = "ORDER-123"
            )
        }
        assertTrue(exception.message?.contains("not initialized") == true)
    }

    @Test
    fun `createPaymentIntent - succeeds when initialized`() {
        PosClient.init(apiKey = "test-api-key", deviceId = "test-device")
        
        // This will attempt to make a real HTTP call which will fail,
        // but we're just testing that it doesn't throw IllegalStateException
        var eventReceived = false
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.Model.PaymentEvent) {
                eventReceived = true
            }
        })
        
        PosClient.createPaymentIntent(
            amount = Pos.Model.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-123"
        )
        
        // Give it a moment for the coroutine to start
        Thread.sleep(100)
        
        // Cancel the payment to prevent ongoing network calls
        PosClient.cancelPayment()
    }

    @Test
    fun `checkPaymentStatus - throws when not initialized`() {
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                PosClient.checkPaymentStatus("pay_123")
            }
        }
    }

    @Test
    fun `setDelegate - can set delegate before init`() {
        var eventReceived = false
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.Model.PaymentEvent) {
                eventReceived = true
            }
        })
        // No exception means success
    }

    @Test
    fun `setDelegate - can set delegate to null`() {
        PosClient.setDelegate(null)
        // No exception means success
    }

    @Test
    fun `cancelPayment - safe to call when not polling`() {
        PosClient.init(apiKey = "test-api-key", deviceId = "test-device")
        PosClient.cancelPayment()
        // No exception means success
    }

    @Test
    fun `shutdown - safe to call multiple times`() {
        PosClient.init(apiKey = "test-api-key", deviceId = "test-device")
        PosClient.shutdown()
        PosClient.shutdown()
        // No exception means success
    }

    @Test
    fun `shutdown - requires reinit after`() {
        PosClient.init(apiKey = "test-api-key", deviceId = "test-device")
        PosClient.shutdown()
        
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.createPaymentIntent(
                amount = Pos.Model.Amount(unit = "iso4217/USD", value = "1000"),
                referenceId = "ORDER-123"
            )
        }
        assertTrue(exception.message?.contains("not initialized") == true)
    }
}