@file:OptIn(ChainAbstractionExperimentalApi::class)

package com.reown.sample.wallet.domain

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.wallet.domain.WCDelegate._walletEvents
import com.reown.sample.wallet.domain.WCDelegate.scope
import com.reown.walletkit.client.ChainAbstractionExperimentalApi
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ChainAbstractionExperimentalApi::class)
fun prepare(sessionRequest: Wallet.Model.SessionRequest, initialTransaction: Wallet.Model.InitialTransaction, verifyContext: Wallet.Model.VerifyContext) {
    try {
        WalletKit.ChainAbstraction.prepare(
            initialTransaction,
            onSuccess = { result ->
                when (result) {
                    is Wallet.Model.PrepareSuccess.Available -> {
                        println("Prepare success available: $result")
                        emitChainAbstractionRequest(sessionRequest, result, verifyContext)
                    }

                    is Wallet.Model.PrepareSuccess.NotRequired -> {
                        println("Prepare success not required: $result")
                        emitSessionRequest(sessionRequest, verifyContext)
                    }
                }
            },
            onError = { error ->
                println("Prepare error: $error")
                respondWithError(getErrorMessage(), sessionRequest)
                emitChainAbstractionError(sessionRequest, error, verifyContext)
            }
        )
    } catch (e: Exception) {
        println("Prepare: Unknown error: $e")
        respondWithError(e.message ?: "Prepare: Unknown error", sessionRequest)
        emitChainAbstractionError(sessionRequest, Wallet.Model.PrepareError.Unknown(e.message ?: "Prepare: Unknown error"), verifyContext)
    }
}

fun respondWithError(errorMessage: String, sessionRequest: Wallet.Model.SessionRequest?) {
    if (sessionRequest != null) {
        val result = Wallet.Params.SessionRequestResponse(
            sessionTopic = sessionRequest.topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                id = sessionRequest.request.id,
                code = 500,
                message = errorMessage
            )
        )
        try {
            WalletKit.respondSessionRequest(result,
                onSuccess = {
                    println("Error sent success")
                    clearSessionRequest()
                },
                onError = { error ->
                    println("Error sent error: $error")
                    recordError(error.throwable)
                })
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
        }
    }
}

suspend fun getTransactionsDetails(): Result<Wallet.Model.TransactionsDetails> =
    suspendCoroutine { continuation ->
        try {
            WalletKit.ChainAbstraction.getTransactionsDetails(
                WCDelegate.fulfilmentAvailable!!,
                onSuccess = {
                    println("Transaction details SUCCESS: $it")
                    continuation.resume(Result.success(it))
                },
                onError = {
                    println("Transaction details ERROR: $it")
                    recordError(Throwable(it.throwable))
                    continuation.resume(Result.failure(it.throwable))
                }
            )
        } catch (e: Exception) {
            println("Transaction details utils: $e")
            recordError(e)
            continuation.resume(Result.failure(e))
        }
    }

@OptIn(ChainAbstractionExperimentalApi::class)
suspend fun execute(transactionDetails: Wallet.Model.TransactionsDetails, fulfilmentTxs: List<String>, initialTx: String): Result<Wallet.Model.ExecuteSuccess> =
    suspendCoroutine { continuation ->
        try {
            WalletKit.ChainAbstraction.execute(transactionDetails, fulfilmentTxs, initialTx,
                onSuccess = {
                    println("kobe: Execute SUCCESS: $it")
                    continuation.resume(Result.success(it))
                },
                onError = {
                    println("kobe: Execute ERROR: $it")
                    recordError(it.throwable)
                    continuation.resume(Result.failure(it.throwable))
                }
            )

        } catch (e: Exception) {
            println("Catch status utils: $e")
            recordError(e)
            continuation.resume(Result.failure(e))
        }
    }

//suspend fun status(): Result<Wallet.Model.Status> =
//    suspendCoroutine { continuation ->
//        try {
//            WalletKit.ChainAbstraction.status(
//                WCDelegate.fulfilmentAvailable!!.fulfilmentId,
//                WCDelegate.fulfilmentAvailable!!.checkIn,
//                onSuccess = {
//                    println("Fulfilment status SUCCESS: $it")
//                    continuation.resume(Result.success(it))
//                },
//                onError = {
//                    println("Fulfilment status ERROR: $it")
//                    recordError(Throwable(it.reason))
//                    continuation.resume(Result.failure(Exception(it.reason)))
//                }
//            )
//        } catch (e: Exception) {
//            println("Catch status utils: $e")
//            recordError(e)
//            continuation.resume(Result.failure(e))
//        }
//    }

fun emitSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)

        scope.launch {
            _walletEvents.emit(sessionRequest)
        }
    }
}

fun emitChainAbstractionRequest(sessionRequest: Wallet.Model.SessionRequest, fulfilment: Wallet.Model.PrepareSuccess.Available, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)
        WCDelegate.fulfilmentAvailable = fulfilment

        scope.launch {
            async { getTransactionsDetails() }.await().fold(
                onSuccess = { WCDelegate.transactionsDetails = it },
                onFailure = { error -> println("Failed getting tx details: $error") }
            )

            _walletEvents.emit(fulfilment)
        }
    }
}

fun emitChainAbstractionError(sessionRequest: Wallet.Model.SessionRequest, prepareError: Wallet.Model.PrepareError, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)
        WCDelegate.prepareError = prepareError
        recordError(Throwable(getErrorMessage()))

        scope.launch {
            _walletEvents.emit(prepareError)
        }
    }
}

fun getErrorMessage(): String {
    return when (val error = WCDelegate.prepareError) {
        Wallet.Model.PrepareError.InsufficientFunds -> "Insufficient funds"
        Wallet.Model.PrepareError.InsufficientGasFunds -> "Insufficient gas funds"
        Wallet.Model.PrepareError.NoRoutesAvailable -> "No routes available"
        is Wallet.Model.PrepareError.Unknown -> error.message
        else -> "Unknown Error"
    }
}

fun clearSessionRequest() {
    WCDelegate.sessionRequestEvent = null
    WCDelegate.currentId = null
//        sessionRequestUI = SessionRequestUI.Initial
}

fun recordError(throwable: Throwable) {
    mixPanel.track("error: $throwable; errorMessage: ${throwable.message}")
    Firebase.crashlytics.recordException(throwable)
}
