package com.reown.appkit.ui.routes.account.chain_redirect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.appkit.client.Modal
import com.reown.appkit.domain.delegate.AppKitDelegate
import com.reown.appkit.ui.components.internal.commons.DeclinedIcon
import com.reown.appkit.ui.components.internal.commons.LoadingHexagonBorder
import com.reown.appkit.ui.components.internal.commons.VerticalSpacer
import com.reown.appkit.ui.components.internal.commons.button.TryAgainButton
import com.reown.appkit.ui.components.internal.commons.network.HexagonNetworkImage
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.previews.AppKitPreview
import com.reown.appkit.ui.previews.testChains
import com.reown.appkit.ui.routes.account.AccountViewModel
import com.reown.appkit.ui.theme.AppKitTheme
import com.reown.appkit.utils.getImageData
import com.reown.appkit.utils.toSession
import kotlinx.coroutines.launch

@Composable
internal fun ChainSwitchRedirectRoute(
    accountViewModel: AccountViewModel,
    chain: Modal.Model.Chain,
) {
    val scope = rememberCoroutineScope()
    var chainSwitchState by remember { mutableStateOf<ChainRedirectState>(ChainRedirectState.Loading) }

    val onReject =  { chainSwitchState = ChainRedirectState.Declined }
    val switchChain = suspend { accountViewModel.switchChain(to = chain, onReject = onReject) }

    LaunchedEffect(Unit) {
        switchChain()
    }

    LaunchedEffect(Unit) {
        AppKitDelegate.wcEventModels.collect {
            when (it) {
                is Modal.Model.UpdatedSession -> accountViewModel.updatedSessionAfterChainSwitch(it.toSession(chain))
                is Modal.Model.SessionRequestResponse -> if (it.result is Modal.Model.JsonRpcResponse.JsonRpcError) { onReject() }
                else -> {}
            }
        }
    }
    ChainSwitchRedirectScreen(
        chain = chain,
        chainRedirectState = chainSwitchState,
        onTryAgainClick = {
            chainSwitchState = ChainRedirectState.Loading
            scope.launch { switchChain() }
        }
    )
}

@Composable
internal fun ChainSwitchRedirectScreen(
    chain: Modal.Model.Chain,
    chainRedirectState: ChainRedirectState,
    onTryAgainClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 40.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChainNetworkImage(
            chain = chain,
            redirectState = chainRedirectState
        )
        VerticalSpacer(height = 12.dp)
        ChainSwitchInfo(redirectState = chainRedirectState)
        VerticalSpacer(height = 20.dp)
        AnimatedVisibility(visible = chainRedirectState == ChainRedirectState.Declined) {
            TryAgainButton { onTryAgainClick() }
        }

    }
}

@Composable
private fun ChainSwitchInfo(redirectState: ChainRedirectState) {
    AnimatedContent(targetState = redirectState, label = "Chain switch info") { state ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = state.toTitle(), style = AppKitTheme.typo.paragraph500)
            VerticalSpacer(height = 8.dp)
            Text(
                text = state.toInformation(),
                style = AppKitTheme.typo.small400.copy(AppKitTheme.colors.foreground.color200, textAlign = TextAlign.Center)
            )
        }
    }
}

private fun ChainRedirectState.toTitle() = when (this) {
    ChainRedirectState.Declined -> "Switch declined"
    ChainRedirectState.Loading -> "Approve in wallet"
}

private fun ChainRedirectState.toInformation() = when (this) {
    ChainRedirectState.Declined -> "Switch can be declined if chain is not supported by a wallet or previous request is still active"
    ChainRedirectState.Loading -> "Accept connection request in your wallet"
}

@Composable
private fun ChainNetworkImage(
    chain: Modal.Model.Chain,
    redirectState: ChainRedirectState
) {
    ChainNetworkImageWrapper(redirectState) {
        HexagonNetworkImage(
            data = chain.getImageData(),
            isEnabled = true,
            size = 96.dp
        )
    }
}

@Composable
private fun ChainNetworkImageWrapper(
    redirectState: ChainRedirectState,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = redirectState,
        label = "ChainNetworkImageWrapper"
    ) { state ->
        when (state) {
            ChainRedirectState.Declined -> {
                Box {
                    content()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(AppKitTheme.colors.background.color100, shape = CircleShape)
                            .padding(2.dp)
                    ) {
                        DeclinedIcon()
                    }
                }
            }

            ChainRedirectState.Loading -> {
                LoadingHexagonBorder {
                    content()
                }
            }
        }
    }
}

@Composable
@UiModePreview
private fun ChainSwitchRedirectScreenWithLoadingStatePreview() {
    val chain = testChains.first()
    AppKitPreview(title = chain.chainName) {
        ChainSwitchRedirectScreen(chain, ChainRedirectState.Loading, {})
    }
}

@Composable
@UiModePreview
private fun ChainSwitchRedirectScreenWithDeclinedStatePreview() {
    val chain = testChains.first()
    AppKitPreview(title = chain.chainName) {
        ChainSwitchRedirectScreen(chain, ChainRedirectState.Declined, {})
    }
}

