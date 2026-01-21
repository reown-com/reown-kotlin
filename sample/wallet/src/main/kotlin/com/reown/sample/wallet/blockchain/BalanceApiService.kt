package com.reown.sample.wallet.blockchain

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BalanceApiService {
    @GET("/v1/account/{address}/balance")
    suspend fun getBalance(
        @Path("address") address: String,
        @Query("projectId") projectId: String,
        @Query("currency") currency: String = "usd"
    ): Response<BalanceResponse>
}

