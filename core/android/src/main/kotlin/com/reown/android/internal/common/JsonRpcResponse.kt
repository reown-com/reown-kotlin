package com.reown.android.internal.common

import com.squareup.moshi.JsonClass
import com.reown.android.internal.common.model.type.SerializableJsonRpc

sealed class JsonRpcResponse : SerializableJsonRpc {
    abstract val id: Long

    @JsonClass(generateAdapter = false)
    data class JsonRpcResult(
        override val id: Long,
        val jsonrpc: String = "2.0",
        val result: Any,
    ) : JsonRpcResponse()

    @JsonClass(generateAdapter = true)
    data class JsonRpcError(
        override val id: Long,
        val jsonrpc: String = "2.0",
        val error: Error,
    ) : JsonRpcResponse() {
        val errorMessage: String = "${error.message} : code: ${error.code}"
    }

    @JsonClass(generateAdapter = true)
    data class Error(
        val code: Int,
        val message: String,
    )
}