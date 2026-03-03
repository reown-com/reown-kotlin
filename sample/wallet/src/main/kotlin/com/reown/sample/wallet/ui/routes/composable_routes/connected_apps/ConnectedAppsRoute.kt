package com.reown.sample.wallet.ui.routes.composable_routes.connected_apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.ui.common.AppConnectionRow
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
            val spacing = WCTheme.spacing
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.spacing3),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
            ) {
                item { Spacer(modifier = Modifier.height(spacing.spacing1)) }
                items(connections) { connectionUI ->
                    ConnectedAppItem(connectionUI) {
                        navController.navigate("${Route.ConnectionDetails.path}/${connectionUI.id}")
                    }
                }
                item { Spacer(modifier = Modifier.height(spacing.spacing2)) }
            }
        }
    }
}

@Composable
fun ConnectedAppItem(
    connectionUI: ConnectionUI,
    onClick: () -> Unit,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    AppConnectionRow(
        iconUrl = connectionUI.icon,
        name = connectionUI.name,
        uri = connectionUI.uri,
        chainIds = getConnectionChainIds(connectionUI),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(color = colors.foregroundPrimary)
            .clickable(onClick = onClick)
            .padding(spacing.spacing3)
    )
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
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No connected apps yet",
            style = WCTheme.typography.h6Regular.copy(
                color = colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(spacing.spacing1))
        Text(
            text = "Scan a WalletConnect QR code to get started.",
            style = WCTheme.typography.bodyLgRegular.copy(
                color = colors.textSecondary
            )
        )
    }
}
