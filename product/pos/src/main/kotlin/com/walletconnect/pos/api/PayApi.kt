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

    @GET("v1/merchants/{merchant_id}/payments")
    suspend fun getTransactionHistory(
        @Path("merchant_id") merchantId: String,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
        @Query("status") status: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_dir") sortDir: String? = null
    ): Response<TransactionHistoryResponse>
}
