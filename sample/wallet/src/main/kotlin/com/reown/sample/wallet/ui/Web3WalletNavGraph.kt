@file:OptIn(ExperimentalAnimationApi::class)

package com.reown.sample.wallet.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.update_subscription.UpdateSubscriptionRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connection_details.ConnectionDetailsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsRoute
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.routes.composable_routes.get_started.GetStartedRoute
import com.reown.sample.wallet.ui.routes.composable_routes.inbox.InboxRoute
import com.reown.sample.wallet.ui.routes.composable_routes.inbox.InboxViewModel
import com.reown.sample.wallet.ui.routes.composable_routes.notifications.NotificationsScreenRoute
import com.reown.sample.wallet.ui.routes.composable_routes.settings.SettingsRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.paste_uri.PasteUriRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_authenticate.SessionAuthenticateRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal.SessionProposalRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction.ChainAbstractionRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestRoute
import com.reown.sample.wallet.ui.routes.dialog_routes.snackbar_message.SnackbarMessageRoute
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
    getStartedVisited: Boolean,
    modifier: Modifier = Modifier,
    startDestination: String = if (getStartedVisited) Route.Connections.path else Route.GetStarted.path,
) {
    var scrimColor by remember { mutableStateOf(Color.Unspecified) }
    val inboxViewModel: InboxViewModel = viewModel()

    navController.addOnDestinationChangedListener(
        listener = { _, destination, _ ->
            if (destination.route == Route.Connections.path) {
                WalletKitDelegate.currentId = null
            }
        })

    ModalBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
            composable(Route.GetStarted.path) {
                GetStartedRoute(navController)
            }
            composable(Route.Connections.path) {
                ConnectionsRoute(navController, connectionsViewModel, web3walletViewModel)
            }
            composable("${Route.ConnectionDetails.path}/{connectionId}", arguments = listOf(
                navArgument("connectionId") { type = NavType.IntType }
            )) {
                ConnectionDetailsRoute(navController, it.arguments?.getInt("connectionId"), connectionsViewModel)
            }
            composable(
                "${Route.Notifications.path}/{topic}", arguments = listOf(
                    navArgument("topic") {
                        type = NavType.Companion.StringType
                        nullable = false
                    },
                )
            ) {
                NotificationsScreenRoute(navController, it.arguments?.getString("topic")!!, inboxViewModel)
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
            composable(Route.Inbox.path) {
                InboxRoute(navController, inboxViewModel)
            }
            composable(Route.Settings.path) {
                SettingsRoute(navController)
            }
            bottomSheet(Route.ScanUri.path) {
                web3walletViewModel.showLoader(false)
                scrimColor = Color.Unspecified
                ScanUriRoute(navController, sheetState, onScanSuccess = {
                    web3walletViewModel.pair(it)
                })
            }
            bottomSheet(
                "${Route.UpdateSubscription.path}/{topic}", arguments = listOf(
                    navArgument("topic") {
                        type = NavType.Companion.StringType
                        nullable = false
                    })
            ) {
                scrimColor = ModalBottomSheetDefaults.scrimColor
                UpdateSubscriptionRoute(navController, sheetState, it.arguments?.getString("topic")!!)
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
            dialog(Route.PasteUri.path, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                PasteUriRoute(onSubmit = {
                    web3walletViewModel.pair(it)
                    navController.popBackStack()
                })
            }
            bottomSheet("${Route.SnackbarMessage.path}/{message}", arguments = listOf(
                navArgument("message") { type = NavType.StringType }
            )) {
                scrimColor = Color.Unspecified
                SnackbarMessageRoute(navController, it.arguments?.getString("message"))
            }
        }
    }
}

