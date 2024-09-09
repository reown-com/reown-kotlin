package com.reown.appkit.ui.routes.account.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.appkit.client.Modal
import com.reown.appkit.domain.model.AccountData
import com.reown.appkit.domain.model.Balance
import com.reown.appkit.ui.components.internal.commons.CloseIcon
import com.reown.appkit.ui.components.internal.commons.CompassIcon
import com.reown.appkit.ui.components.internal.commons.DisconnectIcon
import com.reown.appkit.ui.components.internal.commons.ExternalIcon
import com.reown.appkit.ui.components.internal.commons.VerticalSpacer
import com.reown.appkit.ui.components.internal.commons.account.AccountName
import com.reown.appkit.ui.components.internal.commons.account.AccountImage
import com.reown.appkit.ui.components.internal.commons.button.ButtonSize
import com.reown.appkit.ui.components.internal.commons.button.ButtonStyle
import com.reown.appkit.ui.components.internal.commons.button.ChipButton
import com.reown.appkit.ui.components.internal.commons.entry.AccountEntry
import com.reown.appkit.ui.components.internal.commons.entry.AccountEntryState
import com.reown.appkit.ui.components.internal.commons.network.CircleNetworkImage
import com.reown.appkit.ui.model.UiStateBuilder
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.previews.AppKitPreview
import com.reown.appkit.ui.previews.ethereumChain
import com.reown.appkit.ui.routes.account.AccountViewModel
import com.reown.appkit.ui.previews.accountDataPreview
import com.reown.appkit.ui.theme.AppKitTheme
import com.reown.appkit.utils.getImageData

@Composable
internal fun AccountRoute(
    navController: NavController,
    accountViewModel: AccountViewModel
) {
    val uriHandler = LocalUriHandler.current
    val selectedChain by accountViewModel.selectedChain.collectAsState(initial = accountViewModel.getSelectedChainOrFirst())
    val balance by accountViewModel.balanceState.collectAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        UiStateBuilder(uiStateFlow = accountViewModel.accountState) { data ->
            AccountScreen(
                accountData = data,
                selectedChain = selectedChain,
                balance = balance,
                onBlockExplorerClick = { url -> uriHandler.openUri(url) },
                onChangeNetworkClick = { navController.navigate(Route.CHANGE_NETWORK.path) },
                onDisconnectClick = { accountViewModel.disconnect() }
            )
        }
        CloseIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp),
            onClick = { accountViewModel.closeModal() }
        )
    }
}

@Composable
private fun AccountScreen(
    accountData: AccountData,
    selectedChain: Modal.Model.Chain,
    balance: Balance?,
    onBlockExplorerClick: (String) -> Unit,
    onChangeNetworkClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                top = 32.dp, bottom = 16.dp, start = 12.dp, end = 12.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountImage(address = accountData.address, avatarUrl = accountData.identity?.avatar)
        VerticalSpacer(height = 20.dp)
        AccountName(accountData)
        balance?.let { balance ->
            VerticalSpacer(height = 4.dp)
            Text(
                text = balance.valueWithSymbol,
                style = AppKitTheme.typo.paragraph400.copy(AppKitTheme.colors.foreground.color200)
            )
        }
        selectedChain.blockExplorerUrl?.let { url ->
            VerticalSpacer(height = 12.dp)
            ChipButton(
                text = "Block Explorer",
                startIcon = { CompassIcon() },
                endIcon = { ExternalIcon(it) },
                style = ButtonStyle.SHADE,
                size = ButtonSize.S,
                onClick = { onBlockExplorerClick("$url/address/${accountData.address}") }
            )
        }
        VerticalSpacer(height = 20.dp)
        AccountEntry(
            startIcon = { CircleNetworkImage(selectedChain.getImageData()) },
            onClick = onChangeNetworkClick,
            state = AccountEntryState.NEXT
        ) {
            Text(text = selectedChain.chainName, style = AppKitTheme.typo.paragraph500.copy(color = it.textColor))
        }
        VerticalSpacer(height = 8.dp)
        AccountEntry(
            startIcon = { DisconnectIcon() },
            onClick = onDisconnectClick
        ) {
            Text(text = "Disconnect", style = AppKitTheme.typo.paragraph500.copy(color = it.textColor))
        }
    }
}

@UiModePreview
@Composable
private fun PreviewAccountScreen() {
    AppKitPreview {
        AccountScreen(accountDataPreview, ethereumChain,null, {}, {}, {})
    }
}

@UiModePreview
@Composable
private fun PreviewAccountScreenWithBalance() {
    AppKitPreview {
        AccountScreen(accountDataPreview, ethereumChain, Balance(ethereumChain.token, "0000000"), {}, {}, {})
    }
}

