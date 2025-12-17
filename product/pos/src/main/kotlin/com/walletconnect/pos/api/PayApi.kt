package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface PayApi {

    @POST("v1/merchant/payment")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @GET("v1/merchant/payment/{id}/status")
    suspend fun getPaymentStatus(@Path("id") paymentId: String): Response<GetPaymentStatusResponse>
}
