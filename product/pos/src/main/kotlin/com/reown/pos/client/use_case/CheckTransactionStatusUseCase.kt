package com.reown.pos.client.use_case

import android.util.Log
import com.reown.pos.client.POS
import com.reown.pos.client.service.BlockchainApi
import com.reown.pos.client.service.model.CheckTransactionParams
import com.reown.pos.client.service.model.CheckTransactionResult
import com.reown.pos.client.service.model.JsonRpcCheckTransactionRequest
import kotlinx.coroutines.delay

internal class CheckTransactionStatusUseCase(
    private val blockchainApi: BlockchainApi
) {
    companion object {
        private const val MAX_POLLING_ATTEMPTS = 10
        private const val DEFAULT_CHECK_INTERVAL_MS = 3000L
        private const val TAG = "CheckTransactionStatusUseCase"
    }

    suspend fun checkTransactionStatusWithPolling(
        sendResult: String,
        transactionId: String,
        onResult: (POS.Model.PaymentEvent) -> Unit
    ) {
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank" }
        require(sendResult.isNotBlank()) { "Transaction hash cannot be blank" }

        repeat(MAX_POLLING_ATTEMPTS) { attempt ->
            when (val statusResult = checkTransactionStatus(transactionId, sendResult)) {
                is TransactionStatusResult.Success -> {
                    when (statusResult.result.status.uppercase()) {
                        "CONFIRMED" -> {
                            Log.d(TAG, "Transaction confirmed: $sendResult")
                            onResult(POS.Model.PaymentEvent.PaymentSuccessful(txHash = sendResult))
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
                            onResult(POS.Model.PaymentEvent.Error(
                                error = Exception("Unknown transaction status: ${statusResult.result.status}")
                            ))
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

        // Timeout reached
        Log.w(TAG, "Transaction still pending after $MAX_POLLING_ATTEMPTS attempts: $sendResult")
        onResult(POS.Model.PaymentEvent.Error(
            error = Exception("Transaction still pending after timeout")
        ))
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

    /**
     * Sealed class representing the result of a transaction status check.
     */
    private sealed class TransactionStatusResult {
        data class Success(val result: CheckTransactionResult) : TransactionStatusResult()
        data class Error(val message: String) : TransactionStatusResult()
    }
}