package com.reown.sample.wallet.ui.routes.composable_routes.connected_apps

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
import androidx.compose.material.Icon
import androidx.compose.material.Text
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
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.KhTekaFontFamily
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionType
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionUI
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.routes.host.WalletHeader

@Composable
fun ConnectedAppsRoute(
    navController: NavController,
    connectionsViewModel: ConnectionsViewModel,
) {
    connectionsViewModel.refreshConnections()
    val connections by connectionsViewModel.connections.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        WalletHeader(navController)

        if (connections.isEmpty()) {
            EmptyConnectedApps()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(connections) { connectionUI ->
                    ConnectedAppItem(connectionUI) {
                        navController.navigate("${Route.ConnectionDetails.path}/${connectionUI.id}")
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun ConnectedAppItem(
    connectionUI: ConnectionUI,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF1A1A1A),
                    lightColor = Color(0xFFF5F5F5)
                )
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        val iconModifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(12.dp),
                color = themedColor(
                    darkColor = Color(0xFF2A2A2A),
                    lightColor = Color(0xFFE0E0E0)
                )
            )

        if (connectionUI.icon?.isNotBlank() == true) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(connectionUI.icon)
                    .size(44)
                    .crossfade(true)
                    .error(com.reown.sample.common.R.drawable.ic_walletconnect_circle_blue)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = "${connectionUI.name} icon",
                modifier = iconModifier,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        } else {
            Icon(
                modifier = iconModifier.alpha(.7f),
                imageVector = ImageVector.vectorResource(id = R.drawable.sad_face),
                contentDescription = "No icon"
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connectionUI.name,
                style = WCTheme.typography.bodyLgMedium.copy(
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
                )
            )
            Text(
                text = connectionUI.uri,
                style = WCTheme.typography.bodySmRegular.copy(
                    color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                )
            )
        }

        // Chain icons
        ChainIcons(connectionUI)
    }
}

@Composable
private fun ChainIcons(connectionUI: ConnectionUI) {
    val chains = when (val type = connectionUI.type) {
        is ConnectionType.Sign -> {
            type.namespaces.values
                .flatMap { session -> session.accounts }
                .map { account ->
                    val parts = account.split(":")
                    if (parts.size >= 2) "${parts[0]}:${parts[1]}" else account
                }
                .distinct()
        }
    }

    if (chains.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy((-6).dp)
        ) {
            chains.take(4).forEach { chainId ->
                val (color, label) = chainInfo(chainId)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.5.dp, themedColor(darkColor = Color(0xFF1A1A1A), lightColor = Color(0xFFF5F5F5)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontFamily = KhTekaFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
            if (chains.size > 4) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(themedColor(darkColor = Color(0xFF3A3A3A), lightColor = Color(0xFFBBBBBB)))
                        .border(1.5.dp, themedColor(darkColor = Color(0xFF1A1A1A), lightColor = Color(0xFFF5F5F5)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${chains.size - 4}",
                        style = TextStyle(
                            fontFamily = KhTekaFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

fun chainInfo(chainId: String): Pair<Color, String> {
    return when (chainId) {
        "eip155:1" -> Color(0xFF627EEA) to "E"
        "eip155:137" -> Color(0xFF8247E5) to "P"
        "eip155:8453" -> Color(0xFF0052FF) to "B"
        "eip155:10" -> Color(0xFFFF0420) to "O"
        "eip155:42161" -> Color(0xFF28A0F0) to "A"
        "eip155:56" -> Color(0xFFF0B90B) to "B"
        "eip155:43114" -> Color(0xFFE84142) to "Av"
        "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp" -> Color(0xFF9945FF) to "S"
        else -> Color(0xFF666666) to chainId.takeLast(2)
    }
}

@Composable
private fun EmptyConnectedApps() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No connected apps yet",
            style = WCTheme.typography.h6Regular.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Scan a WalletConnect QR code to get started.",
            style = WCTheme.typography.bodyLgRegular.copy(
                color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF9EA9A9)
            )
        )
    }
}
