package com.reown.pos.client.service.model

import com.reown.pos.client.service.generateId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonRpcBuildTransactionRequest(
    @param:Json(name = "id")
    val id: Int = generateId(),
    @param:Json(name = "jsonrpc")
    val jsonrpc: String = "2.0",
    @param:Json(name = "method")
    val method: String = "wc_pos_buildTransactions",
    @param:Json(name = "params")
    val params: BuildTransactionParams,
)

@JsonClass(generateAdapter = true)
data class BuildTransactionParams(
    @param:Json(name = "paymentIntents")
    val paymentIntents: List<PaymentIntent>,
    @param:Json(name = "capabilities")
    val capabilities: Any? = null
)

data class PaymentIntent(
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
    @Json(name = "transactions")
    val transactions: List<TransactionRpc>
)

@JsonClass(generateAdapter = true)
data class TransactionRpc(
    @Json(name = "method")
    val method: String,
    @Json(name = "chainId")
    val chainId: String,
    @Json(name = "id")
    val id: String,
    @Json(name = "params")
    val params: Any
)