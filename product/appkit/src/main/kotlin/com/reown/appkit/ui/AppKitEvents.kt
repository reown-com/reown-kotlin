package com.reown.appkit.ui

sealed class AppKitEvents {
    object SessionApproved: AppKitEvents()
    object SessionRejected: AppKitEvents()
    object NoAction: AppKitEvents()
    object InvalidState: AppKitEvents()
}