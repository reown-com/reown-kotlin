package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface PayApi {

    @POST("v1/merchant/payment")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @GET("v1/merchant/payment/{id}/status")
    suspend fun getPaymentStatus(@Path("id") paymentId: String): Response<GetPaymentStatusResponse>

    @POST("v1/payments/{id}/cancel")
    suspend fun cancelPayment(@Path("id") paymentId: String): Response<Unit>

    @GET("v1/merchants/payments")
    suspend fun getTransactionHistory(
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
        @Query("status") status: List<String>? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortDir") sortDir: String? = null,
        @Query("startTs") startTs: String? = null,
        @Query("endTs") endTs: String? = null
    ): Response<TransactionHistoryResponse>
}
