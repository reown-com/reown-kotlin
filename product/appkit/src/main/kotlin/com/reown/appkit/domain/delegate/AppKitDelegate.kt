package com.reown.appkit.domain.delegate

import com.reown.android.internal.common.wcKoinApp
import com.reown.appkit.client.Modal
import com.reown.appkit.client.AppKit
import com.reown.appkit.domain.model.Session
import com.reown.appkit.domain.usecase.DeleteSessionDataUseCase
import com.reown.appkit.domain.usecase.SaveChainSelectionUseCase
import com.reown.appkit.domain.usecase.SaveSessionUseCase
import com.reown.appkit.utils.EthUtils
import com.reown.appkit.utils.getAddress
import com.reown.appkit.utils.getSelectedChain
import com.reown.appkit.utils.toSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal object AppKitDelegate : AppKit.ModalDelegate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _wcEventModels: MutableSharedFlow<Modal.Model?> = MutableSharedFlow()
    val wcEventModels: SharedFlow<Modal.Model?> = _wcEventModels.asSharedFlow()

    private val _connectionState: MutableSharedFlow<Modal.Model.ConnectionState> = MutableSharedFlow(replay = 1)
    val connectionState: SharedFlow<Modal.Model.ConnectionState> = _connectionState.asSharedFlow()

    //todo replace it with engine
    private val saveSessionUseCase: SaveSessionUseCase by lazy { wcKoinApp.koin.get() }
    private val saveChainSelectionUseCase: SaveChainSelectionUseCase by lazy { wcKoinApp.koin.get() }
    private val deleteSessionDataUseCase: DeleteSessionDataUseCase by lazy { wcKoinApp.koin.get() }

    fun emit(event: Modal.Model?) {
        scope.launch {
            _wcEventModels.emit(event)
        }
    }

    override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
    scope.launch {
        val chain = AppKit.chains.getSelectedChain(AppKit.selectedChain?.id)
        if (chain != null) {
            saveSessionUseCase(approvedSession.toSession(chain))
            _wcEventModels.emit(approvedSession)
        } else {
            Log.e("AppKitDelegate", "Cannot process session - no chain available")
        }
    }
}

    override fun onSessionAuthenticateResponse(sessionAuthenticateResponse: Modal.Model.SessionAuthenticateResponse) {
        scope.launch {
            if (sessionAuthenticateResponse is Modal.Model.SessionAuthenticateResponse.Result) {
                val chain = AppKit.chains.getSelectedChain(AppKit.selectedChain?.id)
                saveSessionUseCase(
                    Session.WalletConnect(
                        chain = chain.id,
                        topic = sessionAuthenticateResponse.session?.topic ?: "",
                        address = sessionAuthenticateResponse.session?.getAddress(chain) ?: ""
                    )
                )
            }

            _wcEventModels.emit(sessionAuthenticateResponse)
        }
    }

    override fun onSIWEAuthenticationResponse(response: Modal.Model.SIWEAuthenticateResponse) {
        scope.launch {
            _wcEventModels.emit(response)
        }
    }

    override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) {
        scope.launch {
            _wcEventModels.emit(rejectedSession)
        }
    }

    override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) {
        scope.launch {
            val chain = AppKit.chains.getSelectedChain(AppKit.selectedChain?.id)
            saveSessionUseCase(updatedSession.toSession(chain))
            _wcEventModels.emit(updatedSession)
        }
    }

    override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) {
        scope.launch {
            consumeSessionEvent(sessionEvent)
            _wcEventModels.emit(sessionEvent)
        }
    }

    override fun onSessionEvent(sessionEvent: Modal.Model.Event) {
        scope.launch {
            consumeEvent(sessionEvent)
            _wcEventModels.emit(sessionEvent)
        }
    }

    private suspend fun consumeSessionEvent(sessionEvent: Modal.Model.SessionEvent) {
        try {
            when (sessionEvent.name) {
                EthUtils.accountsChanged -> {
                    val (_, chainReference, _) = sessionEvent.data.split(":")
                    AppKit.chains.find { it.chainReference == chainReference }?.let { chain -> saveChainSelectionUseCase(chain.id) }
                }

                EthUtils.chainChanged -> {
                    val (chainReference, _) = sessionEvent.data.split(".")
                    AppKit.chains.find { it.chainReference == chainReference }?.let { chain -> saveChainSelectionUseCase(chain.id) }
                }
            }
        } catch (throwable: Throwable) {
            onError(Modal.Model.Error(throwable))
        }
    }

    //todo: Do we need to change anything here? We have more data in event now.
    private suspend fun consumeEvent(event: Modal.Model.Event) {
        try {
            when (event.name) {
                EthUtils.accountsChanged -> {
                    //todo: Can we take chainReference from the event?
                    val (_, chainReference, _) = event.data.split(":")
                    AppKit.chains.find { it.chainReference == chainReference }?.let { chain -> saveChainSelectionUseCase(chain.id) }
                }

                EthUtils.chainChanged -> {
                    //todo: Can we take chainReference from the event?
                    val (chainReference, _) = event.data.split(".")
                    AppKit.chains.find { it.chainReference == chainReference }?.let { chain -> saveChainSelectionUseCase(chain.id) }
                }
            }
        } catch (throwable: Throwable) {
            onError(Modal.Model.Error(throwable))
        }
    }

    override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {
        scope.launch {
            deleteSessionDataUseCase()
            _wcEventModels.emit(deletedSession)
        }
    }

    override fun onSessionExtend(session: Modal.Model.Session) {
        scope.launch {
            _wcEventModels.emit(session)
        }
    }

    override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) {
        scope.launch {
            _wcEventModels.emit(response)
        }
    }

    override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) {
        scope.launch {
            _wcEventModels.emit(proposal)
        }
    }

    override fun onRequestExpired(request: Modal.Model.ExpiredRequest) {
        scope.launch {
            _wcEventModels.emit(request)
        }
    }

    override fun onConnectionStateChange(state: Modal.Model.ConnectionState) {
        scope.launch {
            _connectionState.emit(state)
        }
    }

    override fun onError(error: Modal.Model.Error) {
        scope.launch {
            _wcEventModels.emit(error)
        }
    }
}
