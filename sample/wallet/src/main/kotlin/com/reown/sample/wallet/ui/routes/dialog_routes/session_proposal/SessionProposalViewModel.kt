package com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal

import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import timber.log.Timber

class SessionProposalViewModel : ViewModel() {
    val sessionProposal: SessionProposalUI? = generateSessionProposalUI()
    fun approve(proposalPublicKey: String, onSuccess: (String) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        val proposal = WalletKit.getSessionProposals().find { it.proposerPublicKey == proposalPublicKey }
        if (proposal != null) {
            try {
                Timber.d("Approving session proposal: $proposalPublicKey")
                val sessionNamespaces = WalletKit.generateApprovedNamespaces(sessionProposal = proposal, supportedNamespaces = walletMetaData.namespaces)
                val approveProposal = Wallet.Params.SessionApprove(proposerPublicKey = proposal.proposerPublicKey, namespaces = sessionNamespaces)

                WalletKit.approveSession(approveProposal,
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        WCDelegate.sessionProposalEvent = null
                        onError(error.throwable)
                    },
                    onSuccess = {
                        WCDelegate.sessionProposalEvent = null
                        onSuccess(proposal.redirect)
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WCDelegate.sessionProposalEvent = null
                onError(e)
            }
        } else {
            onError(Throwable("Cannot approve session proposal, it has expired. Please try again."))
        }
    }

    fun reject(proposalPublicKey: String, onSuccess: (String) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        val proposal = WalletKit.getSessionProposals().find { it.proposerPublicKey == proposalPublicKey }
        if (proposal != null) {
            try {
                val rejectionReason = "Reject Session"
                val reject = Wallet.Params.SessionReject(
                    proposerPublicKey = proposal.proposerPublicKey,
                    reason = rejectionReason
                )

                WalletKit.rejectSession(reject,
                    onSuccess = {
                        WCDelegate.sessionProposalEvent = null
                        onSuccess(proposal.redirect)
                    },
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        WCDelegate.sessionProposalEvent = null
                        onError(error.throwable)
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WCDelegate.sessionProposalEvent = null
                onError(e)
            }
        } else {
            onError(Throwable("Cannot reject session proposal, it has expired. Please try again."))
        }
    }

    private fun generateSessionProposalUI(): SessionProposalUI? {
        return if (WCDelegate.sessionProposalEvent != null) {
            val (proposal, context) = WCDelegate.sessionProposalEvent!!
            SessionProposalUI(
                peerUI = PeerUI(
                    peerIcon = proposal.icons.firstOrNull().toString(),
                    peerName = proposal.name,
                    peerDescription = proposal.description,
                    peerUri = proposal.url,
                ),
                namespaces = proposal.requiredNamespaces,
                optionalNamespaces = proposal.optionalNamespaces,
                peerContext = context.toPeerUI(),
                redirect = proposal.redirect,
                pubKey = proposal.proposerPublicKey
            )
        } else null
    }
}