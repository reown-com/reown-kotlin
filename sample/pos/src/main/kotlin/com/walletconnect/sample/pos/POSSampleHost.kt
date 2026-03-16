package com.walletconnect.sample.pos

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.screens.AmountScreen
import com.walletconnect.sample.pos.screens.ErrorScreen
import com.walletconnect.sample.pos.screens.PaymentScreen
import com.walletconnect.sample.pos.screens.PaymentSuccessScreen
import com.walletconnect.sample.pos.screens.SettingsScreen
import com.walletconnect.sample.pos.screens.StartPaymentScreen
import com.walletconnect.sample.pos.screens.TransactionHistoryScreen

sealed class Screen(val route: String, val label: String) {
    object StartPaymentScreen : Screen("start", "Home")
    object AmountScreen : Screen("amount", "Amount")
    object PaymentScreen : Screen("payment?qrUrl={qrUrl}&expiresAt={expiresAt}", "Payment") {
        fun routeWith(qrUrl: String, expiresAt: Long) = "payment?qrUrl=${Uri.encode(qrUrl)}&expiresAt=$expiresAt"
        const val argQrUrl = "qrUrl"
        const val argExpiresAt = "expiresAt"
    }
    object ErrorScreen : Screen("error?errorCode={errorCode}", "Error") {
        fun routeWith(errorCode: String) = "error?errorCode=${Uri.encode(errorCode)}"
        const val arg = "errorCode"
    }
    object PaymentSuccessScreen : Screen("success?amount={amount}", "Success") {
        fun routeWith(amount: String) = "success?amount=${Uri.encode(amount)}"
        const val arg = "amount"
    }
    object TransactionHistoryScreen : Screen("history", "History")
    object SettingsScreen : Screen("settings", "Settings")
}

@Composable
fun POSSampleHost(viewModel: POSViewModel, navController: NavHostController = rememberNavController()) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = WCTheme.colors.bgPrimary
    ) { paddings ->
        NavHost(
            navController = navController,
            startDestination = Screen.StartPaymentScreen.route,
            modifier = Modifier
                .padding(paddings)
                .background(WCTheme.colors.bgPrimary)
        ) {
            composable(Screen.StartPaymentScreen.route) {
                StartPaymentScreen(viewModel)
            }

            composable(Screen.AmountScreen.route) {
                AmountScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PaymentScreen.route,
                arguments = listOf(
                    navArgument(Screen.PaymentScreen.argQrUrl) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Screen.PaymentScreen.argExpiresAt) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val qrUrl = backStackEntry.arguments?.getString(Screen.PaymentScreen.argQrUrl)
                val expiresAt = backStackEntry.arguments?.getLong(Screen.PaymentScreen.argExpiresAt) ?: 0L
                PaymentScreen(
                    viewModel = viewModel,
                    qrUrl = qrUrl.orEmpty(),
                    expiresAt = expiresAt,
                    onBack = { navController.popBackStack() },
                    onReturnToStart = {
                        viewModel.resetForNewPayment()
                        navController.navigate(Screen.StartPaymentScreen.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAmount = {
                        viewModel.resetForNewPayment()
                        navController.navigate(Screen.AmountScreen.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    navigateToErrorScreen = { errorCode ->
                        navController.navigate(Screen.ErrorScreen.routeWith(errorCode)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.ErrorScreen.route,
                arguments = listOf(navArgument(Screen.ErrorScreen.arg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val errorCode = backStackEntry.arguments?.getString(Screen.ErrorScreen.arg)
                ErrorScreen(
                    errorCode = errorCode.orEmpty(),
                    onNewPayment = {
                        viewModel.resetForNewPayment()
                        navController.navigate(Screen.StartPaymentScreen.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                        navController.navigate(Screen.AmountScreen.route)
                    }
                )
            }

            composable(
                route = Screen.PaymentSuccessScreen.route,
                arguments = listOf(navArgument(Screen.PaymentSuccessScreen.arg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) { backStackEntry ->
                val amount = backStackEntry.arguments?.getString(Screen.PaymentSuccessScreen.arg)
                PaymentSuccessScreen(
                    displayAmount = amount.orEmpty(),
                    paymentInfo = viewModel.lastPaymentInfo,
                    onNewPayment = {
                        viewModel.resetForNewPayment()
                        navController.navigate(Screen.StartPaymentScreen.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                        navController.navigate(Screen.AmountScreen.route)
                    },
                    onPrintReceipt = {
                        viewModel.printReceipt()
                    }
                )
            }

            composable(Screen.TransactionHistoryScreen.route) {
                TransactionHistoryScreen(
                    viewModel = viewModel,
                    onClose = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.SettingsScreen.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onClose = {
                        navController.popBackStack()
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

                is PosNavEvent.QrReady -> {
                    val route = Screen.PaymentScreen.routeWith(event.uri.toString(), event.expiresAt)
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }

                PosNavEvent.FlowFinished -> {
                    viewModel.resetForNewPayment()
                    navController.navigate(Screen.StartPaymentScreen.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                is PosNavEvent.PaymentSuccessScreen -> {
                    viewModel.storeLastPaymentInfo(event.info)
                    val displayAmount = viewModel.displayAmount.value
                    navController.navigate(Screen.PaymentSuccessScreen.routeWith(displayAmount)) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }

                is PosNavEvent.ToErrorScreen -> navController.navigate(Screen.ErrorScreen.routeWith(event.error)) {
                    launchSingleTop = true
                }

                PosNavEvent.ToTransactionHistory -> navController.navigate(Screen.TransactionHistoryScreen.route) {
                    launchSingleTop = true
                }

                PosNavEvent.ToSettings -> navController.navigate(Screen.SettingsScreen.route) {
                    launchSingleTop = true
                }
            }
        }
    }
}
