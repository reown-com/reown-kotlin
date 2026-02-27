@file:OptIn(ExperimentalAnimationApi::class)

package com.reown.sample.wallet.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.reown.sample.wallet.domain.WalletKitDelegate
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scan_uri.ScanUriRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connected_apps.ConnectedAppsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connection_details.ConnectionDetailsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
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
    var scrimColor by remember { mutableStateOf(Color.Black.copy(alpha = 0.32f)) }

    navController.addOnDestinationChangedListener(
        listener = { _, destination, _ ->
            if (destination.route == Route.Wallets.path) {
                WalletKitDelegate.currentId = null
            }
        })

    ModalBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetBackgroundColor = Color.Transparent, sheetElevation = 0.dp,
        scrimColor = scrimColor
    ) {
        val sheetState = remember { bottomSheetNavigator.navigatorSheetState }

        AnimatedNavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                    animationSpec = tween(700)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                    animationSpec = tween(700)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                    animationSpec = tween(700)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                    animationSpec = tween(700)
                )
            }
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
            bottomSheet(Route.ScanUri.path) {
                web3walletViewModel.showLoader(false)
                scrimColor = Color.Transparent
                ScanUriRoute(navController, sheetState, onScanSuccess = {
                    web3walletViewModel.pair(it)
                })
            }
//            dialog(Route.TransactionDialog.path, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
//                TransactionRoute(navController)
//            }
            dialog(Route.SessionProposal.path, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                SessionProposalRoute(navController)
            }
            dialog(Route.SessionAuthenticate.path, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                SessionAuthenticateRoute(navController, connectionsViewModel)
            }
            dialog(
                Route.SessionRequest.path, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
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
                scrimColor = Color.Transparent
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
}

