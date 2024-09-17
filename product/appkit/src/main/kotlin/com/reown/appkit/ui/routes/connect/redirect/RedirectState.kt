package com.reown.appkit.ui.routes.connect.redirect

sealed class RedirectState {
    object Loading: RedirectState()
    object Reject: RedirectState()
    object Expired: RedirectState()
    object NotDetected: RedirectState()
}
