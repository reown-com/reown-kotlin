package com.reown.pos.client.use_case

import android.util.Log
import com.reown.pos.client.service.BlockchainApi
import com.reown.pos.client.service.model.JsonRpcSupportedNetworksRequest
import com.reown.pos.client.service.model.SupportedNamespace
import com.reown.pos.client.service.model.SupportedNetworksParams

internal class GetSupportedNetworksUseCase(
    private val blockchainApi: BlockchainApi
) {
    companion object {
        private const val TAG = "GetSupportedNetworksUseCase"
    }

    sealed class Result {
        data class Success(val namespaces: List<SupportedNamespace>) : Result()
        data class Error(val throwable: Throwable) : Result()
    }

    suspend fun get(): Result = try {
        val response = blockchainApi.getSupportedNetworks(JsonRpcSupportedNetworksRequest(params = SupportedNetworksParams()))

        println("kobe: Response: $response")

        when {
            response.error != null -> {
                val message = "Supported networks failed: ${response.error.message} (code: ${response.error.code})"
                Log.e(TAG, message)
                Result.Error(Exception(message))
            }
            response.result == null -> {
                val message = "Supported networks response is null"
                Log.e(TAG, message)
                Result.Error(Exception(message))
            }
            else -> Result.Success(response.result.namespaces)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Supported networks request exception", e)
        Result.Error(e)
    }
}


