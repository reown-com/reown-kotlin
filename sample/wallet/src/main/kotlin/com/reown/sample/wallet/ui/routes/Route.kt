package com.reown.sample.wallet.ui.routes

import androidx.navigation.NavController

sealed class Route(val path: String) {
    data object GetStarted : Route("get_started")
    data object Connections : Route("connections")
    data object SessionProposal : Route("session_proposal")
    data object SessionRequest : Route("session_request")
    data object SessionAuthenticate : Route("session_authenticate")
    data object ChainAbstraction : Route("chain_abstraction")
    data object PasteUri : Route("paste_uri")
    data object ScanUri : Route("scan_uri")

    data object ConnectionDetails : Route("connection_details")
    data object SnackbarMessage : Route("snackbar_message")
    data object ExploreDapps : Route("explore_dapps")
    data object Inbox : Route("inbox")
    data object Notifications : Route("notifications")
    data object UpdateSubscription : Route("update_subscription")
    data object Settings : Route("settings")
    data object TransactionDialog : Route("transaction_dialog")
}

fun NavController.showSnackbar(message: String) {
    navigate("${Route.SnackbarMessage.path}/$message")
}




