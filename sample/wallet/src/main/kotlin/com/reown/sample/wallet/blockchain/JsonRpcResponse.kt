package com.reown.sample.wallet.blockchain

import com.google.gson.annotations.SerializedName

data class JsonRpcResponse<T>(
    @SerializedName("jsonrpc")
    val jsonrpc: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("result")
    val result: T?,
    @SerializedName("error")
    val error: JsonRpcError?
)

data class JsonRpcError(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Any?
)