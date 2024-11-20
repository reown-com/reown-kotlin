package com.reown.sample.wallet.domain

import com.reown.sample.wallet.domain.WCDelegate._walletEvents
import com.reown.sample.wallet.domain.WCDelegate.fulfilmentAvailable
import com.reown.sample.wallet.domain.WCDelegate.scope
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.launch
import org.json.JSONArray

fun getOriginalTransaction(sessionRequest: Wallet.Model.SessionRequest): Wallet.Model.Transaction {
    val requestParams = JSONArray(sessionRequest.request.params).getJSONObject(0)
    val from = requestParams.getString("from")
    val to = requestParams.getString("to")
    val data = requestParams.getString("data")
    val value = try {
        requestParams.getString("value")
    } catch (e: Exception) {
        "0"
    }

    return Wallet.Model.Transaction(
        from = from,
        to = to,
        value = value,
        data = data,
        nonce = "0",
        gas = "0",
        gasPrice = "0",
        chainId = sessionRequest.chainId!!,
        maxPriorityFeePerGas = "0",
        maxFeePerGas = "0"
    )
}

fun canFulfil(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
    try {
        WalletKit.canFulfil(
            WCDelegate.originalTransaction!!,
            onSuccess = { result ->
                //todo: if fulfilment success amit fulfilment even to UI, if fulfilment not required proceed with the normal flow
                println("kobe: fulfil success: $result")
                if (result is Wallet.Model.FulfilmentSuccess.Available) {
                    fulfilmentAvailable = result
                    emitSessionRequest(sessionRequest, verifyContext)
                } else if (result is Wallet.Model.FulfilmentSuccess.NotRequired) {
                    //todo: proceed with normal flow
                    emitSessionRequest(sessionRequest, verifyContext)
                }
            },
            onError = { error ->
                //todo: emit error to the user and send response error to a dapp
                println("kobe: fulfil error: $error")
                emitSessionRequest(sessionRequest, verifyContext)
            }
        )
    } catch (e: Exception) {
        //todo: emit error to the user and send response error to a dapp
        println("kobe: try error: $e")
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