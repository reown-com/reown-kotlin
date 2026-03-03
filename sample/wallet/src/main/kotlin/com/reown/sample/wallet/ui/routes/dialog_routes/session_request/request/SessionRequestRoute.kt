package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.domain.signer.EthSigner
import com.reown.sample.wallet.domain.signer.Signer.PERSONAL_SIGN
import com.reown.sample.wallet.domain.WalletKitDelegate.currentId
import com.reown.sample.wallet.ui.common.AccordionCard
import com.reown.sample.wallet.ui.common.AppInfoCard
import com.reown.sample.wallet.ui.common.ChainIcons
import com.reown.sample.wallet.ui.common.MessageCard
import com.reown.sample.wallet.ui.common.RequestBottomSheet
import com.reown.sample.wallet.ui.common.handleRedirect
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.showError

@Composable
fun SessionRequestRoute(navController: NavHostController, sessionRequestViewModel: SessionRequestViewModel = viewModel()) {
    val sessionRequestUI = sessionRequestViewModel.sessionRequestUI
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isConfirmLoading by remember { mutableStateOf(false) }
    var isCancelLoading by remember { mutableStateOf(false) }

    when (sessionRequestUI) {
        is SessionRequestUI.Content -> {
            currentId = sessionRequestUI.requestId
            RequestBottomSheet(
                peerUI = sessionRequestUI.peerUI,
                intention = "Sign a message for",
                approveLabel = "Sign",
                isLoadingApprove = isConfirmLoading,
                isLoadingReject = isCancelLoading,
                onApprove = {
                    isConfirmLoading = true
                    try {
                        sessionRequestViewModel.approve(
                            onSuccess = { uri ->
                                isConfirmLoading = false
                                handleRedirect(uri, navController, composableScope, context)
                            },
                            onError = { error ->
                                isConfirmLoading = false
                                showError(navController, error, composableScope, context)
                            }
                        )
                    } catch (e: Throwable) {
                        showError(navController, e, composableScope, context)
                    }
                },
                onReject = {
                    isCancelLoading = true
                    try {
                        sessionRequestViewModel.reject(
                            onSuccess = { uri ->
                                isCancelLoading = false
                                handleRedirect(uri, navController, composableScope, context)
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
                    navController.popBackStack()
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing4),
                    verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
                ) {
                    AppInfoCard(
                        url = sessionRequestUI.peerUI.peerUri,
                        validation = sessionRequestUI.peerContextUI.validation,
                        isScam = sessionRequestUI.peerContextUI.isScam
                    )

                    val displayParam = if (sessionRequestUI.method == PERSONAL_SIGN) {
                        runCatching { EthSigner.extractMessageFromParams(sessionRequestUI.param) }.getOrDefault(sessionRequestUI.param)
                    } else sessionRequestUI.param

                    MessageCard(
                        message = displayParam,
                        title = "Params"
                    )

                    if (!sessionRequestUI.chain.isNullOrEmpty()) {
                        AccordionCard(
                            headerContent = {
                                Text(
                                    text = "Network",
                                    style = WCTheme.typography.bodyLgRegular.copy(
                                        color = WCTheme.colors.textSecondary
                                    )
                                )
                            },
                            rightContent = {
                                ChainIcons(chainIds = listOf(sessionRequestUI.chain))
                            },
                            isExpanded = false,
                            onPress = {},
                            hideExpand = true
                        ) {}
                    }

                    Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))
                }
            }
        }

        SessionRequestUI.Initial -> {
            RequestBottomSheet(
                peerUI = PeerUI.Empty,
                intention = "Loading request for",
                approveLabel = "Sign",
                approveEnabled = false,
                onClose = {
                    navController.popBackStack()
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 8.dp,
                        modifier = Modifier.size(75.dp),
                        color = Color(0xFFB8F53D)
                    )
                }
            }
        }
    }
}
