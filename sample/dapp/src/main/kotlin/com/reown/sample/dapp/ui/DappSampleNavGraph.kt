
package com.reown.sample.dapp.ui

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import com.reown.sample.dapp.ui.routes.Route
import com.reown.sample.dapp.ui.routes.composable_routes.account.AccountRoute
import com.reown.sample.dapp.ui.routes.composable_routes.chain_selection.ChainSelectionRoute
import com.reown.sample.dapp.ui.routes.composable_routes.pay.PayWebViewRoute
import com.reown.sample.dapp.ui.routes.composable_routes.session.SessionRoute
import com.reown.appkit.ui.appKitGraph

@SuppressLint("RestrictedApi")
@Composable
fun DappSampleNavGraph(
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    startDestination: String,
) {
    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetBackgroundColor = Color.Transparent,
        sheetElevation = 0.dp,
        scrimColor = Color.Unspecified,
        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Route.ChainSelection.path) {
                ChainSelectionRoute(navController)
            }
            composable(Route.Session.path) {
                SessionRoute(navController)
            }
            composable(
                route = Route.Account.path + "/{$accountArg}",
                arguments = listOf(navArgument(accountArg) { type = NavType.StringType })
            ) {
                AccountRoute(navController)
            }
            composable(
                route = Route.Pay.path + "/{$payUrlArg}",
                arguments = listOf(navArgument(payUrlArg) { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString(payUrlArg).orEmpty()
                PayWebViewRoute(navController, gatewayUrl = Uri.decode(encoded))
            }
            appKitGraph(navController)
        }
    }
}

const val accountArg = "accountArg"
fun NavController.navigateToAccount(selectedAccount: String) {
    navigate(Route.Account.path + "/$selectedAccount")
}

const val payUrlArg = "payUrlArg"
fun NavController.navigateToPay(gatewayUrl: String) {
    navigate(Route.Pay.path + "/" + Uri.encode(gatewayUrl))
}
