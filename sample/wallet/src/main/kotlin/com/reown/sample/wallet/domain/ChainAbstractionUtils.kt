package com.reown.sample.wallet.domain

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
                //todo: send JsonRpcError response to dapp
                emitChainAbstractionError(sessionRequest, error, verifyContext)
            }
        )
    } catch (e: Exception) {
        println("kobe: CanFulfil: Unknown error: $e")
        //todo: send JsonRpcError response to dapp
        emitChainAbstractionError(sessionRequest, Wallet.Model.FulfilmentError.Unknown(e.message ?: "CanFulfil: Unknown error"), verifyContext)
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