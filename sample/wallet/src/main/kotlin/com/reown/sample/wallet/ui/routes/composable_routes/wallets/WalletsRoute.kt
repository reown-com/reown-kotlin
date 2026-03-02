package com.reown.sample.wallet.ui.routes.composable_routes.wallets

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.reown.sample.common.ui.themedColor
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
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(allBalances) { balance ->
                        WalletBalanceItem(balance)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoadingBalances,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = themedColor(darkColor = Color(0xFF2A2A2A), lightColor = Color.White),
                contentColor = Color(0xFF3396FF)
            )
        }
    }
}

@Composable
fun WalletBalanceItem(balance: TokenBalance) {
    val chainName = getChainName(balance.chainId)
    val address = EthAccountDelegate.address
    val shortAddress = "${address.take(6)}...${address.takeLast(4)}"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val cardBgColor = themedColor(
        darkColor = Color(0xFF1A1A1A),
        lightColor = Color(0xFFF3F3F3)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color = cardBgColor)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Token icon with chain badge
        Box {
            val iconModifier = Modifier
                .size(40.dp)
                .clip(CircleShape)

            if (balance.iconUrl != null) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(balance.iconUrl)
                        .size(40)
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
                        .background(Color(0xFF3396FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = balance.symbol.take(1),
                        style = WCTheme.typography.bodyLgMedium.copy(color = Color.White)
                    )
                }
            }

            // Chain badge overlay
            val chainIcon = getChainIcon(balance.chainId)
            if (chainIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(cardBgColor)
                        .padding(2.dp)
                ) {
                    Image(
                        painter = painterResource(id = chainIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${balance.quantity.numeric} ${balance.symbol}",
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
                )
            )
            Text(
                text = shortAddress,
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = 0xFF9A9A9A, lightColor = 0xFF6C6C6C)
                )
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
            contentDescription = "Copy address",
            modifier = Modifier
                .size(20.dp)
                .clickable {
                    clipboardManager.setText(AnnotatedString(address))
                    Toast.makeText(context, "$chainName address copied", Toast.LENGTH_SHORT).show()
                },
            tint = themedColor(
                darkColor = Color(0xFF788686),
                lightColor = Color(0xFF788686)
            )
        )
    }
}

@Composable
private fun EmptyWallets() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No token balances yet",
            style = WCTheme.typography.h6Regular.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Import a funded wallet or send tokens to this address.",
            style = WCTheme.typography.bodyLgRegular.copy(
                color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF9EA9A9)
            )
        )
    }
}
