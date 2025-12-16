package com.walletconnect.pos.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.walletconnect.pos.BuildConfig
import com.walletconnect.pos.Pos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

internal class ApiClient(
    private val apiKey: String,
    private val merchantId: String,
    baseUrl: String = BuildConfig.CORE_API_BASE_URL
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val errorAdapter = moshi.adapter(ApiErrorResponse::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(createHeadersInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val payApi: PayApi = retrofit.create(PayApi::class.java)

    suspend fun createPayment(
        referenceId: String,
        unit: String,
        value: String,
        onEvent: (Pos.PaymentEvent) -> Unit
    ) {
        val request = CreatePaymentRequest(
            referenceId = referenceId.ifBlank { null },
            amount = Amount(unit = unit, value = value)
        )

        try {
            val response = payApi.createPayment(request)

            if (response.isSuccessful) {
                val data = response.body()
                if (data == null) {
                    onEvent(Pos.PaymentEvent.PaymentError.Undefined("Empty response body"))
                    return
                }

                onEvent(
                    Pos.PaymentEvent.PaymentCreated(
                        uri = URI(data.gatewayUrl.toSandboxUrl()),
                        amount = Pos.Amount(unit, value),
                        paymentId = data.paymentId
                    )
                )

                startPolling(data.paymentId, onEvent)
            } else {
                val error = parseErrorResponse(response)
                onEvent(mapCreatePaymentError(error.code, error.message))
            }
        } catch (e: CancellationException) {
            // Rethrow cancellation to properly propagate coroutine cancellation
            throw e
        } catch (e: IOException) {
            onEvent(Pos.PaymentEvent.PaymentError.CreatePaymentFailed("Network error: ${e.message}"))
        } catch (e: Exception) {
            onEvent(Pos.PaymentEvent.PaymentError.Undefined("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun startPolling(
        paymentId: String,
        onEvent: (Pos.PaymentEvent) -> Unit
    ) {
        var lastEmittedStatus: String? = null

        while (true) {
            when (val result = getPaymentStatus(paymentId)) {
                is ApiResult.Success -> {
                    val data = result.data

                    if (data.status != lastEmittedStatus) {
                        lastEmittedStatus = data.status
                        onEvent(mapStatusToPaymentEvent(data.status, paymentId))
                    }

                    if (data.isFinal || data.pollInMs == null) {
                        break
                    }

                    delay(data.pollInMs)
                }

                is ApiResult.Error -> {
                    onEvent(mapErrorCodeToPaymentError(result.code, result.message))

                    if (isTerminalError(result.code) || result.code == ErrorCodes.NETWORK_ERROR) {
                        break
                    }
                }
            }
        }
    }

    suspend fun getPaymentStatus(paymentId: String): ApiResult<GetPaymentStatusResponse> {
        return try {
            val response = payApi.getPaymentStatus(paymentId)

            if (response.isSuccessful) {
                val data = response.body()
                if (data == null) {
                    ApiResult.Error(ErrorCodes.PARSE_ERROR, "Empty response body")
                } else {
                    ApiResult.Success(data)
                }
            } else {
                val error = parseErrorResponse(response)
                ApiResult.Error(error.code, error.message)
            }
        } catch (e: CancellationException) {
            // Rethrow cancellation to properly propagate coroutine cancellation
            throw e
        } catch (e: IOException) {
            ApiResult.Error(ErrorCodes.NETWORK_ERROR, e.message ?: "Network error")
        } catch (e: Exception) {
            ApiResult.Error(ErrorCodes.PARSE_ERROR, e.message ?: "Unexpected error")
        }
    }

    private fun createHeadersInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Api-Key", apiKey)
                .addHeader("Merchant-Id", merchantId)
                .addHeader("Sdk-Name", "pos-kotlin")
                .addHeader("Sdk-Version", BuildConfig.SDK_VERSION)
                .addHeader("Sdk-Platform", "android")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    private fun <T> parseErrorResponse(response: Response<T>): ApiErrorResponse {
        val errorBody = response.errorBody()?.string()
        return if (errorBody != null) {
            try {
                errorAdapter.fromJson(errorBody) ?: ApiErrorResponse(
                    code = "HTTP_${response.code()}",
                    message = response.message()
                )
            } catch (e: Exception) {
                ApiErrorResponse(
                    code = "HTTP_${response.code()}",
                    message = response.message()
                )
            }
        } else {
            ApiErrorResponse(
                code = "HTTP_${response.code()}",
                message = response.message()
            )
        }
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }

    // TODO: Remove this when the API returns the correct sandbox URL
    private fun String.toSandboxUrl(): String {
        return replace("://pay.walletconnect.com", "://sandbox.pay.walletconnect.com")
    }
}
