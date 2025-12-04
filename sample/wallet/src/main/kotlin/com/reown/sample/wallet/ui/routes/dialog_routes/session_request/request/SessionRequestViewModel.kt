package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.payment.PaymentAuthorization
import com.reown.sample.wallet.domain.payment.PaymentRepository
import com.reown.sample.wallet.domain.payment.PaymentSigner
import com.reown.sample.wallet.domain.signer.Signer
import com.reown.sample.wallet.domain.signer.Signer.PERSONAL_SIGN
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.web3j.utils.Numeric.hexStringToByteArray

class SessionRequestViewModel : ViewModel() {
    var sessionRequestUI: SessionRequestUI = generateSessionRequestUI()

    fun approve(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
                if (sessionRequest != null) {
                    if (sessionRequest.paymentData != null) {
                        Log.d("SessionRequestVM", "Approving payment request id=${sessionRequest.paymentData.paymentId}")
                        approvePayment(sessionRequest)
                        onSuccess(null)
                        return@launch
                    }
                    val result: String = Signer.sign(sessionRequest)

                    val response = Wallet.Params.SessionRequestResponse(
                        sessionTopic = sessionRequest.topic,
                        jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.requestId, result)
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
                } else {
                    onError(Throwable("Approve - Cannot find session request"))
                }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            val isPayment = (sessionRequestUI as? SessionRequestUI.Content)?.paymentData != null
            if (isPayment) {
                clearSessionRequest()
                Log.e("SessionRequestVM", "Payment approval failed: ${e.message}", e)
                onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
            } else {
                reject(message = e.message ?: "Undefined error, please check your Internet connection")
                clearSessionRequest()
                onError(Throwable(e.message ?: "Undefined error, please check your Internet connection"))
            }
        }
    }
    }

    fun reject(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}, message: String = "User rejected the request") {
        try {
            val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
            if (sessionRequest != null) {
                if (sessionRequest.paymentData != null) {
                    clearSessionRequest()
                    PaymentRepository.clearPayment()
                    onSuccess(null)
                    return
                }
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
                String(hexStringToByteArray(jsonArray.getString(0)))
            } else {
                jsonArray.getString(0)
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun clearSessionRequest() {
        WalletKitDelegate.sessionRequestEvent = null
        WalletKitDelegate.currentId = null
        sessionRequestUI = SessionRequestUI.Initial
        PaymentRepository.clearPayment()
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        return if (WalletKitDelegate.sessionRequestEvent != null) {
            val (sessionRequest, context) = WalletKitDelegate.sessionRequestEvent!!
            val isPaymentRequest = sessionRequest.topic.startsWith("payment-") && PaymentRepository.currentPayment != null
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
                param = if (sessionRequest.request.method == PERSONAL_SIGN) extractMessageParamFromPersonalSign(sessionRequest.request.params) else sessionRequest.request.params,
                chain = sessionRequest.chainId,
                method = sessionRequest.request.method,
                peerContextUI = context.toPeerUI(),
                paymentData = if (isPaymentRequest) PaymentRepository.currentPayment else null
            )
        } else {
            SessionRequestUI.Initial
        }
    }

    private suspend fun approvePayment(sessionRequest: SessionRequestUI.Content) {
        val paymentSession = sessionRequest.paymentData ?: throw IllegalStateException("Missing payment data")
        val typedDataJson = paymentSession.typedDataJson ?: throw IllegalStateException("Missing typed data for signing")
        val typedData = paymentSession.typedData ?: throw IllegalStateException("Missing parsed typed data")
        
        Log.d("SessionRequestVM", "Approving payment for ${paymentSession.selectedOption.symbol} on ${paymentSession.selectedOption.chain}")
        
        val signedTypedData = PaymentSigner.signTypedDataV4(typedDataJson)
        val message = typedData.message
        Log.d("SessionRequestVM", "Signed payment r=${signedTypedData.r} s=${signedTypedData.s} v=${signedTypedData.v}")

        val authorization = PaymentAuthorization(
            from = message.from,
            to = message.to,
            value = message.value,
            validAfter = message.validAfter,
            validBefore = message.validBefore,
            nonce = message.nonce,
            v = signedTypedData.v,
            r = signedTypedData.r,
            s = signedTypedData.s
        )
        
        val submitResponse = withContext(Dispatchers.IO) {
            PaymentRepository.submitPayment(
                paymentId = paymentSession.paymentId,
                authorization = authorization,
                asset = paymentSession.selectedOption.asset
            )
        }
        
        if (submitResponse.status.equals("failed", true)) {
            val errorMessage = submitResponse.error ?: "Payment failed to submit"
            throw IllegalStateException(errorMessage)
        }
        
        Log.d("SessionRequestVM", "Payment submitted status=${submitResponse.status} txHash=${submitResponse.txHash} chain=${submitResponse.chainName}")
        clearSessionRequest()
    }
}
