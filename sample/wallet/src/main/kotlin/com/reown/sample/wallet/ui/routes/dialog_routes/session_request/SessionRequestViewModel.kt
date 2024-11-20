package com.reown.sample.wallet.ui.routes.dialog_routes.session_request

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.blockchain.JsonRpcRequest
import com.reown.sample.wallet.blockchain.createBlockChainApiService
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.Signer
import com.reown.sample.wallet.domain.Signer.PERSONAL_SIGN_METHOD
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import org.web3j.utils.Numeric.cleanHexPrefix
import org.web3j.utils.Numeric.hexStringToByteArray
import org.web3j.utils.Numeric.toBigInt
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SessionRequestViewModel : ViewModel() {
    var sessionRequestUI: SessionRequestUI = generateSessionRequestUI()

    fun approve(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
                if (sessionRequest != null) {
                    val signedTransactions = mutableListOf<Pair<String, String>>()

                    //sign fulfilment txs
                    WCDelegate.fulfilmentAvailable!!.transactions.forEach { transaction ->
                        val signedTransaction = Transaction.sign(transaction)
                        signedTransactions.add(Pair(transaction.chainId, signedTransaction))
                    }

                    //execute fulfilment txs
                    signedTransactions.forEach { (chainId, signedTx) ->
                        try {
                            Transaction.sendRaw(chainId, signedTx)
                        } catch (e: Exception) {
                            //todo: what should happen here when one of the route tx fails - stop executing and show the error?
                            println("kobe: tx error: $e")
                            return@launch onError(e)
                        }
                    }

                    //check fulfilment status
                    //TODO: refactor pooling with new bindings
                    delay(WCDelegate.fulfilmentAvailable!!.checkIn)
                    while (true) {
                        println("kobe: Fulfilment status check")
                        val fulfilmentResult = async { fulfillmentStatus() }.await()

                        when (fulfilmentResult) {
                            is Wallet.Model.FulfilmentStatus.Error -> {
                                println("kobe: Fulfilment error: ${fulfilmentResult.reason}")
                                onError(Throwable(fulfilmentResult.reason))
                                break
                            }

                            is Wallet.Model.FulfilmentStatus.Pending -> {
                                println("kobe: Fulfilment pending: ${fulfilmentResult.checkIn}")
                                delay(fulfilmentResult.checkIn)
                            }

                            is Wallet.Model.FulfilmentStatus.Completed -> {
                                println("kobe: Fulfilment completed")

                                //if status completed, execute init tx
                                with(WCDelegate.originalTransaction!!) {
                                    val nonceResult = Transaction.getNonce(chainId, from)
                                    println("kobe: Original TX")
                                    val signedTx = Transaction.sign(this, nonceResult, DefaultGasProvider.GAS_LIMIT)
                                    try {
                                        val resultTx = Transaction.sendRaw(chainId, signedTx)
                                        val response = Wallet.Params.SessionRequestResponse(
                                            sessionTopic = sessionRequest.topic,
                                            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.requestId, resultTx)
                                        )
                                        val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()
                                        WalletKit.respondSessionRequest(response,
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

                                    } catch (e: Exception) {
                                        return@launch onError(e)
                                    }
                                }
                                break //todo: clean up: return status, break, execute init tx
                            }
                        }
                    }
                } else {
                    onError(Throwable("Approve - Cannot find session request"))
                }
            } catch (e: Exception) {
                println("kobe: Error: $e")
                Firebase.crashlytics.recordException(e)
                reject()
                clearSessionRequest()
                onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
            }
        }
    }

    private suspend fun fulfillmentStatus(): Wallet.Model.FulfilmentStatus =
        suspendCoroutine { continuation ->
            try {
                WalletKit.fulfillmentStatus(WCDelegate.fulfilmentAvailable!!.fulfilmentId,
                    onSuccess = {
                        println("kobe: Fulfilment status SUCCESS: $it")
                        continuation.resume(it)
                    },
                    onError = {
                        println("kobe: Fulfilment status ERROR: $it")
                        continuation.resumeWithException(it.throwable)
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
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
                String(hexStringToByteArray(jsonArray.getString(0)))
            } else {
                jsonArray.getString(0)
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun clearSessionRequest() {
        WCDelegate.sessionRequestEvent = null
        WCDelegate.currentId = null
        sessionRequestUI = SessionRequestUI.Initial
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
                param = if (sessionRequest.request.method == PERSONAL_SIGN_METHOD) extractMessageParamFromPersonalSign(sessionRequest.request.params) else sessionRequest.request.params,
                chain = sessionRequest.chainId,
                method = sessionRequest.request.method,
                peerContextUI = context.toPeerUI()
            )
        } else {
            SessionRequestUI.Initial
        }
    }
}