package com.reown.sign

import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import com.reown.sign.engine.domain.wallet_service.WalletServiceRequester
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
class WalletServiceRequesterTest {
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var walletServiceRequester: WalletServiceRequester

    private val testUri = "https://test.wallet.service/endpoint"
    private val testSessionRequest = SignRpc.SessionRequest(
        id = 123456L,
        params = SignParams.SessionRequestParams(
            chainId = "1",
            request = SessionRequestVO(
                params = """{"account":"0x9CAaB7E1D1ad6eaB4d6a7f479Cb8800da551cbc0","chainFilter":["0xa"]}""",
                method = "wallet_getAssets"
            )
        )
    )

    @Before
    fun setup() {
        mockCall = mockk(relaxed = true)
        mockOkHttpClient = mockk {
            every { newCall(any()) } returns mockCall
        }
        walletServiceRequester = WalletServiceRequester(mockOkHttpClient)
    }

    @Test
    fun `should send correct request and return response on success`() = runBlocking {
        // Arrange
        val expectedResponse = """{"jsonrpc":"2.0","id":"123456","result":{"assets":[]}}"""
        val mockResponseBody = expectedResponse.toResponseBody("application/json".toMediaType())
        val mockResponse = mockk<Response> {
            every { isSuccessful } returns true
            every { body } returns mockResponseBody
        }

        every { mockCall.execute() } returns mockResponse

        // Capture the actual request to verify it later
        val requestSlot = slot<Request>()
        every { mockOkHttpClient.newCall(capture(requestSlot)) } returns mockCall

        // Act
        val result = walletServiceRequester.request(testSessionRequest, testUri)

        // Assert
        assertEquals(expectedResponse, result)

        // Verify request details
        with(requestSlot.captured) {
            assertEquals(testUri, url.toString())
            assertEquals("POST", method)
            assertEquals("application/json", header("Content-Type"))
            assertEquals("application/json", header("Accept"))
        }
    }

    @Test
    fun `should throw exception when response is not successful`() = runBlocking {
        // Arrange
        val errorJson = """{"jsonrpc":"2.0","id":"123456","error":{"code":-32000,"message":"Invalid request"}}"""
        val mockResponseBody = errorJson.toResponseBody("application/json".toMediaType())
        val mockResponse = mockk<Response> {
            every { isSuccessful } returns false
            every { body } returns mockResponseBody
        }

        every { mockCall.execute() } returns mockResponse

        // Act & Assert
        val exception = assertFailsWith<IllegalStateException> {
            walletServiceRequester.request(testSessionRequest, testUri)
        }

        // Verify error message contains the error from response
        assert(exception.message?.contains("Invalid request") == true)
    }

    @Test
    fun `should throw exception when response is not successful and has no error detail`() = runBlocking {
        // Arrange
        val mockResponse = mockk<Response> {
            every { isSuccessful } returns false
            every { body } returns "".toResponseBody("application/json".toMediaType())
        }

        every { mockCall.execute() } returns mockResponse

        // Act & Assert
        val exception = assertFailsWith<JSONException> {
            walletServiceRequester.request(testSessionRequest, testUri)
        }

        // Verify generic error message
        assert(exception.message != null)
    }

    @Test
    fun `should handle null response body`() = runBlocking {
        // Arrange
        val mockResponse = mockk<Response> {
            every { isSuccessful } returns true
            every { body } returns null
        }

        every { mockCall.execute() } returns mockResponse

        // Act
        val result = walletServiceRequester.request(testSessionRequest, testUri)

        // Assert
        assertEquals("", result)
    }
}