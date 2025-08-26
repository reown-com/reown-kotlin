package com.reown.pos.client.service

import com.reown.pos.client.service.model.JsonRpcBuildTransactionRequest
import com.reown.pos.client.service.model.JsonRpcBuildTransactionResponse
import com.reown.pos.client.service.model.JsonRpcCheckTransactionRequest
import com.reown.pos.client.service.model.JsonRpcCheckTransactionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BlockchainApi {
    @POST("/v1/json-rpc")
    suspend fun buildTransaction(@Body request: JsonRpcBuildTransactionRequest): JsonRpcBuildTransactionResponse

    @POST("/v1/json-rpc")
    suspend fun checkTransactionStatus(@Body request: JsonRpcCheckTransactionRequest): JsonRpcCheckTransactionResponse
}