package com.reown.sample.wallet.blockchain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class JsonRpcRequest(
    @SerializedName("jsonrpc")
    val jsonrpc: String = "2.0",
    @SerializedName("method")
    val method: String,
    @SerializedName("params")
    val params: List<Any>,
    @SerializedName("id")
    val id: Int
)