package com.reown.pos.client.use_case

import android.util.Log
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.wcKoinApp
import com.reown.pos.client.POS
import com.reown.pos.client.service.BlockchainApi
import com.reown.pos.client.service.model.CheckTransactionParams
import com.reown.pos.client.service.model.CheckTransactionResult
import com.reown.pos.client.service.model.JsonRpcCheckTransactionRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.delay
import org.koin.core.qualifier.named

internal class CheckTransactionStatusUseCase(
    private val blockchainApi: BlockchainApi
) {
    companion object {
        private const val MAX_POLLING_ATTEMPTS = 45
        private const val DEFAULT_CHECK_INTERVAL_MS = 3000L
        private const val TAG = "CheckTransactionStatusUseCase"
    }

    private val moshi: Moshi by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.MOSHI)) }

    suspend fun checkTransactionStatusWithPolling(
        sendResult: Any,
        transactionId: String,
        onResult: (POS.Model.PaymentEvent) -> Unit
    ) {
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank" }
        val sendResultJson = stringifySendResult(sendResult)
        repeat(MAX_POLLING_ATTEMPTS) { attempt ->
            when (val statusResult = checkTransactionStatus(transactionId, sendResultJson)) {
                is TransactionStatusResult.Success -> {
                    when (statusResult.result.status.uppercase()) {
                        "CONFIRMED" -> {
                            Log.d(TAG, "Transaction confirmed: $sendResult")
                            onResult(POS.Model.PaymentEvent.PaymentSuccessful(result = sendResult))
                            return
                        }

                        "FAILED" -> {
                            Log.e(TAG, "Transaction failed: $sendResult")
                            onResult(POS.Model.PaymentEvent.PaymentRejected(message = "Transaction failed on blockchain"))
                            return
                        }

                        "PENDING" -> {
                            Log.d(TAG, "Transaction pending, attempt ${attempt + 1}/$MAX_POLLING_ATTEMPTS")
                            if (attempt < MAX_POLLING_ATTEMPTS - 1) {
                                delay(statusResult.result.checkIn ?: DEFAULT_CHECK_INTERVAL_MS)
                            }
                        }

                        else -> {
                            Log.e(TAG, "Unknown transaction status: ${statusResult.result.status}")
                            onResult(
                                POS.Model.PaymentEvent.Error(
                                    error = Exception("Unknown transaction status: ${statusResult.result.status}")
                                )
                            )
                            return
                        }
                    }
                }

                is TransactionStatusResult.Error -> {
                    Log.e(TAG, "Error checking transaction status: ${statusResult.message}")
                    onResult(POS.Model.PaymentEvent.Error(error = Exception(statusResult.message)))
                    return
                }
            }
        }

        Log.w(TAG, "Transaction still pending after $MAX_POLLING_ATTEMPTS attempts: $sendResult")
        onResult(
            POS.Model.PaymentEvent.Error(
                error = Exception("Transaction still pending after timeout")
            )
        )
    }

    private fun stringifySendResult(result: Any): String {
        if (result is String) {
            val trimmed = result.trim()
            val looksLikeJson = trimmed.startsWith("{") && trimmed.contains(":")
            val looksLikeMapString = trimmed.startsWith("{") && trimmed.contains("=") && !trimmed.contains(":")
            if (looksLikeJson) return trimmed
            if (looksLikeMapString) {
                val content = trimmed.removePrefix("{").removeSuffix("}")
                val map = linkedMapOf<String, String>()
                content.split(",").forEach { pair ->
                    val idx = pair.indexOf("=")
                    if (idx > 0) {
                        val key = pair.substring(0, idx).trim()
                        val value = pair.substring(idx + 1).trim()
                        map[key] = value
                    }
                }
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val adapter = moshi.adapter<Map<String, String>>(mapType)
                return adapter.toJson(map)
            }
            return trimmed
        }

        if (result is Map<*, *>) {
            val stringMap = result.entries.associate { (k, v) -> k.toString() to (v?.toString() ?: "") }
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val adapter = moshi.adapter<Map<String, String>>(mapType)
            return adapter.toJson(stringMap)
        }

        return result.toString()
    }

    private suspend fun checkTransactionStatus(
        transactionId: String,
        txHash: String
    ): TransactionStatusResult {
        return try {
            val request = JsonRpcCheckTransactionRequest(
                params = CheckTransactionParams(
                    id = transactionId,
                    sendResult = txHash
                )
            )

            val response = blockchainApi.checkTransactionStatus(request)

            when {
                response.error != null -> {
                    TransactionStatusResult.Error(
                        "Check transaction status failed: ${response.error.message} (code: ${response.error.code})"
                    )
                }

                response.result == null -> {
                    TransactionStatusResult.Error("Check transaction status response is null")
                }

                else -> {
                    TransactionStatusResult.Success(response.result)
                }
            }
        } catch (e: Exception) {
            TransactionStatusResult.Error("Failed to check transaction status: ${e.message}")
        }
    }

    private sealed class TransactionStatusResult {
        data class Success(val result: CheckTransactionResult) : TransactionStatusResult()
        data class Error(val message: String) : TransactionStatusResult()
    }
}