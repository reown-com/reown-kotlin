package com.reown.sample.wallet.ui.routes.composable_routes.wallets

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import com.reown.sample.common.ui.theme.WCTheme
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.reown.sample.wallet.R
import com.reown.sample.wallet.blockchain.TokenBalance
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.ui.common.getChainIcon
import com.reown.sample.wallet.ui.common.getChainName
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.routes.host.WalletHeader

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WalletsRoute(
    navController: NavController,
    connectionsViewModel: ConnectionsViewModel,
) {
    val usdcBalances by connectionsViewModel.usdcBalances.collectAsState()
    val eurocBalances by connectionsViewModel.eurocBalances.collectAsState()
    val isLoadingBalances by connectionsViewModel.isLoadingBalances.collectAsState()
    val allBalances = usdcBalances + eurocBalances

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoadingBalances,
        onRefresh = { connectionsViewModel.fetchBalances() }
    )

    val colors = WCTheme.colors
    val spacing = WCTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        WalletHeader(navController)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (allBalances.isEmpty() && !isLoadingBalances) {
                EmptyWallets()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.spacing3),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                ) {
                    item { Spacer(modifier = Modifier.height(spacing.spacing1)) }
                    items(allBalances) { balance ->
                        WalletBalanceItem(balance)
                    }
                    item { Spacer(modifier = Modifier.height(spacing.spacing2)) }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoadingBalances,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = colors.foregroundSecondary,
                contentColor = colors.bgAccentPrimary
            )
        }
    }
}

@Composable
fun WalletBalanceItem(balance: TokenBalance) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius
    val chainName = getChainName(balance.chainId)
    val address = EthAccountDelegate.address
    val shortAddress = "${address.take(6)}...${address.takeLast(4)}"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val iconSize = spacing.spacing10
    val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
    val badgeSize = spacing.spacing4 + spacing.spacing05
    val badgeIconSize = spacing.spacing3 + spacing.spacing05

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(borderRadius.shapeXLarge)
            .background(color = colors.foregroundPrimary)
            .padding(spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Token icon with chain badge
        Box {
            val iconModifier = Modifier
                .size(iconSize)
                .clip(CircleShape)

            if (balance.iconUrl != null) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(balance.iconUrl)
                        .size(iconSizePx)
                        .crossfade(true)
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = "${balance.symbol} icon",
                    modifier = iconModifier,
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = iconModifier
                        .background(colors.bgAccentPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = balance.symbol.take(1),
                        style = WCTheme.typography.bodyLgMedium.copy(color = colors.textInvert)
                    )
                }
            }

            // Chain badge overlay
            val chainIcon = getChainIcon(balance.chainId)
            if (chainIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = spacing.spacing05, y = spacing.spacing05)
                        .size(badgeSize)
                        .clip(CircleShape)
                        .background(colors.foregroundPrimary)
                        .padding(spacing.spacing05)
                ) {
                    Image(
                        painter = painterResource(id = chainIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(badgeIconSize)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(spacing.spacing3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${balance.quantity.numeric} ${balance.symbol}",
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = colors.textPrimary
                )
            )
            Text(
                text = shortAddress,
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = colors.textSecondary
                )
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
            contentDescription = "Copy address",
            modifier = Modifier
                .size(spacing.spacing5)
                .clickable {
                    clipboardManager.setText(AnnotatedString(address))
                    Toast.makeText(context, "$chainName address copied", Toast.LENGTH_SHORT).show()
                },
            tint = colors.iconDefault
        )
    }
}

@Composable
private fun EmptyWallets() {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No token balances yet",
            style = WCTheme.typography.h6Regular.copy(
                color = colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(spacing.spacing1 + spacing.spacing05))
        Text(
            text = "Import a funded wallet or send tokens to this address.",
            style = WCTheme.typography.bodyLgRegular.copy(
                color = colors.textSecondary
            )
        )
    }
}
