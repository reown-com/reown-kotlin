package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.EthSigner
import com.reown.sample.wallet.domain.Signer
//import com.reown.sample.wallet.domain.SolanaAccountDelegate
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.WCDelegate.prepareAvailable
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
//import com.reown.walletkit.utils.solanaSignPrehash
import kotlinx.coroutines.async
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

        val tokenAddress = prepareAvailable?.initialTransactionMetadata?.tokenContract ?: ""
//        return try {
//            WalletKit.getERC20Balance(initialTransaction?.chainId ?: "", tokenAddress, EthAccountDelegate.address ?: "")
//        } catch (e: Exception) {
//            println("getERC20Balance error: $e")
//            recordError(e)
//            ""
//        }
       return ""
    }

    fun getTransferAmount(): String {
        return "${
            Transaction.hexToTokenAmount(
                prepareAvailable?.initialTransactionMetadata?.amount ?: "",
                prepareAvailable?.initialTransactionMetadata?.decimals ?: 6
            )?.toPlainString() ?: "-.--"
        } ${prepareAvailable?.initialTransactionMetadata?.symbol}"
    }

    fun approve(onSuccess: (TxSuccess) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
                if (sessionRequest != null) {
                    val signedFulfilmentTransactions = mutableListOf<Wallet.Model.RouteSig>()

                    //signing fulfilment txs
                    prepareAvailable?.transactionsDetails?.route?.forEach { route ->
                        when (route) {
                            is Wallet.Model.Route.Eip155 -> {
                                val eip155Signatures = mutableListOf<String>()
                                route.transactionDetails.forEach { transactionDetails ->
                                    val signedTransaction =
                                        EthSigner.signHash(transactionDetails.transactionHashToSign, EthAccountDelegate.privateKey)
                                    eip155Signatures.add(signedTransaction)

                                }
                                signedFulfilmentTransactions.add(Wallet.Model.RouteSig.Eip155(eip155Signatures))
                            }

                            is Wallet.Model.Route.Solana -> {
//                                val solanaSignatures = mutableListOf<String>()
//                                route.solanaTransactionDetails.forEach { transactionDetails ->
//                                    val signedTransaction = SolanaAccountDelegate.signHash(transactionDetails.transactionHashToSign)
//                                    solanaSignatures.add(signedTransaction)
//                                }
//                                signedFulfilmentTransactions.add(Wallet.Model.RouteSig.Solana(solanaSignatures))
                            }
                        }
                    }

                    //signing initial tx
                    val initTransactionDetails =
                        prepareAvailable?.transactionsDetails?.initialDetails ?: throw IllegalStateException("Initial transaction not found")
                    val signedInitialTx = EthSigner.signHash(initTransactionDetails.transactionHashToSign, EthAccountDelegate.privateKey)

                    //call execute method from WalletKit
                    val result = async { execute(prepareAvailable!!, signedFulfilmentTransactions, signedInitialTx) }.await()

                    result.fold(
                        onSuccess = { executeSuccess ->

                            if (sessionRequest.topic.isNotEmpty()) {
                                val response = Wallet.Params.SessionRequestResponse(
                                    sessionTopic = sessionRequest.topic,
                                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                                        sessionRequest.requestId,
                                        executeSuccess.initialTxHash
                                    )
                                )

                                val redirect = WalletKit.getActiveSessionByTopic(sessionRequest.topic)?.redirect?.toUri()
                                WalletKit.respondSessionRequest(
                                    response,
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
                            } else {
                                clearSessionRequest()
                                onSuccess(TxSuccess(null, executeSuccess.initialTxHash))
                            }
                        },
                        onFailure = {
                            println("Execution error: $it")
                            recordError(it)
                            onError(Throwable("Orchestrator ID: ${prepareAvailable?.orchestratorId}; Execution error: $it"))
                        }
                    )
                }
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
                WalletKit.respondSessionRequest(
                    result,
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