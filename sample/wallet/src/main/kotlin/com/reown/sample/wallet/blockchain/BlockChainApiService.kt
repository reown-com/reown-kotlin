package com.reown.sample.wallet.blockchain

import retrofit2.http.Body
import retrofit2.http.POST

interface BlockChainApiService {
    @POST("/v1")
    suspend fun sendJsonRpcRequest(@Body request: JsonRpcRequest): JsonRpcResponse<Any>
}