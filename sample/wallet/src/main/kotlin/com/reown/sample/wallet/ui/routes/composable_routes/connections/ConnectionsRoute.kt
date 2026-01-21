package com.reown.sample.wallet.ui.routes.composable_routes.connections

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.reown.sample.common.ui.TopBarActionImage
import com.reown.sample.common.ui.WCTopAppBar
import com.reown.sample.common.ui.findActivity
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.R
import com.reown.sample.wallet.blockchain.TokenBalance
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.ui.Web3WalletViewModel
import com.reown.sample.wallet.ui.routes.Route

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConnectionsRoute(navController: NavController, connectionsViewModel: ConnectionsViewModel, web3WalletViewModel: Web3WalletViewModel) {
    connectionsViewModel.refreshConnections()
    val connections by connectionsViewModel.connections.collectAsState(initial = emptyList())
    val usdcBalances by connectionsViewModel.usdcBalances.collectAsState()
    val isLoadingBalances by connectionsViewModel.isLoadingBalances.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoadingBalances,
        onRefresh = { connectionsViewModel.fetchUsdcBalances() }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Title(navController)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    UsdcBalanceSection(usdcBalances, isLoadingBalances)
                }
                item {
                    ConnectionsContent(connections) { connectionUI ->
                        navController.navigate("${Route.ConnectionDetails.path}/${connectionUI.id}")
                    }
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
fun UsdcBalanceSection(balances: List<TokenBalance>, isLoading: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    val address = EthAccountDelegate.address
    val shortAddress = "${address.take(6)}...${address.takeLast(4)}"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(shape)
            .background(
                color = themedColor(
                    darkColor = Color(0xFF1A1A1A),
                    lightColor = Color(0xFFF5F5F5)
                )
            )
            .border(
                1.dp,
                color = themedColor(
                    darkColor = Color(0xFF2A2A2A),
                    lightColor = Color(0xFFE0E0E0)
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "USDC Balances",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
                )
            )
            Text(
                text = shortAddress,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF3396FF)
                )
            }
        } else if (balances.isEmpty()) {
            Text(
                text = "No USDC balance found",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                )
            )
        } else {
            balances.forEach { balance ->
                UsdcBalanceItem(balance)
                if (balance != balances.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UsdcBalanceItem(balance: TokenBalance) {
    val chainName = when (balance.chainId) {
        "eip155:1" -> "Ethereum"
        "eip155:137" -> "Polygon"
        "eip155:8453" -> "Base"
        else -> balance.chainId
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFFFFFFF)
                )
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = chainName,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
                )
            )
            Text(
                text = "USDC",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                )
            )
        }
        Text(
            text = "${balance.quantity.numeric} USDC",
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF3396FF)
            )
        )
    }
}

@Composable
fun Title(navController: NavController) {
    WCTopAppBar(
        titleText = "Connections",
        TopBarActionImage(R.drawable.ic_copy) { navController.navigate(Route.PasteUri.path) },
        TopBarActionImage(R.drawable.ic_qr_code) { navController.navigate(Route.ScanUri.path) }
    )
}

@Composable
fun Connections(
    connections: List<ConnectionUI>,
    onClick: (ConnectionUI) -> Unit = {},
) {
    val modifier = Modifier.fillMaxHeight()
    if (connections.isEmpty()) {
        NoConnections(modifier)
    } else {
        ConnectionsLazyColumn(connections, modifier, onClick)
    }
}

@Composable
fun ConnectionsContent(
    connections: List<ConnectionUI>,
    onClick: (ConnectionUI) -> Unit = {},
) {
    if (connections.isEmpty()) {
        NoConnections(Modifier.fillMaxWidth().height(300.dp))
    } else {
        ConnectionsColumn(connections, onClick)
    }
}

@Composable
fun ConnectionsColumn(
    connections: List<ConnectionUI>,
    onClick: (ConnectionUI) -> Unit = {},
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 10.dp)
            .clip(shape = shape)
            .background(
                color = themedColor(
                    darkColor = Color(0xFFE4E4E7).copy(alpha = .06f),
                    lightColor = Color(0xFF505059).copy(.03f)
                )
            )
            .border(
                1.dp,
                color = themedColor(
                    darkColor = Color(0xFFE4E4E7).copy(alpha = .06f),
                    lightColor = Color(0xFF505059).copy(.03f)
                ),
                shape = shape
            )
            .padding(vertical = 1.dp, horizontal = 2.dp),
    ) {
        connections.forEach { connectionUI ->
            Spacer(modifier = Modifier.height(10.dp))
            Connection(connectionUI, onClick)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ConnectionsLazyColumn(
    connections: List<ConnectionUI>, modifier: Modifier, onClick: (ConnectionUI) -> Unit = {},
) {
    val shape = RoundedCornerShape(28.dp)
    LazyColumn(
        modifier = modifier
            .padding(vertical = 6.dp, horizontal = 10.dp)
            .clip(shape = shape)
            .background(
                color = themedColor(
                    darkColor = Color(0xFFE4E4E7).copy(alpha = .06f),
                    lightColor = Color(0xFF505059).copy(.03f)
                )
            )
            .border(
                1.dp,
                color = themedColor(
                    darkColor = Color(0xFFE4E4E7).copy(alpha = .06f),
                    lightColor = Color(0xFF505059).copy(.03f)
                ),
                shape = shape
            )
            .padding(vertical = 1.dp, horizontal = 2.dp),
    ) {
        connections.forEach { connectionUI ->
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Connection(connectionUI, onClick)
            }
        }
    }
}

@Composable
fun Connection(
    connectionUI: ConnectionUI, onClick: (ConnectionUI) -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick(connectionUI) }) {
        Spacer(modifier = Modifier.width(20.dp))

        val iconModifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                shape = CircleShape,
                color = themedColor(darkColor = Color(0xFF191919), lightColor = Color(0xFFE0E0E0))
            )
        if (connectionUI.icon?.isNotBlank() == true) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(connectionUI.icon)
                    .size(60)
                    .crossfade(true)
                    .error(com.reown.sample.common.R.drawable.ic_walletconnect_circle_blue)
                    .listener(
                        onSuccess = { request, metadata -> println("onSuccess: $request, $metadata") },
                        onError = { _, throwable ->
                            println("Error loading image: ${throwable.throwable.message}")
                        })
                    .build()
            )

            Image(
                painter = painter,
                contentDescription = "Connection image",
                modifier = iconModifier,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        } else {
            Icon(modifier = iconModifier.alpha(.7f), imageVector = ImageVector.vectorResource(id = R.drawable.sad_face), contentDescription = "Sad face")
        }

        Spacer(modifier = Modifier.width(10.dp))
        Column() {
            Text(text = connectionUI.name, style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)))
            Text(text = connectionUI.uri, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)))
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            tint = themedColor(darkColor = Color(0xFFe1e5e5), lightColor = Color(0xFF111111)),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_right),
            contentDescription = "Forward"
        )
        Spacer(modifier = Modifier.width(20.dp))

    }
}

@Composable
fun NoConnections(modifier: Modifier) {
    val contentColor = Color(if (isSystemInDarkTheme()) 0xFF585F5F else 0xFF9EA9A9)
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            tint = contentColor,
            imageVector = ImageVector.vectorResource(if (isSystemInDarkTheme()) R.drawable.no_connections_icon_dark else R.drawable.no_connections_icon_light), contentDescription = null
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Apps you connect with will appear here.", maxLines = 1, color = contentColor, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp))
        Row {
            Text(text = "To connect ", color = contentColor, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp))
            Icon(tint = contentColor, imageVector = ImageVector.vectorResource(id = R.drawable.ic_qr_code), contentDescription = "Scan QRCode Icon", modifier = Modifier.size(24.dp))
            Text(text = " scan or ", color = contentColor, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp))
            Icon(tint = contentColor, imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy), contentDescription = "Paste Icon", modifier = Modifier.size(24.dp))
            Text(text = " paste the code", color = contentColor, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp))
        }
        Text(text = "that’s displayed in the app.", maxLines = 1, color = contentColor, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp))
        Spacer(modifier = Modifier.weight(1f))
    }
}

