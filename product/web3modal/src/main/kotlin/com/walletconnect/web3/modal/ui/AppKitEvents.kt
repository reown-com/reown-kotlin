package com.walletconnect.web3.modal.ui

sealed class AppKitEvents {
    object SessionApproved: AppKitEvents()
    object SessionRejected: AppKitEvents()
    object NoAction: AppKitEvents()
    object InvalidState: AppKitEvents()
}