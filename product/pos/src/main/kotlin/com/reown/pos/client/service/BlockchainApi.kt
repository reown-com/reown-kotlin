package com.reown.pos.client.service

import com.reown.pos.client.service.model.JsonRpcBuildTransactionRequest
import com.reown.pos.client.service.model.JsonRpcBuildTransactionResponse
import com.reown.pos.client.service.model.JsonRpcCheckTransactionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BlockchainApi {
    @POST("/v1")
    suspend fun buildTransaction(@Body request: JsonRpcBuildTransactionRequest): JsonRpcBuildTransactionResponse

    @POST("/v1")
    suspend fun checkTransactionStatus(@Body request: JsonRpcBuildTransactionRequest): JsonRpcCheckTransactionResponse
}