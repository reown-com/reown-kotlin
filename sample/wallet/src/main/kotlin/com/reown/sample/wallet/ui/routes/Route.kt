package com.reown.sample.wallet.ui.routes

import androidx.navigation.NavController

sealed class Route(val path: String) {
    data object Wallets : Route("wallets")
    data object ConnectedApps : Route("connected_apps")
    data object Connections : Route("connections")
    data object SessionProposal : Route("session_proposal")
    data object SessionRequest : Route("session_request")
    data object SessionAuthenticate : Route("session_authenticate")
    data object ChainAbstraction : Route("chain_abstraction")
    data object PasteUri : Route("paste_uri")
    data object ScanUri : Route("scan_uri")

    data object ConnectionDetails : Route("connection_details")
    data object SnackbarMessage : Route("snackbar_message")
    data object Settings : Route("settings")
    @Deprecated("Will be removed") data object Inbox : Route("inbox")
    @Deprecated("Will be removed") data object Notifications : Route("notifications")
    @Deprecated("Will be removed") data object UpdateSubscription : Route("update_subscription")
    data object TransactionDialog : Route("transaction_dialog")
    data object Payment : Route("payment")
    data object PaymentResult : Route("payment_result")
}

fun NavController.showSnackbar(message: String) {
    navigate("${Route.SnackbarMessage.path}/$message")
}




