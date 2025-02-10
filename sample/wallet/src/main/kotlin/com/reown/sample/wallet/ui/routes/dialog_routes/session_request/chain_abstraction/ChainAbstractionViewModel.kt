package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.EthSigner
import com.reown.sample.wallet.domain.Signer
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.clearSessionRequest
import com.reown.sample.wallet.domain.execute
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.domain.recordError
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestUI
import com.reown.walletkit.client.ChainAbstractionExperimentalApi
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.web3j.utils.Numeric

data class TxSuccess(
    val redirect: Uri?,
    val hash: String
)

class ChainAbstractionViewModel : ViewModel() {
    var txHash: String? = null
    var errorMessage: String? = null
    var sessionRequestUI: SessionRequestUI = generateSessionRequestUI()

    @OptIn(ChainAbstractionExperimentalApi::class)
    fun getERC20Balance(): String {
        val initialTransaction = WCDelegate.sessionRequestEvent?.first

        val tokenAddress = WCDelegate.prepareAvailable?.initialTransactionMetadata?.tokenContract ?: ""
        return try {
            WalletKit.getERC20Balance(initialTransaction?.chainId ?: "", tokenAddress, EthAccountDelegate.account ?: "")
        } catch (e: Exception) {
            println("getERC20Balance error: $e")
            recordError(e)
            ""
        }
    }

    fun getTransferAmount(): String {
        return "${
            Transaction.hexToTokenAmount(
                WCDelegate.prepareAvailable?.initialTransactionMetadata?.amount ?: "",
                WCDelegate.prepareAvailable?.initialTransactionMetadata?.decimals ?: 6
            )?.toPlainString() ?: "-.--"
        } ${WCDelegate.prepareAvailable?.initialTransactionMetadata?.symbol}"
    }

    fun approve(onSuccess: (TxSuccess) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
                if (sessionRequest != null) {
                    val signedTransactions = mutableListOf<Pair<String, String>>()
                    val txHashesChannel = Channel<Pair<String, String>>()
                    //sign fulfilment txs
                    WCDelegate.prepareAvailable?.transactionsDetails?.details?.forEach { fulfilment ->
                        val signedTransaction = EthSigner.signHash(fulfilment.transactionHashToSign, EthAccountDelegate.privateKey)
                        signedTransactions.add(Pair(fulfilment.transaction.chainId, signedTransaction))
                    }

                    println("Original TX")
                    val initTransactionDetails = WCDelegate.prepareAvailable?.transactionsDetails!!.initialDetails
                    val signedTx = EthSigner.signHash(initTransactionDetails.transactionHashToSign, EthAccountDelegate.privateKey)
                    val result = async { execute(WCDelegate.prepareAvailable!!, signedTransactions.map { it.second }, signedTx) }.await()

                    result.fold(
                        onSuccess = { executeSuccess ->

                            val response = Wallet.Params.SessionRequestResponse(
                                sessionTopic = sessionRequest.topic,
                                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.requestId, executeSuccess.initialTxHash)
                            )

                            val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()
                            WalletKit.respondSessionRequest(response,
                                onSuccess = {
                                    clearSessionRequest()
                                    onSuccess(TxSuccess(redirect, executeSuccess.initialTxHash))
                                },
                                onError = { error ->
                                    recordError(error.throwable)
                                    if (error.throwable !is NoConnectivityException) {
                                        clearSessionRequest()
                                    }
                                    onError(error.throwable)
                                })
                        },
                        onFailure = {
                            println("Execution error: $it")
                            recordError(it)
                            onError(Throwable("Execution error: $it"))
                        }
                    )
//                //execute fulfilment txs
//                signedTransactions.forEach { (chainId, signedTx) ->
//                    viewModelScope.launch {
//                        try {
//                            println("Send raw: $signedTx")
//                            val txHash = Transaction.sendRaw(chainId, signedTx, "Route")
//                            println("Receive hash: $txHash")
//                            txHashesChannel.send(Pair(chainId, txHash))
//                        } catch (e: Exception) {
//                            println("Route broadcast tx error: $e")
//                            recordError(e)
//                            respondWithError(e.message ?: "Route TX broadcast error", WCDelegate.sessionRequestEvent?.first)
//                            return@launch onError(e)
//                        }
//                    }
//                }

//                //awaits receipts
//                viewModelScope.launch {
//                    repeat(signedTransactions.size) {
//                        val (chainId, txHash) = txHashesChannel.receive()
//                        println("Receive tx hash: $txHash")
//
//                        try {
//                            Transaction.getReceipt(chainId, txHash)
//                        } catch (e: Exception) {
//                            println("Route execution tx error: $e")
//                            recordError(e)
//                            respondWithError(e.message ?: "Route TX execution error", WCDelegate.sessionRequestEvent?.first)
//                            return@launch onError(e)
//                        }
//                    }
//                    txHashesChannel.close()


                    //check fulfilment status
//                    println("Fulfilment status check")
//                    val fulfilmentResult = async { status() }.await()
//                fulfilmentResult.fold(
//                    onSuccess = {
//                        when (it) {
//                            is Wallet.Model.Status.Error -> {
//                                println("Fulfilment error: ${it.reason}")
//                                onError(Throwable(it.reason))
//                            }
//
//                            is Wallet.Model.Status.Completed -> {
//                                println("Fulfilment completed")
//                                //if status completed, execute init tx
//                                with(WCDelegate.transactionsDetails!!.initialDetails.transaction) {
//                try {
//                                        val nonceResult = Transaction.getNonce(chainId, from)
//                                        println("Original TX")
//                                        val signedTx = Transaction.sign(this, nonceResult)
//                                        val resultTx = Transaction.sendRaw(chainId, signedTx, "Original")
//                                        val receipt = Transaction.getReceipt(chainId, resultTx)
//                                        println("Original TX receipt: $receipt")


//                } catch (e: Exception) {
//                    recordError(e)
//                    respondWithError(e.message ?: "Init TX execution error", WCDelegate.sessionRequestEvent?.first)
//                    return@launch onError(e)
//                }
                }
//                            }
//                        }
//                    },
//                    onFailure = {
//                        println("Fulfilment error: $it")
//                        recordError(it)
//                        onError(Throwable("Fulfilment status error: $it"))
//                    }
//                )
//            }
//        }
            } catch (e: Exception) {
                println("Error: $e")
                recordError(e)
                reject(message = e.message ?: "Undefined error, please check your Internet connection")
                clearSessionRequest()
                onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
            }
        }
    }

    fun reject(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}, message: String = "User rejected the request") {
        try {
            val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
            if (sessionRequest != null) {
                val result = Wallet.Params.SessionRequestResponse(
                    sessionTopic = sessionRequest.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = sessionRequest.requestId,
                        code = 500,
                        message = message
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