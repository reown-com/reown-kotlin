package com.reown.appkit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.android.internal.common.wcKoinApp
import com.reown.appkit.client.AppKit
import com.reown.appkit.engine.AppKitEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AppKitViewModel : ViewModel() {

    private val appKitEngine: AppKitEngine = wcKoinApp.koin.get()
    private val _modalState: MutableStateFlow<AppKitState> = MutableStateFlow(AppKitState.Loading)
    val shouldDisconnect get() = appKitEngine.shouldDisconnect

    val modalState: StateFlow<AppKitState>
        get() = _modalState.asStateFlow()

    init {
        require(AppKit.chains.isNotEmpty()) { "Be sure to set the Chains using AppKit.setChains" }
        initModalState()
    }

    fun disconnect() {
        appKitEngine.disconnect(
            onSuccess = { println("Disconnected successfully") },
            onError = { println("Disconnect error: $it") }
        )
    }

    internal fun initModalState() {
        viewModelScope.launch {
            appKitEngine.getActiveSession()?.let { _ ->
                createAccountModalState()
            } ?: createConnectModalState()
        }
    }

    private fun createAccountModalState() {
        _modalState.value = AppKitState.AccountState
    }

    private fun createConnectModalState() {
        _modalState.value = AppKitState.Connect
    }
}
