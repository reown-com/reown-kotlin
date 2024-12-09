package com.reown.sample.wallet.ui.routes

import androidx.navigation.NavController

sealed class Route(val path: String) {
    object GetStarted : Route("get_started")
    object Connections : Route("connections")
    object SessionProposal : Route("session_proposal")
    object SessionRequest : Route("session_request")
    object SessionAuthenticate : Route("session_authenticate")
    object ChainAbstraction : Route("chain_abstraction")
    object PasteUri : Route("paste_uri")
    object ScanUri : Route("scan_uri")

    object ConnectionDetails : Route("connection_details")
    object SnackbarMessage : Route("snackbar_message")
    object ExploreDapps : Route("explore_dapps")
    object Inbox : Route("inbox")
    object Notifications : Route("notifications")
    object UpdateSubscription : Route("update_subscription")
    object Settings : Route("settings")
}

fun NavController.showSnackbar(message: String) {
    navigate("${Route.SnackbarMessage.path}/$message")
}




