@file:OptIn(ExperimentalAnimationApi::class)

package com.reown.sample.wallet.ui

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.import_wallet.ImportWalletRoute
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scan_uri.ScanUriRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connected_apps.ConnectedAppsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connection_details.ConnectionDetailsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.routes.composable_routes.secret_keys.SecretKeysRoute
import com.reown.sample.wallet.ui.routes.composable_routes.settings.SettingsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.wallets.WalletsRoute
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scanner_options.ScannerOptionsRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_authenticate.SessionAuthenticateRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal.SessionProposalRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction.ChainAbstractionRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.snackbar_message.SnackbarMessageRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.payment.PaymentRoute
//import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.TransactionRoute

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("RestrictedApi")
@ExperimentalMaterialNavigationApi
@Composable
fun Web3WalletNavGraph(
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    web3walletViewModel: Web3WalletViewModel,
    connectionsViewModel: ConnectionsViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = Route.Wallets.path,
) {
    navController.addOnDestinationChangedListener(
        listener = { _, destination, _ ->
            if (destination.route == Route.Wallets.path) {
                WalletKitDelegate.currentId = null
            }
        })

    val sheetState = remember { bottomSheetNavigator.navigatorSheetState }

    AnimatedNavHost(
        navController = navController,
        modifier = modifier,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(Route.Wallets.path) {
            WalletsRoute(navController, connectionsViewModel)
        }
        composable(Route.ConnectedApps.path) {
            ConnectedAppsRoute(navController, connectionsViewModel)
        }
        bottomSheet("${Route.ConnectionDetails.path}/{connectionId}", arguments = listOf(
            navArgument("connectionId") { type = NavType.IntType }
        )) {
            ConnectionDetailsRoute(navController, it.arguments?.getInt("connectionId"), connectionsViewModel)
        }
        dialog(
            route = "${Route.ChainAbstraction.path}/{isError}",
            arguments = listOf(
                navArgument("isError") {
                    type = NavType.BoolType
                    nullable = false
                }),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ChainAbstractionRoute(navController, it.arguments?.getBoolean("isError")!!)
        }
        composable(Route.Settings.path) {
            SettingsRoute(navController)
        }
        composable(Route.SecretKeysAndPhrases.path) {
            SecretKeysRoute(navController)
        }
        bottomSheet(Route.ImportWallet.path) {
            ImportWalletRoute(navController)
        }
        bottomSheet(Route.ScanUri.path) {
            web3walletViewModel.showLoader(false)
            ScanUriRoute(navController, sheetState, onScanSuccess = {
                web3walletViewModel.pair(it)
            })
        }
//            dialog(Route.TransactionDialog.path, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
//                TransactionRoute(navController)
//            }
        bottomSheet(Route.SessionProposal.path) {
            SessionProposalRoute(navController)
        }
        bottomSheet(Route.SessionAuthenticate.path) {
            SessionAuthenticateRoute(navController, connectionsViewModel)
        }
        bottomSheet(Route.SessionRequest.path) {
            SessionRequestRoute(navController)
        }
        bottomSheet(Route.ScannerOptions.path) {
            ScannerOptionsRoute(
                navController = navController,
                onPair = { web3walletViewModel.pair(it) }
            )
        }
        bottomSheet("${Route.SnackbarMessage.path}/{message}", arguments = listOf(
            navArgument("message") { type = NavType.StringType }
        )) {
            SnackbarMessageRoute(navController, it.arguments?.getString("message"))
        }
        dialog(
            "${Route.Payment.path}/{paymentLink}",
            arguments = listOf(
                navArgument("paymentLink") {
                    type = NavType.StringType
                    nullable = false
                }
            ),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            val encodedLink = it.arguments?.getString("paymentLink") ?: ""
            val paymentLink = java.net.URLDecoder.decode(encodedLink, "UTF-8")
            PaymentRoute(
                navController = navController,
                paymentLink = paymentLink,
                onPaymentSuccess = { connectionsViewModel.fetchBalances() }
            )
        }
    }
}
