package com.walletconnect.pos.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.walletconnect.pos.BuildConfig
import com.walletconnect.pos.Pos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

internal class ApiClient(
    private val apiKey: String,
    private val deviceId: String,
    baseUrl: String = BuildConfig.CORE_API_BASE_URL
) {
    private val coreUrl = "$baseUrl/v1/gateway"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val createPaymentRequestAdapter = moshi.adapter(CreatePaymentRequest::class.java)
    private val createPaymentResponseAdapter = moshi.adapter(CreatePaymentResponse::class.java)
    private val getPaymentRequestAdapter = moshi.adapter(GetPaymentRequest::class.java)
    private val getPaymentResponseAdapter = moshi.adapter(GetPaymentResponse::class.java)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Creates a payment and starts polling for status updates.
     * All events are emitted through the callback.
     */
    suspend fun createPayment(
        referenceId: String,
        unit: String,
        value: String,
        onEvent: (Pos.PaymentEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = CreatePaymentRequest(
            params = CreatePaymentParams(
                referenceId = referenceId,
                amount = Amount(unit = unit, value = value)
            )
        )
        val jsonBody = createPaymentRequestAdapter.toJson(request)
        val httpResponse = executeHttpRequest(jsonBody)

        when (httpResponse) {
            is HttpResponse.Success -> {
                val response = try {
                    createPaymentResponseAdapter.fromJson(httpResponse.body)
                } catch (e: Exception) {
                    onEvent(Pos.PaymentEvent.PaymentError.Undefined("Failed to parse response: ${e.message}"))
                    return@withContext
                }

                if (response == null) {
                    onEvent(Pos.PaymentEvent.PaymentError.Undefined("Null response"))
                    return@withContext
                }

                when (response.status) {
                    "success" -> {
                        val data = response.data
                        if (data == null) {
                            onEvent(Pos.PaymentEvent.PaymentError.Undefined("Missing data in success response"))
                            return@withContext
                        }

                        val uri = URI(buildPaymentUri(data.paymentId))
                        onEvent(
                            Pos.PaymentEvent.PaymentCreated(
                                uri = uri,
                                amount = Pos.Amount(data.amount.unit, data.amount.value),
                                paymentId = data.paymentId
                            )
                        )

                        startPolling(data.paymentId, data.pollInMs, onEvent)
                    }

                    "error" -> {
                        onEvent(
                            mapCreatePaymentError(
                                response.error?.code ?: "UNKNOWN_ERROR",
                                response.error?.message ?: "Unknown error"
                            )
                        )
                    }

                    else -> {
                        onEvent(Pos.PaymentEvent.PaymentError.Undefined("Unknown status: ${response.status}"))
                    }
                }
            }

            is HttpResponse.Error -> {
                onEvent(mapCreatePaymentError(httpResponse.code, httpResponse.message))
            }
        }
    }

    /**
     * Gets the current status of a payment (one-off, no polling).
     */
    suspend fun getPayment(paymentId: String): ApiResult<GetPaymentData> = withContext(Dispatchers.IO) {
        val request = GetPaymentRequest(params = GetPaymentParams(paymentId = paymentId))
        val jsonBody = getPaymentRequestAdapter.toJson(request)
        val httpResponse = executeHttpRequest(jsonBody)

        when (httpResponse) {
            is HttpResponse.Success -> {
                val response = try {
                    getPaymentResponseAdapter.fromJson(httpResponse.body)
                } catch (e: Exception) {
                    return@withContext ApiResult.Error("PARSE_ERROR", "Failed to parse response: ${e.message}")
                }

                if (response == null) {
                    return@withContext ApiResult.Error("PARSE_ERROR", "Null response")
                }

                when (response.status) {
                    "success" -> {
                        val data = response.data
                            ?: return@withContext ApiResult.Error("PARSE_ERROR", "Missing data in success response")
                        ApiResult.Success(data)
                    }

                    "error" -> {
                        ApiResult.Error(
                            code = response.error?.code ?: "UNKNOWN_ERROR",
                            message = response.error?.message ?: "Unknown error"
                        )
                    }

                    else -> ApiResult.Error("UNKNOWN_STATUS", "Unknown status: ${response.status}")
                }
            }

            is HttpResponse.Error -> ApiResult.Error(httpResponse.code, httpResponse.message)
        }
    }

    private suspend fun startPolling(
        paymentId: String,
        initialPollMs: Long,
        onEvent: (Pos.PaymentEvent) -> Unit
    ) {
        var pollDelayMs = initialPollMs
        var lastEmittedStatus: String? = null

        while (true) {
            delay(pollDelayMs)

            when (val result = getPayment(paymentId)) {
                is ApiResult.Success -> {
                    val data = result.data
                    pollDelayMs = data.pollInMs

                    if (data.status != lastEmittedStatus) {
                        lastEmittedStatus = data.status
                        onEvent(mapStatusToPaymentEvent(data.status, data.paymentId))
                    }

                    if (isTerminalStatus(data.status)) {
                        break
                    }
                }

                is ApiResult.Error -> {
                    onEvent(mapErrorCodeToPaymentError(result.code, result.message))

                    if (isTerminalError(result.code) || result.code == "NETWORK_ERROR") {
                        break
                    }
                }
            }
        }
    }

    private fun executeHttpRequest(jsonBody: String): HttpResponse {
        val httpRequest = Request.Builder()
            .url(coreUrl)
            .addHeader("X-Api-Key", apiKey)
            .addHeader("X-Device-Id", deviceId)
            .addHeader("X-Sdk-Version", "pos-kotlin-${BuildConfig.SDK_VERSION}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return try {
            httpClient.newCall(httpRequest).execute().use { response ->
                HttpResponse.Success(response.body.string())
            }
        } catch (e: IOException) {
            HttpResponse.Error("NETWORK_ERROR", e.message ?: "Network error")
        } catch (e: Exception) {
            HttpResponse.Error("UNKNOWN_ERROR", e.message ?: "Unknown error")
        }
    }

    private sealed class HttpResponse {
        data class Success(val body: String) : HttpResponse()
        data class Error(val code: String, val message: String) : HttpResponse()
    }
}
