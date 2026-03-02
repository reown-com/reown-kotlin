package com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.reown.sample.wallet.ui.common.AccordionCard
import com.reown.sample.wallet.ui.common.AppInfoCard
import com.reown.sample.wallet.ui.common.ChainIcons
import com.reown.sample.wallet.ui.common.ChainItem
import com.reown.sample.wallet.ui.common.NetworkSelector
import com.reown.sample.wallet.ui.common.RequestBottomSheet
import com.reown.sample.wallet.ui.common.ScammerBottomSheet
import com.reown.sample.wallet.ui.common.getChainName
import com.reown.sample.wallet.ui.common.handleRedirect
import com.reown.sample.wallet.ui.common.showError

@Composable
fun SessionProposalRoute(navController: NavHostController, sessionProposalViewModel: SessionProposalViewModel = viewModel()) {
    val sessionProposalUI = sessionProposalViewModel.sessionProposal ?: throw Exception("Missing session proposal")
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showScamWarning by remember { mutableStateOf(sessionProposalUI.peerContext.isScam == true) }

    if (showScamWarning) {
        ScammerBottomSheet(
            origin = sessionProposalUI.peerContext.origin,
            onProceed = { showScamWarning = false },
            onClose = {
                try {
                    sessionProposalViewModel.reject(
                        proposalPublicKey = sessionProposalUI.pubKey,
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
        SessionProposalContent(
            sessionProposalUI = sessionProposalUI,
            viewModel = sessionProposalViewModel,
            navController = navController
        )
    }
}

@Composable
private fun SessionProposalContent(
    sessionProposalUI: SessionProposalUI,
    viewModel: SessionProposalViewModel,
    navController: NavHostController,
) {
    var isConfirmLoading by remember { mutableStateOf(false) }
    var isCancelLoading by remember { mutableStateOf(false) }
    var expandedAccordion by remember { mutableStateOf<String?>(null) }

    val availableChains = remember { extractAvailableChains(sessionProposalUI) }
    var selectedChainIds by remember { mutableStateOf(availableChains.map { it.chainId }) }

    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current

    RequestBottomSheet(
        peerUI = sessionProposalUI.peerUI,
        intention = "Connect your wallet to",
        approveLabel = "Connect",
        approveEnabled = selectedChainIds.isNotEmpty(),
        isLoadingApprove = isConfirmLoading,
        isLoadingReject = isCancelLoading,
        onApprove = {
            isConfirmLoading = true
            try {
                viewModel.approve(
                    proposalPublicKey = sessionProposalUI.pubKey,
                    selectedChainIds = selectedChainIds,
                    onSuccess = { redirect ->
                        isConfirmLoading = false
                        handleRedirect(redirect, navController, composableScope, context)
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
                viewModel.reject(
                    proposalPublicKey = sessionProposalUI.pubKey,
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
                    proposalPublicKey = sessionProposalUI.pubKey,
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
            AppInfoCard(
                url = sessionProposalUI.peerUI.peerUri,
                validation = sessionProposalUI.peerContext.validation,
                isScam = sessionProposalUI.peerContext.isScam,
                isExpanded = expandedAccordion == "app",
                onPress = { expandedAccordion = if (expandedAccordion == "app") null else "app" }
            )

            AccordionCard(
                headerContent = {
                    Text(
                        text = "Network",
                        style = WCTheme.typography.bodyLgRegular.copy(
                            color = themedColor(darkColor = 0xFF9A9A9A, lightColor = 0xFF9A9A9A)
                        )
                    )
                },
                rightContent = {
                    ChainIcons(chainIds = selectedChainIds)
                },
                isExpanded = expandedAccordion == "network",
                onPress = { expandedAccordion = if (expandedAccordion == "network") null else "network" },
                hideExpand = availableChains.size <= 1
            ) {
                NetworkSelector(
                    availableChains = availableChains,
                    selectedChainIds = selectedChainIds,
                    onSelectionChange = { selectedChainIds = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun extractAvailableChains(sessionProposalUI: SessionProposalUI): List<ChainItem> {
    val requiredChains = sessionProposalUI.namespaces.flatMap { (namespaceKey, proposal) ->
        proposal.chains ?: listOf(namespaceKey)
    }
    val optionalChains = sessionProposalUI.optionalNamespaces.flatMap { (namespaceKey, proposal) ->
        proposal.chains ?: listOf(namespaceKey)
    }
    val allChainIds = (requiredChains + optionalChains).distinct()

    return allChainIds.map { chainId ->
        ChainItem(
            chainId = chainId,
            name = getChainName(chainId),
            namespace = chainId.split(":").firstOrNull() ?: ""
        )
    }
}
