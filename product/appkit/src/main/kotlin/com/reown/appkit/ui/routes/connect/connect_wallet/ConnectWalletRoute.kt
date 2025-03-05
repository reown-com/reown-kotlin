package com.reown.appkit.ui.routes.connect.connect_wallet

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.reown.android.internal.common.modal.data.model.Wallet
import com.reown.appkit.ui.components.internal.ErrorModalState
import com.reown.appkit.ui.components.internal.commons.InstalledWalletIcon
import com.reown.appkit.ui.components.internal.commons.ListSelectRow
import com.reown.appkit.ui.components.internal.commons.RecentLabel
import com.reown.appkit.ui.components.internal.commons.WalletImage
import com.reown.appkit.ui.components.internal.walletconnect.allWallets
import com.reown.appkit.ui.model.UiStateBuilder
import com.reown.appkit.ui.previews.ConnectYourWalletPreviewProvider
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.previews.AppKitPreview
import com.reown.appkit.ui.routes.connect.ConnectViewModel
import com.reown.appkit.ui.theme.AppKitTheme

@Composable
internal fun ConnectWalletRoute(
    connectViewModel: ConnectViewModel
) {
    UiStateBuilder(
        connectViewModel.uiState,
        onError = { ErrorModalState { connectViewModel.fetchInitialWallets() } }
    ) {
        ConnectWalletContent(
            wallets = it,
            walletsTotalCount = connectViewModel.getWalletsTotalCount(),
            onWalletItemClick = { wallet -> connectViewModel.navigateToRedirectRoute(wallet) },
            onViewAllClick = { connectViewModel.navigateToAllWallets() },
        )
    }
}

@Composable
private fun ConnectWalletContent(
    wallets: List<Wallet>,
    walletsTotalCount: Int,
    onWalletItemClick: (Wallet) -> Unit,
    onViewAllClick: () -> Unit,
) {
    WalletsList(
        wallets = wallets,
        walletsTotalCount = walletsTotalCount,
        onWalletItemClick = onWalletItemClick,
        onViewAllClick = onViewAllClick,
    )
}

@Composable
private fun WalletsList(
    wallets: List<Wallet>,
    walletsTotalCount: Int,
    onWalletItemClick: (Wallet) -> Unit,
    onViewAllClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        itemsIndexed(items = wallets.take(4)) { _, item ->
            WalletListSelect(item, onWalletItemClick)
        }
        allWallets(text = walletSizeLabel(walletsTotalCount), onClick = onViewAllClick)
    }
}

private fun walletSizeLabel(total: Int): String {
    if (total < 10) {
        return total.toString()
    }

    val remainder = total % 10
    return if (remainder != 0) {
        "${total - remainder}+"
    } else {
        total.toString()
    }
}

@Composable
private fun WalletListSelect(item: Wallet, onWalletItemClick: (Wallet) -> Unit) {
    val label: (@Composable (Boolean) -> Unit)? = when {
        item.isRecent -> {
            { RecentLabel(it) }
        }
        else -> null
    }

    ListSelectRow(
        startIcon = {
            Box {
                WalletImage(
                    url = item.imageUrl,
                    modifier = Modifier
                        .size(40.dp)
                        .border(width = 1.dp, color = AppKitTheme.colors.grayGlass10, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                )
                if (item.isWalletInstalled) {
                    InstalledWalletIcon()
                }
            }
        },
        text = item.name,
        onClick = { onWalletItemClick(item) },
        contentPadding = PaddingValues(vertical = 4.dp),
        label = label
    )
}

@UiModePreview
@Composable
private fun ConnectYourWalletPreview(
    @PreviewParameter(ConnectYourWalletPreviewProvider::class) wallets: List<Wallet>
) {
    AppKitPreview(title = "Connect Wallet") {
        ConnectWalletContent(wallets, 200, {}, {})
    }
}
