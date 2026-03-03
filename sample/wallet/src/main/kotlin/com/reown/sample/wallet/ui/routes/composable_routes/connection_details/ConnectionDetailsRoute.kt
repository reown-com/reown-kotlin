package com.reown.sample.wallet.ui.routes.composable_routes.connection_details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scanner_options.ModalCloseButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.common.AppConnectionRow
import com.reown.sample.wallet.ui.routes.composable_routes.connected_apps.getConnectionChainIds
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionType
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionUI
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ConnectionDetailsRoute(navController: NavController, connectionId: Int?, connectionsViewModel: ConnectionsViewModel) {
    connectionsViewModel.currentConnectionId = connectionId
    val connectionUI by remember { connectionsViewModel.currentConnectionUI }
    var isDisconnecting by remember { mutableStateOf(false) }
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    connectionUI?.let { uiConnection ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.bgPrimary,
                    shape = RoundedCornerShape(topStart = borderRadius.radius8, topEnd = borderRadius.radius8)
                )
                .padding(spacing.spacing5)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar: Disconnect button + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Disconnect button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(borderRadius.radius3))
                        .background(color = colors.bgInvert)
                        .clickable(enabled = !isDisconnecting) {
                            disconnectSession(
                                uiConnection,
                                connectionsViewModel,
                                composableScope,
                                context,
                                navController,
                                onLoading = { isDisconnecting = it }
                            )
                        }
                        .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDisconnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(spacing.spacing4),
                            strokeWidth = spacing.spacing05,
                            color = colors.textInvert
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_link_break),
                            contentDescription = null,
                            modifier = Modifier.size(spacing.spacing4),
                            tint = colors.textInvert
                        )
                    }
                    Spacer(modifier = Modifier.width(spacing.spacing1 + spacing.spacing05))
                    Text(
                        text = "Disconnect",
                        style = WCTheme.typography.bodyMdRegular.copy(
                            color = colors.textInvert
                        )
                    )
                }

                // Close button
                ModalCloseButton(onClick = { navController.popBackStack() })
            }

            Spacer(modifier = Modifier.height(spacing.spacing5))

            // App info card
            AppInfoCard(uiConnection)

            Spacer(modifier = Modifier.height(spacing.spacing2))

            // Methods section
            val methods = getSessionItems(uiConnection) { it.methods }
            if (methods.isNotEmpty()) {
                InfoSection(title = "Methods", items = methods)
                Spacer(modifier = Modifier.height(spacing.spacing2))
            }

            // Events section
            val events = getSessionItems(uiConnection) { it.events }
            if (events.isNotEmpty()) {
                InfoSection(title = "Events", items = events)
            }

            Spacer(modifier = Modifier.height(spacing.spacing5))
        }
    } ?: run {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.spacing8),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connection not found",
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun AppInfoCard(connectionUI: ConnectionUI) {
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
            .clip(borderRadius.shapeXLarge)
            .background(color = colors.foregroundPrimary)
            .padding(spacing.spacing5)
    )
}

@Composable
private fun InfoSection(title: String, items: List<String>) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(borderRadius.shapeXLarge)
            .background(color = colors.foregroundPrimary)
            .padding(spacing.spacing5)
    ) {
        Text(
            text = title,
            style = WCTheme.typography.bodyLgRegular.copy(
                color = colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(spacing.spacing2))
        Text(
            text = items.joinToString(", "),
            style = WCTheme.typography.bodyMdRegular.copy(
                color = colors.textSecondary
            )
        )
    }
}

private fun getSessionItems(
    connectionUI: ConnectionUI,
    selector: (Wallet.Model.Namespace.Session) -> List<String>
): List<String> = when (val type = connectionUI.type) {
    is ConnectionType.Sign -> type.namespaces.values.flatMap(selector).distinct()
}

private fun disconnectSession(
    connectionUI: ConnectionUI,
    connectionsViewModel: ConnectionsViewModel,
    composableScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    navController: NavController,
    onLoading: (Boolean) -> Unit
) {
    when (connectionUI.type) {
        is ConnectionType.Sign -> {
            try {
                onLoading(true)
                WalletKit.disconnectSession(
                    Wallet.Params.SessionDisconnect(connectionUI.type.topic),
                    onSuccess = {
                        onLoading(false)
                        connectionsViewModel.refreshConnections()
                        composableScope.launch(Dispatchers.Main) {
                            navController.popBackStack()
                            Toast.makeText(context, "Session disconnected", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        onLoading(false)
                        connectionsViewModel.refreshConnections()
                        composableScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Disconnection error: ${error.throwable.message ?: "Unknown error"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                onLoading(false)
                connectionsViewModel.refreshConnections()
                composableScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Disconnection error: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
