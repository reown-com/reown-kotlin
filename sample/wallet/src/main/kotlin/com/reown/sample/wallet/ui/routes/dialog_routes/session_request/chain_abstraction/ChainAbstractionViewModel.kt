package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.wallet.domain.Signer
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.clearSessionRequest
import com.reown.sample.wallet.domain.fulfillmentStatus
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.domain.respondWithError
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric

data class TxSuccess(
    val redirect: Uri?,
    val hash: String
)

class ChainAbstractionViewModel : ViewModel() {
    var txHash: String? = null
    var errorMessage: String? = null
    var sessionRequestUI: SessionRequestUI = generateSessionRequestUI()

    fun approve(onSuccess: (TxSuccess) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        try {
            val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
            if (sessionRequest != null) {
                val signedTransactions = mutableListOf<Pair<String, String>>()
                val txHashesChannel = Channel<Pair<String, String>>()
                //sign fulfilment txs
                WCDelegate.fulfilmentAvailable!!.transactions.forEach { transaction ->
                    val signedTransaction = Transaction.sign(transaction)
                    signedTransactions.add(Pair(transaction.chainId, signedTransaction))
                }


                //execute fulfilment txs
                signedTransactions.forEach { (chainId, signedTx) ->
                    viewModelScope.launch {
                        try {
                            println("kobe: send raw: $signedTx")
                            val txHash = Transaction.sendRaw(chainId, signedTx, "Route")
                            println("kobe: receive hash: $txHash")
                            txHashesChannel.send(Pair(chainId, txHash))
                        } catch (e: Exception) {
                            println("kobe: route broadcast tx error: $e")
                            respondWithError(e.message ?: "Route TX broadcast error", WCDelegate.sessionRequestEvent!!.first)
                            return@launch onError(e)
                        }
                    }
                }

                //awaits receipts
                viewModelScope.launch {
                    repeat(signedTransactions.size) {
                        val (chainId, txHash) = txHashesChannel.receive()
                        println("kobe: receive tx hash: $txHash")

                        try {
                            Transaction.getReceipt(chainId, txHash)
                        } catch (e: Exception) {
                            println("kobe: route execution tx error: $e")
                            respondWithError(e.message ?: "Route TX execution error", WCDelegate.sessionRequestEvent!!.first)
                            return@launch onError(e)
                        }
                    }
                    println("kobe: close channel")
                    txHashesChannel.close()


                    //check fulfilment status
                    println("kobe: Fulfilment status check")


                    val fulfilmentResult = async { fulfillmentStatus() }.await()
                    fulfilmentResult.fold(
                        onSuccess = {
                            when (it) {
                                is Wallet.Model.FulfilmentStatus.Error -> {
                                    println("kobe: Fulfilment error: ${it.reason}")
                                    onError(Throwable(it.reason))
                                }

                                is Wallet.Model.FulfilmentStatus.Completed -> {
                                    println("kobe: Fulfilment completed")
                                    //if status completed, execute init tx
                                    with(WCDelegate.originalTransaction!!) {
                                        val nonceResult = Transaction.getNonce(chainId, from)
                                        println("kobe: Original TX")
                                        val signedTx = Transaction.sign(this, nonceResult, DefaultGasProvider.GAS_LIMIT)
                                        try {
                                            val resultTx = Transaction.sendRaw(chainId, signedTx, "Original")
                                            val response = Wallet.Params.SessionRequestResponse(
                                                sessionTopic = sessionRequest.topic,
                                                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.requestId, resultTx)
                                            )
                                            val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()
                                            WalletKit.respondSessionRequest(response,
                                                onSuccess = {
                                                    clearSessionRequest()
                                                    onSuccess(TxSuccess(redirect, resultTx))
                                                },
                                                onError = { error ->
                                                    Firebase.crashlytics.recordException(error.throwable)
                                                    if (error.throwable !is NoConnectivityException) {
                                                        clearSessionRequest()
                                                    }
                                                    onError(error.throwable)
                                                })

                                        } catch (e: Exception) {
                                            respondWithError(e.message ?: "Init TX execution error", WCDelegate.sessionRequestEvent!!.first)
                                            return@launch onError(e)
                                        }
                                    }
                                }
                            }
                        },
                        onFailure = {
                            println("kobe: fold error: $it")
                            onError(Throwable("Fulfilment status error: $it"))
                        }
                    )
                }
            }
        } catch (e: Exception) {
            println("kobe: Error: $e")
            Firebase.crashlytics.recordException(e)
            reject()
            clearSessionRequest()
            onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
        }
    }

    fun reject(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        try {
            val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
            if (sessionRequest != null) {
                val result = Wallet.Params.SessionRequestResponse(
                    sessionTopic = sessionRequest.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = sessionRequest.requestId,
                        code = 500,
                        message = "Kotlin Wallet Error"
                    )
                )
                val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()
                WalletKit.respondSessionRequest(result,
                    onSuccess = {
                        clearSessionRequest()
                        onSuccess(redirect)
                    },
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        if (error.throwable !is NoConnectivityException) {
                            clearSessionRequest()
                        }
                        onError(error.throwable)
                    })
            } else {
                onError(Throwable("Reject - Cannot find session request"))
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            clearSessionRequest()
            onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
        }
    }

    private fun extractMessageParamFromPersonalSign(input: String): String {
        val jsonArray = JSONArray(input)
        return if (jsonArray.length() > 0) {
            if (jsonArray.getString(0).startsWith("0x")) {
                String(Numeric.hexStringToByteArray(jsonArray.getString(0)))
            } else {
                jsonArray.getString(0)
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        return if (WCDelegate.sessionRequestEvent != null) {
            val (sessionRequest, context) = WCDelegate.sessionRequestEvent!!
            SessionRequestUI.Content(
                peerUI = PeerUI(
                    peerName = sessionRequest.peerMetaData?.name ?: "",
                    peerIcon = sessionRequest.peerMetaData?.icons?.firstOrNull() ?: "",
                    peerUri = sessionRequest.peerMetaData?.url ?: "",
                    peerDescription = sessionRequest.peerMetaData?.description ?: "",
                    linkMode = sessionRequest.peerMetaData?.linkMode ?: false
                ),
                topic = sessionRequest.topic,
                requestId = sessionRequest.request.id,
                param = if (sessionRequest.request.method == Signer.PERSONAL_SIGN) extractMessageParamFromPersonalSign(sessionRequest.request.params) else sessionRequest.request.params,
                chain = sessionRequest.chainId,
                method = sessionRequest.request.method,
                peerContextUI = context.toPeerUI()
            )
        } else {
            SessionRequestUI.Initial
        }
    }
}