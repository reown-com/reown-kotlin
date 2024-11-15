package com.reown.sample.wallet.blockchain

data class JsonRpcResponse<T>(
    val jsonrpc: String,
    val id: Int,
    val result: T?,
    val error: JsonRpcError?
)

data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: Any?
)