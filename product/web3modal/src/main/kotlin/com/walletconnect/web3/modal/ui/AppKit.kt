@file:OptIn(ExperimentalMaterialNavigationApi::class)

package com.walletconnect.web3.modal.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.fragment.dialog
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.walletconnect.web3.modal.R
import com.walletconnect.web3.modal.ui.components.internal.AppKitComponent
import com.walletconnect.web3.modal.ui.navigation.Route

internal const val CHOOSE_NETWORK_KEY = "chooseNetwork"
private const val CHOOSE_NETWORK_ARG = "{chooseNetwork}"
private val appKitPath = Route.APPKIT.path + "/" + CHOOSE_NETWORK_ARG

fun NavGraphBuilder.appKit() {
    dialog<AppKitSheet>(appKitPath) { argument(CHOOSE_NETWORK_KEY) { type = NavType.BoolType } }
}

@SuppressLint("RestrictedApi")
fun NavController.openAppKit(
    shouldOpenChooseNetwork: Boolean = false,
    onError: (Throwable) -> Unit = {}
) {
    when {
        findDestination(R.id.web3ModalGraph) != null -> {
            navigate(R.id.web3ModalGraph, args = Bundle().apply {
                putBoolean(CHOOSE_NETWORK_KEY, shouldOpenChooseNetwork)
            }, navOptions = buildAppKitNavOptions())
        }
        findDestination(appKitPath) != null -> {
            navigate(
                route = Route.APPKIT.path + "/$shouldOpenChooseNetwork",
                navOptions = buildAppKitNavOptions()
            )
        }
        else -> onError(IllegalStateException("Invalid AppKit path"))
    }
}

fun NavGraphBuilder.appKitGraph(navController: NavController) {
    bottomSheet(
        route = appKitPath,
        arguments = listOf(navArgument(CHOOSE_NETWORK_KEY) { type = NavType.BoolType })
    ) {
        val shouldOpenChooseNetwork = it.arguments?.getBoolean(CHOOSE_NETWORK_KEY) ?: false
        AppKit(
            navController = navController,
            shouldOpenChooseNetwork = shouldOpenChooseNetwork
        )
    }
}

private fun buildAppKitNavOptions() = NavOptions.Builder().setLaunchSingleTop(true).build()

@Composable
internal fun AppKit(
    navController: NavController,
    shouldOpenChooseNetwork: Boolean
) {
    AppKitComponent(
        closeModal = navController::popBackStack,
        shouldOpenChooseNetwork = shouldOpenChooseNetwork
    )
}
