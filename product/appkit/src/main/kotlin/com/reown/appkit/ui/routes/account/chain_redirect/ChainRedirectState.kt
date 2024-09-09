package com.reown.appkit.ui.routes.account.chain_redirect

internal sealed class ChainRedirectState {
    object Loading : ChainRedirectState()

    object Declined: ChainRedirectState()
}