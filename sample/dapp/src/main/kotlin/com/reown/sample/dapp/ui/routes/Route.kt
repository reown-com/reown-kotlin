package com.reown.sample.dapp.ui.routes

sealed class Route(val path: String) {
    object ChainSelection : Route("chain_selection")
    object Session : Route("session")
    object Account : Route("account")
}
