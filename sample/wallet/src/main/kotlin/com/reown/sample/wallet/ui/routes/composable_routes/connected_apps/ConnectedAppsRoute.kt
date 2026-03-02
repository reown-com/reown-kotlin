package com.reown.sample.wallet.ui.routes.composable_routes.connected_apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.common.ChainIcons
import com.reown.sample.wallet.ui.common.formatDomain
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
                text = formatDomain(connectionUI.uri),
                style = WCTheme.typography.bodySmRegular.copy(
                    color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                )
            )
        }

        // Chain icons
        ChainIcons(chainIds = getConnectionChainIds(connectionUI), size = 20)
    }
}

internal fun getConnectionChainIds(connectionUI: ConnectionUI): List<String> {
    return when (val type = connectionUI.type) {
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
