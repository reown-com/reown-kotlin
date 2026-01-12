package com.walletconnect.pay.test.utils

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface PosTestApi {
    @POST("v1/merchant/payment")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @GET("v1/merchant/payment/{id}/status")
    suspend fun getPaymentStatus(@Path("id") paymentId: String): Response<GetPaymentStatusResponse>
}

data class CreatePaymentRequest(
    @Json(name = "referenceId") val referenceId: String,
    @Json(name = "amount") val amount: AmountRequest
)

data class AmountRequest(
    @Json(name = "unit") val unit: String,
    @Json(name = "value") val value: String
)

data class CreatePaymentResponse(
    @Json(name = "paymentId") val paymentId: String,
    @Json(name = "status") val status: String,
    @Json(name = "expiresAt") val expiresAt: Long,
    @Json(name = "gatewayUrl") val gatewayUrl: String,
    @Json(name = "pollInMs") val pollInMs: Long? = null,
    @Json(name = "isFinal") val isFinal: Boolean = false
)

data class GetPaymentStatusResponse(
    @Json(name = "status") val status: String,
    @Json(name = "isFinal") val isFinal: Boolean,
    @Json(name = "pollInMs") val pollInMs: Long? = null
)

fun createPosApi(): PosTestApi {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Api-Key", Common.API_KEY)
                    .addHeader("Merchant-Id", Common.MERCHANT_ID)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Sdk-Name", "kotlin-pay-test")
                    .addHeader("Sdk-Version", "1.0.0")
                    .addHeader("Sdk-Platform", "android")
                    .build()
            )
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    return Retrofit.Builder()
        .baseUrl(Common.PAY_API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PosTestApi::class.java)
}
