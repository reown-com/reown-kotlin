package com.reown.appkit.data.json_rpc.balance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.util.generateId
import com.reown.appkit.data.json_rpc.JsonRpcMethod

@JsonClass(generateAdapter = true)
internal data class BalanceRequest(
    val address: String,
    @Json(name = "id")
    val id: Long = generateId(),
    @Json(name = "jsonrpc")
    val jsonrpc: String = "2.0",
    @Json(name = "method")
    val method: String = JsonRpcMethod.ETH_GET_BALANCE,
    @Json(name = "params")
    val params: List<String> = listOf(address, "latest")
)
