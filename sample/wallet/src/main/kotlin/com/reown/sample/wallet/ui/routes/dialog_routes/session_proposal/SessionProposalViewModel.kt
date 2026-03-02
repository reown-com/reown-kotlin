package com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal

import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.cacao.signature.SignatureType
import com.reown.android.utils.cacao.sign
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.StacksAccountDelegate.wallet
import com.reown.android.BuildConfig as AndroidBuildConfig
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.domain.account.ACCOUNTS_1_EIP155_ADDRESS
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.domain.client.Stacks
import com.reown.sample.wallet.domain.client.SuiUtils
import com.reown.sample.wallet.domain.client.TONClient
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.toPeerUI
import com.reown.util.hexToBytes
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.reown.walletkit.utils.CacaoSigner
import timber.log.Timber
import uniffi.yttrium_utils.solanaSignPrehash
import uniffi.yttrium_utils.tronSignMessage

class SessionProposalViewModel : ViewModel() {
    val sessionProposal: SessionProposalUI? = generateSessionProposalUI()
    fun approve(
        proposalPublicKey: String,
        selectedChainIds: List<String> = emptyList(),
        onSuccess: (String) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val proposal = WalletKit.getSessionProposals().find { it.proposerPublicKey == proposalPublicKey }
        if (proposal != null) {
            try {
                Timber.d("Approving session proposal: $proposalPublicKey")
                val (sessionNamespaces, sessionProperties) = getNamespacesAndProperties(
                    proposal = proposal,
                    selectedChainIds = selectedChainIds
                )
                val scopedProperties = mapOf(
                    "eip155" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=${BuildConfig.PROJECT_ID}&st=wkca&sv=reown-kotlin-${AndroidBuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\"]}]}"
                )

                val authRequests = proposal.requests?.authentication ?: emptyList()
                val auths = mutableListOf<Wallet.Model.Cacao>()

                authRequests.forEach { authRequest ->
                    authRequest.chains.forEach { chainId ->
                        val signatureAndIssuer: Pair<Wallet.Model.Cacao.Signature, String> = when {
                            chainId.contains("eip155") -> {
                                val issuer = "did:pkh:$chainId:$ACCOUNTS_1_EIP155_ADDRESS"
                                val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))
                                Pair(CacaoSigner.sign(message, EthAccountDelegate.privateKey.hexToBytes(), SignatureType.EIP191), issuer)
                            }

                            chainId.contains("ton") -> {
                                val issuer = "did:pkh:$chainId:${TONAccountDelegate.addressFriendly}"
                                val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))
                                Pair(
                                    Wallet.Model.Cacao.Signature(
                                        t = "ton",
                                        s = TONClient.signData(message),
                                        m = android.util.Base64.encodeToString(
                                            TONAccountDelegate.publicKey.hexToBytes(),
                                            android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                        )
                                    ), issuer
                                )
                            }

                            chainId.contains("stacks") -> {
                                val issuer = "did:pkh:$chainId:${Stacks.getAddress(wallet, Stacks.Version.mainnetP2PKH)}"
                                val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))

                                Pair(
                                    Wallet.Model.Cacao.Signature(
                                        t = "stacks",
                                        s = Stacks.signMessage(wallet, message)
                                    ), issuer
                                )
                            }

                            chainId.contains("sui") -> {
                                val issuer = "did:pkh:$chainId:${SuiAccountDelegate.address}"
                                val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))
                                Pair(
                                    Wallet.Model.Cacao.Signature(
                                        t = "sui",
                                        s = SuiUtils.personalSign(SuiAccountDelegate.keypair, message.toByteArray())
                                    ), issuer
                                )
                            }

                            chainId.contains("solana") -> {
                                val issuer = "did:pkh:$chainId:${SolanaAccountDelegate.keys.second}"
                                val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))
                                Pair(
                                    Wallet.Model.Cacao.Signature(
                                        t = "solana",
                                        s = solanaSignPrehash(SolanaAccountDelegate.keyPair, message)
                                    ), issuer
                                )
                            }

                            chainId.contains("tron") -> {
                                val issuer = "did:pkh:$chainId:${TronAccountDelegate.address}"
                                val message = WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))
                                Pair(
                                    Wallet.Model.Cacao.Signature(
                                        t = "tron",
                                        s = tronSignMessage(message, TronAccountDelegate.keypair)
                                    ), issuer
                                )
                            }

                            else -> Pair(
                                Wallet.Model.Cacao.Signature(
                                    t = "",
                                    s = ""
                                ), ""
                            )
                        }


                        val auth = WalletKit.generateAuthObject(authRequest, signatureAndIssuer.second, signatureAndIssuer.first)
                        auths.add(auth)
                    }
                }

                val approveProposal = Wallet.Params.SessionApprove(
                    proposerPublicKey = proposal.proposerPublicKey,
                    namespaces = sessionNamespaces,
                    properties = sessionProperties,
                    scopedProperties = scopedProperties,
                    proposalRequestsResponses = Wallet.Model.ProposalRequestsResponses(authentication = auths)
                )

                WalletKit.approveSession(
                    approveProposal,
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        WalletKitDelegate.sessionProposalEvent = null
                        onError(error.throwable)
                    },
                    onSuccess = {
                        WalletKitDelegate.sessionProposalEvent = null
                        onSuccess(proposal.redirect)
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WalletKitDelegate.sessionProposalEvent = null
                onError(e)
            }
        } else {
            onError(Throwable("Cannot approve session proposal, it has expired. Please try again."))
        }
    }

    private fun getNamespacesAndProperties(
        proposal: Wallet.Model.SessionProposal,
        selectedChainIds: List<String>
    ): Pair<Map<String, Wallet.Model.Namespace.Session>, Map<String, String>> {
        val filteredSupportedNamespaces = filterSupportedNamespaces(selectedChainIds)
        val sessionNamespaces = WalletKit.generateApprovedNamespaces(
            sessionProposal = proposal,
            supportedNamespaces = filteredSupportedNamespaces
        )
        val sessionProperties = mapOf(
            "tron_method_version" to "v1",
            "ton_getPublicKey" to TONAccountDelegate.publicKey,
            "ton_getStateInit" to TONClient.getStateInitBoc(TONAccountDelegate.keypair)
        )
        return Pair(sessionNamespaces, sessionProperties)
    }

    private fun filterSupportedNamespaces(selectedChainIds: List<String>): Map<String, Wallet.Model.Namespace.Session> {
        if (selectedChainIds.isEmpty()) return walletMetaData.namespaces

        val selectedChainIdSet = selectedChainIds.toSet()
        return walletMetaData.namespaces.mapNotNull { (namespaceKey, namespaceSession) ->
            val filteredChains = namespaceSession.chains?.filter { chainId ->
                chainId in selectedChainIdSet
            }
            val filteredAccounts = namespaceSession.accounts.filter { accountId ->
                val accountParts = accountId.split(":")
                if (accountParts.size < 2) {
                    false
                } else {
                    "${accountParts[0]}:${accountParts[1]}" in selectedChainIdSet
                }
            }

            val hasSelectedChains = !filteredChains.isNullOrEmpty()
            val hasSelectedAccounts = filteredAccounts.isNotEmpty()
            if (!hasSelectedChains && !hasSelectedAccounts) return@mapNotNull null

            namespaceKey to Wallet.Model.Namespace.Session(
                chains = filteredChains,
                methods = namespaceSession.methods,
                events = namespaceSession.events,
                accounts = filteredAccounts
            )
        }.toMap()
    }

    private fun collectAuthMessages(proposal: Wallet.Model.SessionProposal): List<String> {
        val authRequests = proposal.requests?.authentication ?: return emptyList()
        val messages = mutableListOf<String>()

        authRequests.forEach { authRequest ->
            authRequest.chains.forEach { chainId ->
                val issuer = "did:pkh:$chainId:$ACCOUNTS_1_EIP155_ADDRESS"
                val message = runCatching {
                    WalletKit.formatAuthMessage(Wallet.Params.FormatAuthMessage(authRequest, issuer))
                }.onFailure { error ->
                    Firebase.crashlytics.recordException(error)
                }.getOrNull()

                if (message != null) {
                    messages.add(message)
                }
            }
        }

        return messages
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

                WalletKit.rejectSession(
                    reject,
                    onSuccess = {
                        WalletKitDelegate.sessionProposalEvent = null
                        onSuccess(proposal.redirect)
                    },
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        WalletKitDelegate.sessionProposalEvent = null
                        onError(error.throwable)
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WalletKitDelegate.sessionProposalEvent = null
                onError(e)
            }
        } else {
            onError(Throwable("Cannot reject session proposal, it has expired. Please try again."))
        }
    }

    private fun generateSessionProposalUI(): SessionProposalUI? {
        return if (WalletKitDelegate.sessionProposalEvent != null) {
            val (proposal, context) = WalletKitDelegate.sessionProposalEvent!!
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
                messagesToSign = collectAuthMessages(proposal),
                pubKey = proposal.proposerPublicKey
            )
        } else null
    }
}
