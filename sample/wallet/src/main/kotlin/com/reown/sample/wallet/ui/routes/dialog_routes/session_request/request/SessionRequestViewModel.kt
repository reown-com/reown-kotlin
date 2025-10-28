package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.wallet.domain.Signer
import com.reown.sample.wallet.domain.Signer.PERSONAL_SIGN
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.web3j.utils.Numeric.hexStringToByteArray

class SessionRequestViewModel : ViewModel() {
    var sessionRequestUI: SessionRequestUI = generateSessionRequestUI()

    fun approve(onSuccess: (Uri?) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sessionRequest = sessionRequestUI as? SessionRequestUI.Content
                if (sessionRequest != null) {
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
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        return if (WalletKitDelegate.sessionRequestEvent != null) {
            val (sessionRequest, context) = WalletKitDelegate.sessionRequestEvent!!
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
                peerContextUI = context.toPeerUI()
            )
        } else {
            SessionRequestUI.Initial
        }
    }
}