package com.reown.sample.wallet.blockchain

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: List<Any>,
    val id: Int
)
