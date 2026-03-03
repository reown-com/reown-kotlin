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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
    LaunchedEffect(Unit) {
        connectionsViewModel.refreshConnections()
    }
    val connections by connectionsViewModel.connections.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        WalletHeader(navController)

        if (connections.isEmpty()) {
            EmptyConnectedApps()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = WCTheme.spacing.spacing3),
                verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
            ) {
                item { Spacer(modifier = Modifier.height(WCTheme.spacing.spacing1)) }
                items(connections) { connectionUI ->
                    ConnectedAppItem(connectionUI) {
                        navController.navigate("${Route.ConnectionDetails.path}/${connectionUI.id}")
                    }
                }
                item { Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2)) }
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
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius4))
            .background(color = WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick)
            .padding(WCTheme.spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        val iconModifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius3))
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(WCTheme.borderRadius.radius3),
                color = WCTheme.colors.foregroundSecondary
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

        Spacer(modifier = Modifier.width(WCTheme.spacing.spacing3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connectionUI.name,
                style = WCTheme.typography.bodyLgMedium.copy(
                    color = WCTheme.colors.textPrimary
                )
            )
            Text(
                text = formatDomain(connectionUI.uri),
                style = WCTheme.typography.bodySmRegular.copy(
                    color = WCTheme.colors.textSecondary
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
                color = WCTheme.colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing1))
        Text(
            text = "Scan a WalletConnect QR code to get started.",
            style = WCTheme.typography.bodyLgRegular.copy(
                color = WCTheme.colors.textSecondary
            )
        )
    }
}
