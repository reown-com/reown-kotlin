package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface PayApi {

    @POST("v1/payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @GET("v1/payments/{id}/status")
    suspend fun getPaymentStatus(
        @Path("id") paymentId: String,
        @Query("maxPollMs") maxPollMs: Long? = null
    ): Response<GetPaymentStatusResponse>

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

/**
 * Merchant data API endpoints, hosted at `api.merchant.pay.walletconnect.com`.
 * Distinct from [PayApi] which targets `api.pay.walletconnect.com`.
 */
internal interface MerchantApi {

    @GET("v1/payments")
    suspend fun searchPayments(
        @Query("referenceId") referenceId: String,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
        @Query("status") status: List<String>? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortDir") sortDir: String? = null,
        @Query("startTs") startTs: String? = null,
        @Query("endTs") endTs: String? = null
    ): Response<TransactionHistoryResponse>

    @POST("v1/refunds")
    suspend fun refundPayment(@Body request: RefundRequest): Response<RefundResponse>
}
