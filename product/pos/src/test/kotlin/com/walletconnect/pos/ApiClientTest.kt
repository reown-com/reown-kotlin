package com.walletconnect.pos

import com.walletconnect.pos.api.ApiClient
import com.walletconnect.pos.api.ApiResult
import com.walletconnect.pos.api.GetPaymentData
import com.walletconnect.pos.api.PaymentStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApiClientTest {

    private fun createMockResponse(body: String, code: Int = 200): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    // ==================== getPaymentStatus Tests ====================

    @Test
    fun `getPaymentStatus - returns success with valid response`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "success",
                "data": {
                    "paymentId": "pay_123",
                    "status": "requires_action",
                    "pollInMs": 1000
                }
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("pay_123", data.paymentId)
        assertEquals(PaymentStatus.REQUIRES_ACTION, data.status)
        assertEquals(1000L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns success with succeeded status and pollInMs zero`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "success",
                "data": {
                    "paymentId": "pay_123",
                    "status": "succeeded",
                    "pollInMs": 0
                }
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.SUCCEEDED, data.status)
        assertEquals(0L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns success with processing status`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "success",
                "data": {
                    "paymentId": "pay_456",
                    "status": "processing",
                    "pollInMs": 1000
                }
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_456")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.PROCESSING, data.status)
    }

    @Test
    fun `getPaymentStatus - returns success with expired status and pollInMs zero`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "success",
                "data": {
                    "paymentId": "pay_789",
                    "status": "expired",
                    "pollInMs": 0
                }
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_789")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.EXPIRED, data.status)
        assertEquals(0L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns success with failed status and pollInMs zero`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "success",
                "data": {
                    "paymentId": "pay_failed",
                    "status": "failed",
                    "pollInMs": 0
                }
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_failed")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.FAILED, data.status)
        assertEquals(0L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns error when payment not found`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "error",
                "error": {
                    "code": "PAYMENT_NOT_FOUND",
                    "message": "Payment not found"
                }
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("invalid_id")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("PAYMENT_NOT_FOUND", error.code)
        assertEquals("Payment not found", error.message)
    }

    @Test
    fun `getPaymentStatus - returns error on network failure`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } throws IOException("Connection refused")

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("NETWORK_ERROR", error.code)
    }

    @Test
    fun `getPaymentStatus - returns parse error on invalid JSON`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse("invalid json {{{")

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("PARSE_ERROR", error.code)
    }

    @Test
    fun `getPaymentStatus - returns error on unknown status`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "unknown_status"
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("UNKNOWN_STATUS", error.code)
    }

    @Test
    fun `getPaymentStatus - returns error when success response has null data`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "success",
                "data": null
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("PARSE_ERROR", error.code)
        assertTrue(error.message.contains("Missing data"))
    }

    @Test
    fun `getPaymentStatus - returns error with default values when error object is missing`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val responseJson = """
            {
                "status": "error"
            }
        """.trimIndent()

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } returns createMockResponse(responseJson)

        val apiClient = createApiClientWithMockHttp(mockClient)
        val result = apiClient.getPaymentStatus("pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("UNKNOWN_ERROR", error.code)
        assertEquals("Unknown error", error.message)
    }

    // ==================== Polling Logic Tests ====================

    @Test
    fun `polling - stops when pollInMs is zero (final state)`() = runTest {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        var callCount = 0
        val responses = listOf(
            """{"status":"success","data":{"paymentId":"pay_123","status":"requires_action","pollInMs":100}}""",
            """{"status":"success","data":{"paymentId":"pay_123","status":"processing","pollInMs":100}}""",
            """{"status":"success","data":{"paymentId":"pay_123","status":"succeeded","pollInMs":0}}"""
        )

        coEvery { mockClient.newCall(any()) } returns mockCall
        coEvery { mockCall.execute() } answers {
            createMockResponse(responses.getOrElse(callCount++) { responses.last() })
        }

        val apiClient = createApiClientWithMockHttp(mockClient)
        val events = mutableListOf<Pos.PaymentEvent>()

        // Simulate a simplified polling scenario
        var pollInMs = 100L
        while (pollInMs > 0) {
            val result = apiClient.getPaymentStatus("pay_123")
            if (result is ApiResult.Success) {
                pollInMs = result.data.pollInMs
            } else {
                break
            }
        }

        // Verify that we stopped at pollInMs == 0
        assertEquals(0L, pollInMs)
    }

    // ==================== Helper Methods ====================

    private fun createApiClientWithMockHttp(mockClient: OkHttpClient): ApiClient {
        // Using reflection to inject mock client - in real scenario, use dependency injection
        val apiClient = ApiClient(
            apiKey = "test-api-key",
            deviceId = "test-device-id",
            baseUrl = "https://test.walletconnect.com"
        )

        // Use reflection to replace the httpClient
        val httpClientField = ApiClient::class.java.getDeclaredField("httpClient")
        httpClientField.isAccessible = true
        httpClientField.set(apiClient, mockClient)

        return apiClient
    }
}
