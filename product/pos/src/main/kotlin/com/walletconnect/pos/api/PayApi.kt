package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface PayApi {

    @POST("payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @GET("payments/{paymentId}/status")
    suspend fun getPaymentStatus(@Path("paymentId") paymentId: String): Response<GetPaymentStatusResponse>
}
