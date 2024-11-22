package com.reown.sample.wallet.domain

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.wallet.domain.WCDelegate._walletEvents
import com.reown.sample.wallet.domain.WCDelegate.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.launch

fun canFulfil(sessionRequest: Wallet.Model.SessionRequest, originalTransaction: Wallet.Model.Transaction, verifyContext: Wallet.Model.VerifyContext) {
    try {
        WalletKit.canFulfil(
            originalTransaction,
            onSuccess = { result ->
                when (result) {
                    is Wallet.Model.FulfilmentSuccess.Available -> {
                        println("kobe: fulfil success available: $result")
                        emitChainAbstractionRequest(sessionRequest, result, verifyContext)
                    }

                    is Wallet.Model.FulfilmentSuccess.NotRequired -> {
                        println("kobe: fulfil success not required: $result")
                        emitSessionRequest(sessionRequest, verifyContext)
                    }
                }
            },
            onError = { error ->
                println("kobe: fulfil error: $error")
                respondWithError(getErrorMessage(), sessionRequest)
                emitChainAbstractionError(sessionRequest, error, verifyContext)
            }
        )
    } catch (e: Exception) {
        println("kobe: CanFulfil: Unknown error: $e")
        respondWithError(e.message ?: "CanFulfil: Unknown error", sessionRequest)
        emitChainAbstractionError(sessionRequest, Wallet.Model.FulfilmentError.Unknown(e.message ?: "CanFulfil: Unknown error"), verifyContext)
    }
}

fun respondWithError(errorMessage: String, sessionRequest: Wallet.Model.SessionRequest) {
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
                println("kobe: Error sent success")
                clearSessionRequest()
            },
            onError = { error ->
                println("kobe: Error sent error: $error")
                Firebase.crashlytics.recordException(error.throwable)
            })
    } catch (e: Exception) {
        Firebase.crashlytics.recordException(e)
    }
}

fun emitSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)

        scope.launch {
            _walletEvents.emit(sessionRequest)
        }
    }
}

fun emitChainAbstractionRequest(sessionRequest: Wallet.Model.SessionRequest, fulfilment: Wallet.Model.FulfilmentSuccess.Available, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)
        WCDelegate.fulfilmentAvailable = fulfilment

        scope.launch {
            _walletEvents.emit(fulfilment)
        }
    }
}

fun emitChainAbstractionError(sessionRequest: Wallet.Model.SessionRequest, fulfilmentError: Wallet.Model.FulfilmentError, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)
        WCDelegate.fulfilmentError = fulfilmentError

        scope.launch {
            _walletEvents.emit(fulfilmentError)
        }
    }
}

fun getErrorMessage(): String {
    return when (val error = WCDelegate.fulfilmentError) {
        Wallet.Model.FulfilmentError.InsufficientFunds -> "Insufficient funds"
        Wallet.Model.FulfilmentError.InsufficientGasFunds -> "Insufficient gas funds"
        Wallet.Model.FulfilmentError.NoRoutesAvailable -> "No routes available"
        is Wallet.Model.FulfilmentError.Unknown -> error.message
        else -> "Unknown Error"
    }
}

fun clearSessionRequest() {
    WCDelegate.sessionRequestEvent = null
    WCDelegate.currentId = null
//        sessionRequestUI = SessionRequestUI.Initial
}
