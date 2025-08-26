package com.reown.pos.client.service.model

import com.reown.util.generateId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonRpcBuildTransactionRequest(
    @param:Json(name = "id")
    val id: Long = generateId(),
    @param:Json(name = "jsonrpc")
    val jsonrpc: String = "2.0",
    @param:Json(name = "method")
    val method: String = "reown_pos_buildTransaction",
    @param:Json(name = "params")
    val params: BuildTransactionParams,
)

@JsonClass(generateAdapter = true)
data class BuildTransactionParams(
    @param:Json(name = "asset")
    val asset: String,
    @param:Json(name = "recipient")
    val recipient: String,
    @param:Json(name = "sender")
    val sender: String,
    @param:Json(name = "amount")
    val amount: String
)

@JsonClass(generateAdapter = true)
data class JsonRpcBuildTransactionResponse(
    @Json(name = "jsonrpc")
    val jsonrpc: String,
    @Json(name = "id")
    val id: Int,
    @Json(name = "result")
    val result: BuildTransactionParamsResponse?,
    @Json(name = "error")
    val error: JsonRpcError?
)

@JsonClass(generateAdapter = true)
data class BuildTransactionParamsResponse(
    @Json(name = "transactionRpc")
    val transactionRpc: TransactionRpc,
    @Json(name = "id")
    val id: String
)

@JsonClass(generateAdapter = true)
data class TransactionRpc(
    @Json(name = "method")
    val method: String,
    @Json(name = "params")
    val params: List<TransactionParam>
)

@JsonClass(generateAdapter = true)
data class TransactionParam(
    @Json(name = "to")
    val to: String,
    @Json(name = "from")
    val from: String,
    @Json(name = "gas")
    val gas: String,
    @Json(name = "value")
    val value: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "gasPrice")
    val gasPrice: String
)