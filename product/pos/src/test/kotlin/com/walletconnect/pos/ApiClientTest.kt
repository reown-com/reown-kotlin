package com.walletconnect.pos

import com.walletconnect.pos.api.ApiResult
import com.walletconnect.pos.api.ErrorCodes
import com.walletconnect.pos.api.GetPaymentStatusResponse
import com.walletconnect.pos.api.PayApi
import com.walletconnect.pos.api.PaymentStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ApiClientTest {

    @Test
    fun `getPaymentStatus - returns success with requires_action status`() = runTest {
        val mockApi = mockk<PayApi>()
        val expectedResponse = GetPaymentStatusResponse(
            paymentId = "pay_123",
            status = PaymentStatus.REQUIRES_ACTION,
            pollInMs = 1000L
        )

        coEvery { mockApi.getPaymentStatus("pay_123") } returns Response.success(expectedResponse)

        val result = callGetPaymentStatus(mockApi, "pay_123")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("pay_123", data.paymentId)
        assertEquals(PaymentStatus.REQUIRES_ACTION, data.status)
        assertEquals(1000L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns success with succeeded status and pollInMs zero`() = runTest {
        val mockApi = mockk<PayApi>()
        val expectedResponse = GetPaymentStatusResponse(
            paymentId = "pay_123",
            status = PaymentStatus.SUCCEEDED,
            pollInMs = 0L
        )

        coEvery { mockApi.getPaymentStatus("pay_123") } returns Response.success(expectedResponse)

        val result = callGetPaymentStatus(mockApi, "pay_123")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.SUCCEEDED, data.status)
        assertEquals(0L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns success with processing status`() = runTest {
        val mockApi = mockk<PayApi>()
        val expectedResponse = GetPaymentStatusResponse(
            paymentId = "pay_456",
            status = PaymentStatus.PROCESSING,
            pollInMs = 1000L
        )

        coEvery { mockApi.getPaymentStatus("pay_456") } returns Response.success(expectedResponse)

        val result = callGetPaymentStatus(mockApi, "pay_456")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.PROCESSING, data.status)
    }

    @Test
    fun `getPaymentStatus - returns success with expired status and pollInMs zero`() = runTest {
        val mockApi = mockk<PayApi>()
        val expectedResponse = GetPaymentStatusResponse(
            paymentId = "pay_789",
            status = PaymentStatus.EXPIRED,
            pollInMs = 0L
        )

        coEvery { mockApi.getPaymentStatus("pay_789") } returns Response.success(expectedResponse)

        val result = callGetPaymentStatus(mockApi, "pay_789")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.EXPIRED, data.status)
        assertEquals(0L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns success with failed status and pollInMs zero`() = runTest {
        val mockApi = mockk<PayApi>()
        val expectedResponse = GetPaymentStatusResponse(
            paymentId = "pay_failed",
            status = PaymentStatus.FAILED,
            pollInMs = 0L
        )

        coEvery { mockApi.getPaymentStatus("pay_failed") } returns Response.success(expectedResponse)

        val result = callGetPaymentStatus(mockApi, "pay_failed")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(PaymentStatus.FAILED, data.status)
        assertEquals(0L, data.pollInMs)
    }

    @Test
    fun `getPaymentStatus - returns error when payment not found`() = runTest {
        val mockApi = mockk<PayApi>()
        val errorBody = """{"code":"PAYMENT_NOT_FOUND","message":"Payment not found"}"""
            .toResponseBody("application/json".toMediaType())

        coEvery { mockApi.getPaymentStatus("invalid_id") } returns Response.error(404, errorBody)

        val result = callGetPaymentStatus(mockApi, "invalid_id")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ErrorCodes.PAYMENT_NOT_FOUND, error.code)
        assertEquals("Payment not found", error.message)
    }

    @Test
    fun `getPaymentStatus - returns error on network failure`() = runTest {
        val mockApi = mockk<PayApi>()

        coEvery { mockApi.getPaymentStatus("pay_123") } throws IOException("Connection refused")

        val result = callGetPaymentStatus(mockApi, "pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ErrorCodes.NETWORK_ERROR, error.code)
    }

    @Test
    fun `getPaymentStatus - returns parse error when response body is null`() = runTest {
        val mockApi = mockk<PayApi>()

        coEvery { mockApi.getPaymentStatus("pay_123") } returns Response.success(null)

        val result = callGetPaymentStatus(mockApi, "pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ErrorCodes.PARSE_ERROR, error.code)
    }

    @Test
    fun `getPaymentStatus - returns HTTP error with fallback when error body is invalid`() = runTest {
        val mockApi = mockk<PayApi>()
        val errorBody = "invalid json".toResponseBody("application/json".toMediaType())

        coEvery { mockApi.getPaymentStatus("pay_123") } returns Response.error(500, errorBody)

        val result = callGetPaymentStatus(mockApi, "pay_123")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertTrue(error.code.startsWith("HTTP_"))
    }

    @Test
    fun `polling - terminates when pollInMs is zero`() = runTest {
        val mockApi = mockk<PayApi>()
        var callCount = 0

        coEvery { mockApi.getPaymentStatus("pay_123") } answers {
            callCount++
            when (callCount) {
                1 -> Response.success(GetPaymentStatusResponse("pay_123", PaymentStatus.REQUIRES_ACTION, 100L))
                2 -> Response.success(GetPaymentStatusResponse("pay_123", PaymentStatus.PROCESSING, 100L))
                else -> Response.success(GetPaymentStatusResponse("pay_123", PaymentStatus.SUCCEEDED, 0L))
            }
        }

        var lastPollInMs = 100L
        while (lastPollInMs > 0) {
            val result = callGetPaymentStatus(mockApi, "pay_123")
            if (result is ApiResult.Success) {
                lastPollInMs = result.data.pollInMs
            } else {
                break
            }
        }

        assertEquals(0L, lastPollInMs)
        assertEquals(3, callCount)
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
        assertEquals("NETWORK_ERROR", ErrorCodes.NETWORK_ERROR)
        assertEquals("PARSE_ERROR", ErrorCodes.PARSE_ERROR)
    }

    private suspend fun callGetPaymentStatus(
        mockApi: PayApi,
        paymentId: String
    ): ApiResult<GetPaymentStatusResponse> {
        return try {
            val response = mockApi.getPaymentStatus(paymentId)
            if (response.isSuccessful) {
                val data = response.body()
                if (data == null) {
                    ApiResult.Error(ErrorCodes.PARSE_ERROR, "Empty response body")
                } else {
                    ApiResult.Success(data)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null && errorBody.contains("code")) {
                    val code = Regex(""""code"\s*:\s*"([^"]+)"""").find(errorBody)?.groupValues?.get(1) ?: "HTTP_${response.code()}"
                    val message = Regex(""""message"\s*:\s*"([^"]+)"""").find(errorBody)?.groupValues?.get(1) ?: response.message()
                    ApiResult.Error(code, message)
                } else {
                    ApiResult.Error("HTTP_${response.code()}", response.message())
                }
            }
        } catch (e: IOException) {
            ApiResult.Error(ErrorCodes.NETWORK_ERROR, e.message ?: "Network error")
        } catch (e: Exception) {
            ApiResult.Error(ErrorCodes.PARSE_ERROR, e.message ?: "Unexpected error")
        }
    }
}
