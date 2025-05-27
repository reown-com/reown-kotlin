package com.reown.sample.wallet.ui.routes.dialog_routes.session_authenticate

import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.android.utils.cacao.sign
import com.reown.sample.wallet.domain.account.ACCOUNTS_1_EIP155_ADDRESS
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.util.hexToBytes
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.reown.walletkit.utils.CacaoSigner

class SessionAuthenticateViewModel : ViewModel() {
    val sessionAuthenticateUI: SessionAuthenticateUI? get() = generateAuthRequestUI()

    fun approve(onSuccess: (String) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        if (WCDelegate.sessionAuthenticateEvent != null) {
            try {
                val sessionAuthenticate = WCDelegate.sessionAuthenticateEvent!!.first
                val auths = mutableListOf<Wallet.Model.Cacao>()

                val authPayloadParams =
                    WalletKit.generateAuthPayloadParams(
                        sessionAuthenticate.payloadParams,
                        supportedChains = listOf("eip155:1", "eip155:137", "eip155:56"),
                        supportedMethods = listOf("personal_sign", "eth_signTypedData", "eth_signTypedData_v4", "eth_sign")
                    )

                authPayloadParams.chains
                    .forEach { chain ->
                        val issuer = "did:pkh:$chain:$ACCOUNTS_1_EIP155_ADDRESS"
                        val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authPayloadParams, issuer))
                        val signature = CacaoSigner.sign(message, EthAccountDelegate.privateKey.hexToBytes(), SignatureType.EIP191)
                        val auth = WalletKit.generateAuthObject(authPayloadParams, issuer, signature)
                        auths.add(auth)
                    }

                val approveProposal = Wallet.Params.ApproveSessionAuthenticate(id = sessionAuthenticate.id, auths = auths)
                WalletKit.approveSessionAuthenticate(approveProposal,
                    onSuccess = {
                        WCDelegate.sessionAuthenticateEvent = null
                        onSuccess(sessionAuthenticate.participant.metadata?.redirect ?: "")
                    },
                    onError = { error ->
                        if (error.throwable !is NoConnectivityException) {
                            WCDelegate.sessionAuthenticateEvent = null
                        }
                        Firebase.crashlytics.recordException(error.throwable)
                        onError(error.throwable)
                    }
                )
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WCDelegate.sessionAuthenticateEvent = null
                onError(e)
            }
        } else {
            onError(Throwable("Authenticate request expired"))
        }
    }

    fun reject(onSuccess: (String) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        if (WCDelegate.sessionAuthenticateEvent != null) {
            try {
                val sessionAuthenticate = WCDelegate.sessionAuthenticateEvent!!.first
                val rejectionReason = "Reject Session Authenticate"
                val reject = Wallet.Params.RejectSessionAuthenticate(
                    id = sessionAuthenticate.id,
                    reason = rejectionReason
                )

                WalletKit.rejectSessionAuthenticate(reject,
                    onSuccess = {
                        WCDelegate.sessionAuthenticateEvent = null
                        onSuccess(sessionAuthenticate.participant.metadata?.redirect ?: "")
                    },
                    onError = { error ->
                        if (error.throwable !is NoConnectivityException) {
                            WCDelegate.sessionAuthenticateEvent = null
                        }
                        Firebase.crashlytics.recordException(error.throwable)
                        onError(error.throwable)
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WCDelegate.sessionAuthenticateEvent = null
                onError(e)
            }
        } else {
            onError(Throwable("Authenticate request expired"))
        }
    }

    private fun generateAuthRequestUI(): SessionAuthenticateUI? {
        return if (WCDelegate.sessionAuthenticateEvent != null) {
            val (sessionAuthenticate, authContext) = WCDelegate.sessionAuthenticateEvent!!
            val messages = mutableListOf<String>()
            sessionAuthenticate.payloadParams.chains
                .forEach { chain ->
                    val issuer = "did:pkh:$chain:$ACCOUNTS_1_EIP155_ADDRESS"
                    val message = try {
                        WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(sessionAuthenticate.payloadParams, issuer))
                    } catch (e: Exception) {
                        "Invalid message, error: ${e.message}"
                    }
                    messages.add(message)
                }

            SessionAuthenticateUI(
                peerUI = PeerUI(
                    peerIcon = sessionAuthenticate.participant.metadata?.icons?.firstOrNull().toString(),
                    peerName = sessionAuthenticate.participant.metadata?.name ?: "WalletConnect",
                    peerUri = sessionAuthenticate.participant.metadata?.url ?: "https://walletconnect.com/",
                    peerDescription = sessionAuthenticate.participant.metadata?.url ?: "The communications protocol for web3.",
                    linkMode = sessionAuthenticate.participant.metadata?.linkMode ?: false
                ),
                messages = messages,
                peerContextUI = authContext.toPeerUI()
            )
        } else null
    }
}