package com.walletconnect.web3.modal.ui

internal sealed class AppKitState {
    object Connect : AppKitState()

    object Loading : AppKitState()

    data class Error(val error: Throwable) : AppKitState()

    object AccountState : AppKitState()
}