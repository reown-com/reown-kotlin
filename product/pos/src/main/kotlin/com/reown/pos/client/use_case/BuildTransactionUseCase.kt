package com.reown.pos.client.use_case

import android.util.Log
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.wcKoinApp
import com.reown.pos.client.POS
import com.reown.pos.client.service.BlockchainApi
import com.reown.pos.client.service.model.BuildTransactionParams
import com.reown.pos.client.service.model.JsonRpcBuildTransactionRequest
import com.reown.pos.client.service.model.PaymentIntent
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.squareup.moshi.Moshi
import org.koin.core.qualifier.named

internal class BuildTransactionUseCase(
    private val blockchainApi: BlockchainApi
) {
    companion object {
        private const val TAG = "BuildTransactionUseCase"
    }

    private val moshi: Moshi by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.MOSHI)) }

    suspend fun build(
        paymentIntent: POS.Model.PaymentIntent?,
        approvedSession: Sign.Model.ApprovedSession,
        onSuccess: (Pair<POS.Model.PaymentEvent, String>) -> Unit,
        onError: (POS.Model.PaymentEvent) -> Unit
    ) {
        require(paymentIntent != null) { "PaymentIntent cannot be null" }

        val senderAddress = findSenderAddress(approvedSession, paymentIntent.token.network.chainId)
            ?: run {
                val errorMessage = "No matching account found for chain ${paymentIntent.token.network.chainId}"
                Log.e(TAG, errorMessage)
                onError(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
                return
            }

        val buildResult = buildTransaction(paymentIntent, senderAddress)
        when (buildResult) {
            is BuildTransactionResult.Success -> {
                sendTransactionRequest(
                    transactionRpc = buildResult.transactions[0], //TODO: Update to send all transactions one by one to the wallet
                    transactionId = buildResult.transactions[0].id, //TODO: Update to send all transactions one by one to the wallet
                    paymentIntent = paymentIntent,
                    approvedSession = approvedSession,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }

            is BuildTransactionResult.Error -> {
                println("kobe: build error: ${buildResult.message}")
                onError(POS.Model.PaymentEvent.Error(error = Exception(buildResult.message)))
            }
        }
    }

    private suspend fun buildTransaction(
        paymentIntent: POS.Model.PaymentIntent,
        senderAddress: String
    ): BuildTransactionResult {
        return try {
            val request = JsonRpcBuildTransactionRequest(
                params = BuildTransactionParams(
                    paymentIntents = listOf(
                        PaymentIntent(
                            asset = paymentIntent.caip19Token,
                            recipient = paymentIntent.recipient,
                            sender = senderAddress,
                            amount = paymentIntent.amount
                        )
                    )
                )
            )

            val response = blockchainApi.buildTransaction(request)

            when {
                response.error != null -> {
                    val errorMessage = "Build transaction failed: ${response.error.message} (code: ${response.error.code})"
                    Log.e(TAG, errorMessage)
                    BuildTransactionResult.Error(errorMessage)
                }

                response.result == null -> {
                    val errorMessage = "Build transaction response is null"
                    Log.e(TAG, errorMessage)
                    BuildTransactionResult.Error(errorMessage)
                }

                else -> {
                    Log.d(TAG, "Transaction built successfully")
                    BuildTransactionResult.Success(transactions = response.result.transactions)
                }
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to build transaction: ${e.message}"
            Log.e(TAG, errorMessage, e)
            BuildTransactionResult.Error(errorMessage)
        }
    }

    private fun sendTransactionRequest(
        transactionRpc: com.reown.pos.client.service.model.TransactionRpc,
        transactionId: String,
        paymentIntent: POS.Model.PaymentIntent,
        approvedSession: Sign.Model.ApprovedSession,
        onSuccess: (Pair<POS.Model.PaymentEvent, String>) -> Unit,
        onError: (POS.Model.PaymentEvent) -> Unit
    ) {
        try {
            val paramsJson = moshi.adapter(Any::class.java).toJson(transactionRpc.params)
            val request = Sign.Params.Request(
                sessionTopic = approvedSession.topic,
                method = transactionRpc.method,
                params = paramsJson,
                chainId = paymentIntent.token.network.chainId
            )

            SignClient.request(
                request = request,
                onSuccess = { _ ->
                    Log.d(TAG, "Transaction request sent successfully")
                    onSuccess(Pair(POS.Model.PaymentEvent.PaymentRequested, transactionId))
                },
                onError = { error ->
                    val errorMessage = "Failed to send transaction request: ${error.throwable.message}"
                    Log.e(TAG, errorMessage, error.throwable)
                    onError(POS.Model.PaymentEvent.ConnectionFailed(error.throwable))
                }
            )
        } catch (e: Exception) {
            val errorMessage = "Failed to send transaction request: ${e.message}"
            Log.e(TAG, errorMessage, e)
            onError(POS.Model.PaymentEvent.Error(error = Exception(errorMessage)))
        }
    }

    private fun findSenderAddress(
        approvedSession: Sign.Model.ApprovedSession,
        chainId: String
    ): String? = approvedSession.namespaces.values
        .flatMap { session -> session.accounts }
        .firstOrNull { account -> account.startsWith(chainId) }

    sealed class BuildTransactionResult {
        data class Success(val transactions: List<com.reown.pos.client.service.model.TransactionRpc>) : BuildTransactionResult()

        data class Error(val message: String) : BuildTransactionResult()
    }
}