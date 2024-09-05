package com.reown.appkit.ui.navigation.account

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import com.google.accompanist.navigation.animation.composable
import androidx.navigation.navArgument
import com.reown.util.Empty
import com.reown.appkit.client.Modal
import com.reown.appkit.client.AppKit
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.navigation.addTitleArg
import com.reown.appkit.ui.routes.account.AccountViewModel
import com.reown.appkit.ui.routes.account.chain_redirect.ChainSwitchRedirectRoute

private const val CHAIN_ID_KEY = "chainId"
private const val CHAIN_ID_ARG = "{chainId}"

internal fun Modal.Model.Chain.toChainSwitchPath() = Route.CHAIN_SWITCH_REDIRECT.path + "/${id}&${chainName}"

@OptIn(ExperimentalAnimationApi::class)
internal fun NavGraphBuilder.chainSwitchRoute(
    accountViewModel: AccountViewModel
) {
    composable(
        route = Route.CHAIN_SWITCH_REDIRECT.path + "/" + CHAIN_ID_ARG + addTitleArg(),
        arguments = listOf(navArgument(CHAIN_ID_KEY) { type = NavType.StringType })
    ) { backStackEntry ->
        val chainId = backStackEntry.arguments?.getString(CHAIN_ID_KEY, String.Empty)
        val chain = AppKit.chains.find { it.id == chainId }
        chain?.let { ChainSwitchRedirectRoute(accountViewModel = accountViewModel, chain = it) }
    }
}
