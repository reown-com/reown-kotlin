package com.reown.sample.dapp.ui.routes.composable_routes.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.Session
import com.walletconnect.sample.common.Chains
import com.walletconnect.sample.common.tag
import com.reown.sample.dapp.domain.DappDelegate
import com.reown.sample.dapp.ui.DappSampleEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class SessionViewModel : ViewModel() {

    private val _sessionUI: MutableStateFlow<List<SessionUi>> = MutableStateFlow(getSessions())
    val uiState: StateFlow<List<SessionUi>> = _sessionUI.asStateFlow()

    private val _sessionEvents: MutableSharedFlow<DappSampleEvents> = MutableSharedFlow()
    val sessionEvent: SharedFlow<DappSampleEvents>
        get() = _sessionEvents.asSharedFlow()

    init {
        DappDelegate.wcEventModels
            .filterNotNull()
            .onEach { event ->
                when (event) {
                    is Modal.Model.UpdatedSession -> {
                        _sessionUI.value = getSessions(event.topic)
                    }

                    is Modal.Model.DeletedSession -> {
                        _sessionEvents.emit(DappSampleEvents.Disconnect)
                    }

                    else -> Unit
                }
            }.launchIn(viewModelScope)
    }

    private fun getSessions(topic: String? = null): List<SessionUi> {
        return (AppKit.getSession() as Session.WalletConnectSession).namespaces.values
            .flatMap { it.accounts }
            .map { caip10Account ->
                val (chainNamespace, chainReference, account) = caip10Account.split(":")
                val chain = Chains.values().first { chain ->
                    chain.chainNamespace == chainNamespace && chain.chainReference == chainReference
                }
                SessionUi(chain.icon, chain.name, account, chain.chainNamespace, chain.chainReference)
            }


    }

    fun ping() {
        viewModelScope.launch { _sessionEvents.emit(DappSampleEvents.PingLoading) }

        try {
            AppKit.ping(object : Modal.Listeners.SessionPing {
                override fun onSuccess(pingSuccess: Modal.Model.Ping.Success) {
                    viewModelScope.launch {
                        _sessionEvents.emit(DappSampleEvents.PingSuccess(pingSuccess.topic))
                    }
                }

                override fun onError(pingError: Modal.Model.Ping.Error) {
                    viewModelScope.launch {
                        _sessionEvents.emit(DappSampleEvents.PingError)
                    }
                }
            })
        } catch (e: Exception) {
            viewModelScope.launch {
                _sessionEvents.emit(DappSampleEvents.PingError)
            }
        }
    }

    fun disconnect() {
        if (DappDelegate.selectedSessionTopic != null) {
            try {
                viewModelScope.launch { _sessionEvents.emit(DappSampleEvents.DisconnectLoading) }
                AppKit.disconnect(
                    onSuccess = {
                        DappDelegate.deselectAccountDetails()
                        viewModelScope.launch {
                            _sessionEvents.emit(DappSampleEvents.Disconnect)
                        }
                    },
                    onError = { throwable: Throwable ->
                        Timber.tag(tag(this)).e(throwable.stackTraceToString())
                        Firebase.crashlytics.recordException(throwable)
                        viewModelScope.launch {
                            _sessionEvents.emit(DappSampleEvents.DisconnectError(throwable.message ?: "Unknown error, please try again or contact support"))
                        }
                    })

            } catch (e: Exception) {
                viewModelScope.launch {
                    _sessionEvents.emit(DappSampleEvents.DisconnectError(e.message ?: "Unknown error, please try again or contact support"))
                }
            }
        }
    }
}