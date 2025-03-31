package com.reown.sign.engine.domain.wallet_service

import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

internal class WalletServiceRequester(private val okHttpClient: OkHttpClient) {
    suspend fun request(sessionRequest: SignRpc.SessionRequest, walletServiceUri: String): String {
        val jsonRpcRequest = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", sessionRequest.id)
            put("method", sessionRequest.rpcMethod)
            put("params", JSONObject(sessionRequest.rpcParams))
        }

        println("kobe: Sending Request: $jsonRpcRequest to $walletServiceUri")

        val httpRequest = Request.Builder()
            .url(walletServiceUri)
            .post(jsonRpcRequest.toString().toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        val response: Response = okHttpClient.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            var errorMessage: String? = null
            response.body?.string()?.let { responseBody ->
                val jsonObject = JSONObject(responseBody)
                if (jsonObject.has("error")) {
                    val error = jsonObject.getJSONObject("error")
                    errorMessage = error.optString("message")
                }
            }
            throw IllegalStateException("Failed to send request to wallet service: $errorMessage")
        } else {
            val responseBody = response.body?.string() ?: ""

            println("kobe: Wallet Service Response: $responseBody")
            return responseBody
        }
    }
}