@file:OptIn(ChainAbstractionExperimentalApi::class)

package com.reown.sample.wallet.domain

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.wallet.domain.WCDelegate._walletEvents
import com.reown.sample.wallet.domain.WCDelegate.scope
import com.reown.walletkit.client.ChainAbstractionExperimentalApi
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ChainAbstractionExperimentalApi::class)
suspend fun execute(
    prepareAvailable: Wallet.Model.PrepareSuccess.Available,
    signedTxs: List<Wallet.Model.RouteSig>,
    initialTx: String
): Result<Wallet.Model.ExecuteSuccess> =
    suspendCoroutine { continuation ->
        try {
            WalletKit.ChainAbstraction.execute(
                prepareAvailable, signedTxs, initialTx,
                onSuccess = { executeSuccess ->
                    continuation.resume(Result.success(executeSuccess))
                },
                onError = { executeError ->
                    recordError(executeError.throwable)
                    continuation.resume(Result.failure(executeError.throwable))
                }
            )

        } catch (e: Exception) {
            recordError(e)
            continuation.resume(Result.failure(e))
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
            WalletKit.respondSessionRequest(
                result,
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

fun emitSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)

        scope.launch {
            _walletEvents.emit(sessionRequest)
        }
    }
}

fun emitChainAbstractionRequest(
    sessionRequest: Wallet.Model.SessionRequest,
    fulfilment: Wallet.Model.PrepareSuccess.Available,
    verifyContext: Wallet.Model.VerifyContext
) {
    if (WCDelegate.currentId != sessionRequest.request.id) {
        WCDelegate.sessionRequestEvent = Pair(sessionRequest, verifyContext)
        WCDelegate.prepareAvailable = fulfilment

        scope.launch {
            _walletEvents.emit(fulfilment)
        }
    }
}

fun emitChainAbstractionError(
    sessionRequest: Wallet.Model.SessionRequest,
    prepareError: Wallet.Model.PrepareError,
    verifyContext: Wallet.Model.VerifyContext
) {
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
        is Wallet.Model.PrepareError.InsufficientFunds -> error.message
        is Wallet.Model.PrepareError.InsufficientGasFunds -> error.message
        is Wallet.Model.PrepareError.NoRoutesAvailable -> error.message
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
