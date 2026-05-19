package com.reown.sample.wallet.blockchain

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FungiblePriceApiService {
    @POST("/v1/fungible/price")
    suspend fun getFungiblePrice(@Body body: FungiblePriceRequest): Response<FungiblePriceResponse>
}

data class FungiblePriceRequest(
    val projectId: String,
    val currency: String,
    val addresses: List<String>,
)

data class FungiblePriceResponse(
    val fungibles: List<FungiblePriceEntry>? = null,
)

data class FungiblePriceEntry(
    val address: String? = null,
    val price: Double? = null,
    val symbol: String? = null,
)
