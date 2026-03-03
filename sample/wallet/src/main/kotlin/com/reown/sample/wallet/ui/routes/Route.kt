package com.reown.sample.wallet.ui.routes

import androidx.navigation.NavController

sealed class Route(val path: String) {
    data object Wallets : Route("wallets")
    data object ConnectedApps : Route("connected_apps")
    data object SessionProposal : Route("session_proposal")
    data object SessionRequest : Route("session_request")
    data object SessionAuthenticate : Route("session_authenticate")
    data object ChainAbstraction : Route("chain_abstraction")
    data object ScannerOptions : Route("scanner_options")
    data object ScanUri : Route("scan_uri")

    data object ConnectionDetails : Route("connection_details")
    data object SnackbarMessage : Route("snackbar_message")
    data object Settings : Route("settings")
    data object TransactionDialog : Route("transaction_dialog")
    data object Payment : Route("payment")
    data object PaymentResult : Route("payment_result")
    data object SecretKeysAndPhrases : Route("secret_keys_and_phrases")
    data object ImportWallet : Route("import_wallet")
}

fun NavController.showSnackbar(message: String) {
    navigate("${Route.SnackbarMessage.path}/$message")
}




