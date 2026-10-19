package com.reown.android.internal

import com.squareup.moshi.Moshi
import com.tinder.scarlet.utils.getRawType
import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.adapter.JsonRpcResultAdapter
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.android.internal.common.signing.cacao.Cacao
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.jvm.jvmName

internal class JsonRpcResponseJsonRpcResultJsonAdapterTest {

    @Test
    fun `test sign params to json`() {
        val moshi = Moshi.Builder().add { type, _, moshi ->
            return@add if (type.getRawType().name == JsonRpcResponse.JsonRpcResult::class.jvmName) {
                JsonRpcResultAdapter(moshi = moshi)
            } else {
                null
            }
        }.build()
        val adapter = moshi.adapter(JsonRpcResponse.JsonRpcResult::class.java)
        val approvalParams =
            CoreSignParams.ApprovalParams(relay = RelayProtocolOptions("irn"), responderPublicKey = "124")
        val jsonResult = JsonRpcResponse.JsonRpcResult(
            id = 1L,
            jsonrpc = "2.0",
            result = approvalParams
        )

        val result = adapter.toJson(jsonResult)
        println()
        println(result)
        println()
    }

    @Test
    fun `test sign params from json`() {
        val moshi = Moshi.Builder().add { type, _, moshi ->
            return@add if (type.getRawType().name == JsonRpcResponse.JsonRpcResult::class.jvmName) {
                JsonRpcResultAdapter(moshi = moshi)
            } else {
                null
            }
        }.build()
        val adapter = moshi.adapter(JsonRpcResponse.JsonRpcResult::class.java)
        val approvalParamsJsonResult = JsonRpcResponse.JsonRpcResult(
            id = 11L,
            result = CoreSignParams.ApprovalParams(relay = RelayProtocolOptions("irn"), responderPublicKey = "124")
        )
        val resultString = moshi.adapter(JsonRpcResponse.JsonRpcResult::class.java).toJson(approvalParamsJsonResult)
        val result = adapter.fromJson(resultString)
        result is JsonRpcResponse.JsonRpcResult
        println()
        println(result)
        println()
    }

    @Test
    fun `test from json with boolean`() {
        val moshi = Moshi.Builder().add { type, _, moshi ->
            return@add if (type.getRawType().name == JsonRpcResponse.JsonRpcResult::class.jvmName) {
                JsonRpcResultAdapter(moshi = moshi)
            } else {
                null
            }
        }.build()
        val adapter = moshi.adapter(JsonRpcResponse.JsonRpcResult::class.java)

        val approvalParamsJsonResult = JsonRpcResponse.JsonRpcResult(id = 11L, result = true)
        val resultString = moshi.adapter(JsonRpcResponse.JsonRpcResult::class.java).toJson(approvalParamsJsonResult)

        val result = adapter.fromJson(resultString)
        result is JsonRpcResponse.JsonRpcResult
        println()
        println(result)
        println()
    }

    @Test
    fun `result object with an auth field decodes as a map and keeps all fields`() {
        val moshi = Moshi.Builder().add { type, _, moshi ->
            return@add if (type.getRawType().name == JsonRpcResponse.JsonRpcResult::class.jvmName) {
                JsonRpcResultAdapter(moshi = moshi)
            } else {
                null
            }
        }.build()
        val adapter = moshi.adapter(JsonRpcResponse.JsonRpcResult::class.java)

        listOf("auth", "responseAuth", "updateAuth").forEach { authField ->
            val json = """{"id":1,"jsonrpc":"2.0","result":{"$authField":"did.jwt.auth","signature":"0xabc"}}"""

            val result = requireNotNull(adapter.fromJson(json))

            assertEquals(mapOf(authField to "did.jwt.auth", "signature" to "0xabc"), result.result)
            assertEquals(json, adapter.toJson(result))
        }
    }
}