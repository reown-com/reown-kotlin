package com.reown.sample.dapp.ui.routes.composable_routes.chain_selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sample.common.Chains
import com.reown.sample.common.tag
import com.reown.sample.dapp.domain.DappDelegate
import com.reown.sample.dapp.ui.DappSampleEvents
import com.reown.util.bytesToHex
import com.reown.util.randomBytes
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ChainSelectionViewModel : ViewModel() {
    private val _awaitingProposalSharedFlow: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val awaitingSharedFlow = _awaitingProposalSharedFlow.asSharedFlow()

    private val chains: List<ChainSelectionUi> =
        Chains.values().map { it.toChainUiState() }

    private val _uiState = MutableStateFlow(chains)
    val uiState = _uiState.asStateFlow()

    val isAnyChainSelected: Boolean
        get() = uiState.value.any { it.isSelected }

    val walletEvents = DappDelegate.wcEventModels.map { walletEvent: Modal.Model? ->
        when (walletEvent) {
            is Modal.Model.ApprovedSession -> DappSampleEvents.SessionApproved
            is Modal.Model.RejectedSession -> DappSampleEvents.SessionRejected
            is Modal.Model.SessionAuthenticateResponse -> {
                if (walletEvent is Modal.Model.SessionAuthenticateResponse.Result) {
                    DappSampleEvents.SessionAuthenticateApproved(if (walletEvent.session == null) "Authenticated successfully!" else null)
                } else {
                    DappSampleEvents.SessionAuthenticateRejected
                }
            }

            is Modal.Model.ExpiredProposal -> DappSampleEvents.ProposalExpired
            else -> DappSampleEvents.NoAction
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    fun awaitingProposalResponse(isAwaiting: Boolean) {
        viewModelScope.launch {
            _awaitingProposalSharedFlow.emit(isAwaiting)
        }
    }

    fun updateChainSelectState(position: Int, selected: Boolean) {
        _uiState.update {
            it.toMutableList().apply {
                this[position] = it[position].copy(isSelected = !selected)
            }
        }
    }

    fun authenticate(authenticateParams: Modal.Params.Authenticate, appLink: String = "", onAuthenticateSuccess: (String?) -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _awaitingProposalSharedFlow.emit(true)
        }

        AppKit.authenticate(authenticateParams, walletAppLink = appLink,
            onSuccess = { url ->
                viewModelScope.launch {
                    _awaitingProposalSharedFlow.emit(false)
                }
                onAuthenticateSuccess(url)
            },
            onError = { error ->
                viewModelScope.launch {
                    _awaitingProposalSharedFlow.emit(false)
                }
                Timber.tag(tag(this)).e(error.throwable.stackTraceToString())
                Firebase.crashlytics.recordException(error.throwable)
                onError(error.throwable.message ?: "Unknown error, please contact support")
            })
    }

    fun connectToWallet(pairingTopicPosition: Int = -1, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _awaitingProposalSharedFlow.emit(true)
        }
        try {
            val pairing: Core.Model.Pairing? = if (pairingTopicPosition > -1) {
                CoreClient.Pairing.getPairings()[pairingTopicPosition]
            } else {
                CoreClient.Pairing.create { error ->
                    viewModelScope.launch {
                        _awaitingProposalSharedFlow.emit(false)
                    }
                    onError("Creating Pairing failed: ${error.throwable.stackTraceToString()}")
                }
            }

            if (pairing != null) {
                val connectParams =
                    Modal.Params.ConnectParams(
                        sessionNamespaces = getOptionalNamespaces(),
                        properties = getProperties(),
                        pairing = pairing
                    )

                AppKit.connect(connectParams,
                    onSuccess = { url ->
                        if (pairingTopicPosition == -1) {
                            viewModelScope.launch {
                                _awaitingProposalSharedFlow.emit(false)
                            }
                        }
                        onSuccess(url)
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            _awaitingProposalSharedFlow.emit(false)
                        }
                        Timber.tag(tag(this)).e(error.throwable.stackTraceToString())
                        Firebase.crashlytics.recordException(error.throwable)
                        onError(error.throwable.message ?: "Unknown error, please contact support")
                    }
                )
            }

        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            Timber.tag(tag(this)).e(e)
            onError(e.message ?: "Unknown error, please contact support")
        }
    }

    private fun getNamespaces(): Map<String, Modal.Model.Namespace.Proposal> {
        val namespaces: Map<String, Modal.Model.Namespace.Proposal> =
            uiState.value
                .filter { it.isSelected && it.chainId != Chains.POLYGON_MATIC.chainId && it.chainId != Chains.ETHEREUM_KOVAN.chainId }
                .groupBy { it.chainNamespace }
                .map { (key: String, selectedChains: List<ChainSelectionUi>) ->
                    key to Modal.Model.Namespace.Proposal(
                        chains = selectedChains.map { it.chainId }, //OR uncomment if chainId is an index
                        methods = selectedChains.flatMap { it.methods }.distinct(),
                        events = selectedChains.flatMap { it.events }.distinct()
                    )
                }.toMap()

        val tmp = uiState.value
            .filter { it.isSelected && it.chainId == Chains.ETHEREUM_KOVAN.chainId }
            .groupBy { it.chainId }
            .map { (key: String, selectedChains: List<ChainSelectionUi>) ->
                key to Modal.Model.Namespace.Proposal(
                    methods = selectedChains.flatMap { it.methods }.distinct(),
                    events = selectedChains.flatMap { it.events }.distinct()
                )
            }.toMap()

        return namespaces.toMutableMap().plus(tmp)
    }

    private fun getOptionalNamespaces() = uiState.value
        .filter { it.isSelected && it.chainId == Chains.POLYGON_MATIC.chainId }
        .groupBy { it.chainId }
        .map { (key: String, selectedChains: List<ChainSelectionUi>) ->
            key to Modal.Model.Namespace.Proposal(
                methods = selectedChains.flatMap { it.methods }.distinct(),
                events = selectedChains.flatMap { it.events }.distinct()
            )
        }.toMap()

    private fun getProperties(): Map<String, String> {
        //note: this property is not used in the SDK, only for demonstration purposes
        val expiry = (System.currentTimeMillis() / 1000) + TimeUnit.SECONDS.convert(7, TimeUnit.DAYS)
        return mapOf("sessionExpiry" to "$expiry")
    }


    val authenticateParams
        get() = Modal.Params.Authenticate(
            chains = uiState.value.filter { it.isSelected }.map { it.chainId },
            domain = "sample.kotlin.dapp",
            uri = "https://web3inbox.com/all-apps",
            nonce = randomBytes(12).bytesToHex(),
            exp = null,
            nbf = null,
            statement = "Sign in with wallet.",
            requestId = null,
            resources = listOf(
                "urn:recap:eyJhdHQiOnsiaHR0cHM6Ly9ub3RpZnkud2FsbGV0Y29ubmVjdC5jb20vYWxsLWFwcHMiOnsiY3J1ZC9zdWJzY3JpcHRpb25zIjpbe31dLCJjcnVkL25vdGlmaWNhdGlvbnMiOlt7fV19fX0=",
                "ipfs://bafybeiemxf5abjwjbikoz4mc3a3dla6ual3jsgpdr4cjr3oz3evfyavhwq/"
            ),
            methods = listOf("personal_sign", "eth_signTypedData"),
            expiry = null
        )

    val siweParams
        get() = Modal.Params.Authenticate(
            chains = uiState.value.filter { it.isSelected }.map { it.chainId },
            domain = "sample.kotlin.dapp",
            uri = "https://web3inbox.com/all-apps",
            nonce = randomBytes(12).bytesToHex(),
            exp = null,
            nbf = null,
            statement = "Sign in with wallet.",
            requestId = null,
            resources = listOf(),
            methods = null,
            expiry = null
        )
}