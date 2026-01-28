package com.walletconnect.pos.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MerchantApi {

    @GET("v1/merchants/{merchant_id}/payments")
    suspend fun getTransactionHistory(
        @Path("merchant_id") merchantId: String,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
        @Query("status") status: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_dir") sortDir: String? = null,
        @Query("start_ts") startTs: String? = null,
        @Query("end_ts") endTs: String? = null
    ): Response<TransactionHistoryResponse>
}
