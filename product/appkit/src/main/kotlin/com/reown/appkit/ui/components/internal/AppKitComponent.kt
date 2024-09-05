@file:OptIn(ExperimentalAnimationApi::class)

package com.reown.appkit.ui.components.internal

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.reown.appkit.client.Modal
import com.reown.appkit.client.AppKit
import com.reown.appkit.domain.delegate.AppKitDelegate
import com.reown.appkit.ui.AppKitState
import com.reown.appkit.ui.AppKitViewModel
import com.reown.appkit.ui.components.internal.root.AppKitRoot
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.routes.account.AccountNavGraph
import com.reown.appkit.ui.routes.connect.ConnectionNavGraph
import com.reown.appkit.ui.utils.ComposableLifecycleEffect
import com.reown.appkit.ui.utils.toComponentEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
fun AppKitComponent(
    shouldOpenChooseNetwork: Boolean,
    closeModal: () -> Unit
) {
    AppKitComponent(
        navController = rememberAnimatedNavController(),
        shouldOpenChooseNetwork = shouldOpenChooseNetwork,
        closeModal = closeModal
    )
}

@SuppressLint("RestrictedApi")
@Composable
internal fun AppKitComponent(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberAnimatedNavController(),
    shouldOpenChooseNetwork: Boolean,
    closeModal: () -> Unit
) {
    val appKitViewModel: AppKitViewModel = viewModel()
    val state by appKitViewModel.modalState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppKitDelegate
            .wcEventModels
            .onEach { event ->
                when (event) {
                    is Modal.Model.SIWEAuthenticateResponse.Result, is Modal.Model.SessionAuthenticateResponse.Result -> closeModal()
                    is Modal.Model.ApprovedSession -> {
                        if (AppKit.authPayloadParams != null) {
                            navController.navigate(Route.SIWE_FALLBACK.path)
                        } else {
                            closeModal()
                        }
                    }
                    is Modal.Model.DeletedSession.Success -> closeModal()

                    else -> Unit
                }
            }
            .collect()
    }

    ComposableLifecycleEffect(
        onEvent = { _, event ->
            coroutineScope.launch {
                event.toComponentEvent(onClosed = {
                    if (navController.currentDestination?.route == Route.SIWE_FALLBACK.path && appKitViewModel.shouldDisconnect) {
                        appKitViewModel.disconnect()
                    }
                })
            }
        }
    )

    AppKitRoot(
        modifier = modifier,
        navController = navController,
        closeModal = closeModal
    ) {
        AnimatedContent(
            targetState = state,
            contentAlignment = Alignment.BottomCenter,
            transitionSpec = {
                (fadeIn() + slideInVertically(animationSpec = tween(500),
                    initialOffsetY = { fullHeight -> fullHeight })).togetherWith(fadeOut(animationSpec = tween(500)))
            },
            label = "Root Animated content"
        ) { state ->
            when (state) {
                is AppKitState.Connect -> ConnectionNavGraph(
                    navController = navController,
                    closeModal = closeModal,
                    shouldOpenChooseNetwork = shouldOpenChooseNetwork
                )

                is AppKitState.AccountState -> AccountNavGraph(
                    navController = navController,
                    closeModal = closeModal,
                    shouldOpenChangeNetwork = shouldOpenChooseNetwork
                )

                AppKitState.Loading -> LoadingModalState()
                is AppKitState.Error -> ErrorModalState(retry = appKitViewModel::initModalState)
            }
        }
    }
}
