package com.reown.pos.client.service.model

import com.reown.pos.client.service.generateId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonRpcCheckTransactionRequest(
    @param:Json(name = "id")
    val id: Int = generateId(),
    @param:Json(name = "jsonrpc")
    val jsonrpc: String = "2.0",
    @param:Json(name = "method")
    val method: String = "wc_pos_checkTransaction",
    @param:Json(name = "params")
    val params: CheckTransactionParams,
)

@JsonClass(generateAdapter = true)
data class CheckTransactionParams(
    @param:Json(name = "id")
    val id: String,
    @param:Json(name = "sendResult")
    val sendResult: String
)

@JsonClass(generateAdapter = true)
data class JsonRpcCheckTransactionResponse(
    @Json(name = "jsonrpc")
    val jsonrpc: String,
    @Json(name = "id")
    val id: Int,
    @Json(name = "result")
    val result: CheckTransactionResult?,
    @Json(name = "error")
    val error: JsonRpcError?
)

@JsonClass(generateAdapter = true)
data class CheckTransactionResult(
    @Json(name = "status")
    val status: String,
    @Json(name = "txid")
    val txid: String?,
    @Json(name = "checkIn")
    val checkIn: Long?
)