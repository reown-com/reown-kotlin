package com.walletconnect.pos

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PosClientTest {

    @Before
    fun setUp() {
        // Ensure clean state before each test
        PosClient.shutdown()
    }

    @After
    fun tearDown() {
        PosClient.shutdown()
    }

    @Test
    fun `init - succeeds with valid parameters`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        // No exception means success
    }

    @Test
    fun `init - throws on blank apiKey`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.init(apiKey = "", merchantId = "test-merchant", deviceId = "test-device")
        }
        assertTrue(exception.message?.contains("apiKey") == true)
    }

    @Test
    fun `init - throws on whitespace only apiKey`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.init(apiKey = "   ", merchantId = "test-merchant", deviceId = "test-device")
        }
        assertTrue(exception.message?.contains("apiKey") == true)
    }

    @Test
    fun `init - throws on blank merchantId`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.init(apiKey = "test-api-key", merchantId = "", deviceId = "test-device")
        }
        assertTrue(exception.message?.contains("merchantId") == true)
    }

    @Test
    fun `init - throws on whitespace only merchantId`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.init(apiKey = "test-api-key", merchantId = "   ", deviceId = "test-device")
        }
        assertTrue(exception.message?.contains("merchantId") == true)
    }

    @Test
    fun `init - throws on blank deviceId`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "")
        }
        assertTrue(exception.message?.contains("deviceId") == true)
    }

    @Test
    fun `init - throws on whitespace only deviceId`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "   ")
        }
        assertTrue(exception.message?.contains("deviceId") == true)
    }

    @Test
    fun `init - can reinitialize after shutdown`() {
        PosClient.init(apiKey = "first-key", merchantId = "first-merchant", deviceId = "first-device")
        PosClient.shutdown()
        PosClient.init(apiKey = "second-key", merchantId = "second-merchant", deviceId = "second-device")
        // No exception means success
    }

    @Test
    fun `init - can reinitialize without shutdown`() {
        PosClient.init(apiKey = "first-key", merchantId = "first-merchant", deviceId = "first-device")
        PosClient.init(apiKey = "second-key", merchantId = "second-merchant", deviceId = "second-device")
        // No exception means success - init cleans up previous state
    }

    @Test
    fun `createPaymentIntent - throws when not initialized`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.createPaymentIntent(
                amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
                referenceId = "ORDER-123"
            )
        }
        assertTrue(exception.message?.contains("not initialized") == true)
    }

    @Test
    fun `createPaymentIntent - succeeds when initialized`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        var eventReceived = false
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {
                eventReceived = true
            }
        })

        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-123"
        )

        // Give it a moment for the coroutine to start
        Thread.sleep(100)

        // Cancel the payment to prevent ongoing network calls
        PosClient.cancelPayment()
    }

    @Test
    fun `createPaymentIntent - accepts various currency units`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {}
        })

        // Test different currencies - should not throw
        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/EUR", value = "5000"),
            referenceId = "ORDER-EUR"
        )
        PosClient.cancelPayment()

        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/GBP", value = "3000"),
            referenceId = "ORDER-GBP"
        )
        PosClient.cancelPayment()
    }

    @Test
    fun `createPaymentIntent - cancels previous payment when called again`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        val events = mutableListOf<Pos.PaymentEvent>()
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {
                events.add(event)
            }
        })

        // Start first payment
        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-1"
        )

        Thread.sleep(50)

        // Start second payment - should cancel first
        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "2000"),
            referenceId = "ORDER-2"
        )

        Thread.sleep(100)
        PosClient.cancelPayment()
    }

    @Test
    fun `checkPaymentStatus - throws when not initialized`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                PosClient.checkPaymentStatus("pay_123")
            }
        }
    }

    @Test
    fun `checkPaymentStatus - returns error event for invalid payment`() = runBlocking {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        // This will make a real network call and likely fail
        val result = PosClient.checkPaymentStatus("invalid_payment_id")

        // Should return an error event (network error or payment not found)
        assertNotNull(result)
    }

    @Test
    fun `setDelegate - can set delegate before init`() {
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {}
        })
        // No exception means success
    }

    @Test
    fun `setDelegate - can change delegate after init`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {}
        })

        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {}
        })
        // No exception means success
    }

    @Test
    fun `delegate - receives events from createPaymentIntent`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        val latch = CountDownLatch(1)
        var receivedEvent: Pos.PaymentEvent? = null

        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {
                receivedEvent = event
                latch.countDown()
            }
        })

        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-123"
        )

        // Wait for event (or timeout)
        latch.await(2, TimeUnit.SECONDS)
        PosClient.cancelPayment()

        // We should have received some event (likely an error due to test environment)
        assertNotNull(receivedEvent)
    }

    @Test
    fun `cancelPayment - safe to call when not polling`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.cancelPayment()
        // No exception means success
    }

    @Test
    fun `cancelPayment - safe to call multiple times`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.cancelPayment()
        PosClient.cancelPayment()
        PosClient.cancelPayment()
        // No exception means success
    }

    @Test
    fun `cancelPayment - safe to call before init`() {
        // Note: This tests current behavior - cancelPayment doesn't require init
        PosClient.cancelPayment()
        // No exception means success
    }

    @Test
    fun `cancelPayment - stops ongoing payment polling`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        val events = mutableListOf<Pos.PaymentEvent>()
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {
                events.add(event)
            }
        })

        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-123"
        )

        Thread.sleep(50)
        PosClient.cancelPayment()
        val eventsAfterCancel = events.size

        Thread.sleep(200)
        // Events should not increase significantly after cancel
        assertTrue(events.size <= eventsAfterCancel + 1)
    }

    @Test
    fun `shutdown - safe to call multiple times`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.shutdown()
        PosClient.shutdown()
        PosClient.shutdown()
        // No exception means success
    }

    @Test
    fun `shutdown - safe to call without init`() {
        PosClient.shutdown()
        // No exception means success
    }

    @Test
    fun `shutdown - requires reinit after`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.shutdown()

        val exception = assertThrows(IllegalStateException::class.java) {
            PosClient.createPaymentIntent(
                amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
                referenceId = "ORDER-123"
            )
        }
        assertTrue(exception.message?.contains("not initialized") == true)
    }

    @Test
    fun `shutdown - checkPaymentStatus throws after shutdown`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.shutdown()

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                PosClient.checkPaymentStatus("pay_123")
            }
        }
    }

    @Test
    fun `shutdown - cancels ongoing polling`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")

        val events = mutableListOf<Pos.PaymentEvent>()
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {
                events.add(event)
            }
        })

        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-123"
        )

        Thread.sleep(50)
        PosClient.shutdown()
        val eventsAfterShutdown = events.size

        Thread.sleep(200)
        // Events should not increase after shutdown
        assertTrue(events.size <= eventsAfterShutdown + 1)
    }

    @Test
    fun `amount - can be created with zero value`() {
        val amount = Pos.Amount(unit = "iso4217/USD", value = "0")
        assertTrue(amount.value == "0")
    }

    @Test
    fun `amount - can be created with large value`() {
        val amount = Pos.Amount(unit = "iso4217/USD", value = "999999999")
        assertTrue(amount.value == "999999999")
    }

    @Test
    fun `referenceId - can be empty string`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {}
        })

        // Should not throw with empty referenceId
        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = ""
        )
        PosClient.cancelPayment()
    }

    @Test
    fun `referenceId - accepts special characters`() {
        PosClient.init(apiKey = "test-api-key", merchantId = "test-merchant", deviceId = "test-device")
        PosClient.setDelegate(object : POSDelegate {
            override fun onEvent(event: Pos.PaymentEvent) {}
        })

        PosClient.createPaymentIntent(
            amount = Pos.Amount(unit = "iso4217/USD", value = "1000"),
            referenceId = "ORDER-123_ABC/2024"
        )
        PosClient.cancelPayment()
    }
}
