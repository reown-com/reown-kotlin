package com.reown.sample.pos

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reown.sample.pos.screens.AmountScreen
import com.reown.sample.pos.screens.PaymentScreen
import com.reown.sample.pos.screens.SelectNetworkScreen
import com.reown.sample.pos.screens.SelectTokenScreen
import com.reown.sample.pos.screens.StartPaymentScreen

sealed class Screen(val route: String, val label: String) {
    object StartPaymentScreen : Screen("start", "Home")
    object AmountScreen : Screen("amount", "Profile")
    object SelectTokenScreen : Screen("token", "Token")
    object SelectNetworkScreen : Screen("network", "Network")
    object PaymentScreen : Screen("payment?qrUrl={qrUrl}", "Payment") {
        fun routeWith(qrUrl: String) = "payment?qrUrl=${Uri.encode(qrUrl)}"
        const val arg = "qrUrl"
    }
}

@Composable
fun POSSampleHost(viewModel: POSViewModel, navController: NavHostController = rememberNavController()) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()

    Scaffold(scaffoldState = scaffoldState) { paddings ->
        NavHost(
            navController = navController,
            startDestination = Screen.StartPaymentScreen.route,
            modifier = Modifier.padding(paddings)
        ) {
            composable(Screen.StartPaymentScreen.route) { StartPaymentScreen(viewModel) }
            composable(Screen.AmountScreen.route) { AmountScreen(viewModel) }
            composable(Screen.SelectTokenScreen.route) { SelectTokenScreen(viewModel) }
            composable(Screen.SelectNetworkScreen.route) { SelectNetworkScreen(viewModel) }
            composable(
                route = Screen.PaymentScreen.route,
                arguments = listOf(navArgument(Screen.PaymentScreen.arg) {
                    type = NavType.StringType
                    nullable = true   // allow navigating even if you temporarily have no URL
                    defaultValue = null
                })
            ) { backStackEntry ->
                val qrUrl = backStackEntry.arguments?.getString(Screen.PaymentScreen.arg)
                PaymentScreen(
                    viewModel = viewModel,
                    qrUrl = qrUrl.orEmpty(),            // pass to your composable
                    onReturnToStart = {
                        navController.navigate(Screen.StartPaymentScreen.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.posNavEventsFlow.collect { event ->
            when (event) {
                PosNavEvent.ToStart -> navController.navigate(Screen.StartPaymentScreen.route) {
                    launchSingleTop = true
                    popUpTo(Screen.StartPaymentScreen.route) { inclusive = false }
                }

                PosNavEvent.ToAmount -> navController.navigate(Screen.AmountScreen.route) {
                    launchSingleTop = true
                }

                PosNavEvent.ToSelectToken -> navController.navigate(Screen.SelectTokenScreen.route) {
                    launchSingleTop = true
                }

                PosNavEvent.ToSelectNetwork -> navController.navigate(Screen.SelectNetworkScreen.route) {
                    launchSingleTop = true
                }

                is PosNavEvent.QrReady -> navController.navigate("payment?qrUrl=${Uri.encode(event.uri.toString())}") {
                    launchSingleTop = true
                }

                PosNavEvent.FlowFinished -> {
                    // Example: return to start and clear the flow from back stack
                    navController.navigate(Screen.StartPaymentScreen.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}