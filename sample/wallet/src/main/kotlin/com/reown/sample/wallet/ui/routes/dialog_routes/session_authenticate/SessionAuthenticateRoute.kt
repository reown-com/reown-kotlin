package com.reown.sample.wallet.ui.routes.dialog_routes.session_authenticate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.ui.common.AppInfoCard
import com.reown.sample.wallet.ui.common.MessageCard
import com.reown.sample.wallet.ui.common.RequestBottomSheet
import com.reown.sample.wallet.ui.common.ScammerBottomSheet
import com.reown.sample.wallet.ui.common.handleRedirect
import com.reown.sample.wallet.ui.common.showError
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SessionAuthenticateRoute(
    navController: NavHostController,
    connectionsViewModel: ConnectionsViewModel,
    sessionAuthenticateViewModel: SessionAuthenticateViewModel = viewModel()
) {
    val authenticateRequestUI = sessionAuthenticateViewModel.sessionAuthenticateUI ?: run {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showScamWarning by remember { mutableStateOf(authenticateRequestUI.peerContextUI.isScam == true) }

    if (showScamWarning) {
        ScammerBottomSheet(
            origin = authenticateRequestUI.peerContextUI.origin,
            onProceed = { showScamWarning = false },
            onClose = {
                try {
                    sessionAuthenticateViewModel.reject(
                        onSuccess = { redirect ->
                            handleRedirect(redirect, navController, composableScope, context)
                        },
                        onError = { error ->
                            showError(navController, error, composableScope, context)
                        }
                    )
                } catch (e: Throwable) {
                    showError(navController, e, composableScope, context)
                }
            }
        )
    } else {
        SessionAuthenticateContent(
            authenticateRequestUI = authenticateRequestUI,
            viewModel = sessionAuthenticateViewModel,
            connectionsViewModel = connectionsViewModel,
            navController = navController
        )
    }
}

@Composable
private fun SessionAuthenticateContent(
    authenticateRequestUI: SessionAuthenticateUI,
    viewModel: SessionAuthenticateViewModel,
    connectionsViewModel: ConnectionsViewModel,
    navController: NavHostController,
) {
    var isConfirmLoading by remember { mutableStateOf(false) }
    var isCancelLoading by remember { mutableStateOf(false) }

    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    RequestBottomSheet(
        peerUI = authenticateRequestUI.peerUI,
        intention = "Sign a message for",
        isLinkMode = authenticateRequestUI.peerUI.linkMode,
        approveLabel = "Connect",
        isLoadingApprove = isConfirmLoading,
        isLoadingReject = isCancelLoading,
        onApprove = {
            isConfirmLoading = true
            try {
                viewModel.approve(
                    onSuccess = { redirect ->
                        isConfirmLoading = false
                        composableScope.launch(Dispatchers.Main) {
                            connectionsViewModel.refreshConnections()
                        }
                        handleRedirect(redirect, navController, composableScope, context)
                    },
                    onError = { error ->
                        isConfirmLoading = false
                        showError(navController, error, composableScope, context)
                    }
                )
            } catch (e: Exception) {
                showError(navController, e, composableScope, context)
            }
        },
        onReject = {
            isCancelLoading = true
            try {
                viewModel.reject(
                    onSuccess = { redirect ->
                        isCancelLoading = false
                        handleRedirect(redirect, navController, composableScope, context)
                    },
                    onError = { error ->
                        isCancelLoading = false
                        showError(navController, error, composableScope, context)
                    }
                )
            } catch (e: Throwable) {
                showError(navController, e, composableScope, context)
            }
        },
        onClose = {
            try {
                viewModel.reject(
                    onSuccess = { redirect ->
                        handleRedirect(redirect, navController, composableScope, context)
                    },
                    onError = { error ->
                        showError(navController, error, composableScope, context)
                    }
                )
            } catch (e: Throwable) {
                showError(navController, e, composableScope, context)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = themedColor(
                    darkColor = 0xFF3A3A3A,
                    lightColor = 0xFFD0D0D0
                ),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            AppInfoCard(
                url = authenticateRequestUI.peerUI.peerUri,
                validation = authenticateRequestUI.peerContextUI.validation,
                isScam = authenticateRequestUI.peerContextUI.isScam
            )

            Text(
                text = "Messages to sign (${authenticateRequestUI.messages.size})",
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = 0xFF9A9A9A, lightColor = 0xFF9A9A9A)
                )
            )

            MessageCard(
                message = authenticateRequestUI.messages.joinToString("\n\n"),
                showTitle = false,
                maxHeight = 250.dp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
