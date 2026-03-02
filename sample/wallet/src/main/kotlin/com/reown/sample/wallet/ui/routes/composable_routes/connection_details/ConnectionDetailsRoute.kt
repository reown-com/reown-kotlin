package com.reown.sample.wallet.ui.routes.composable_routes.connection_details

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.common.ChainIcons
import com.reown.sample.wallet.ui.common.formatDomain
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

    connectionUI?.let { uiConnection ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = themedColor(
                        darkColor = Color(0xFF1A1A1A),
                        lightColor = Color.White
                    ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(20.dp)
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = themedColor(
                                darkColor = Color.White,
                                lightColor = Color(0xFF202020)
                            )
                        )
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDisconnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = themedColor(
                                darkColor = Color(0xFF202020),
                                lightColor = Color.White
                            )
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_link_break),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = themedColor(
                                darkColor = Color(0xFF202020),
                                lightColor = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Disconnect",
                        style = WCTheme.typography.bodyMdRegular.copy(
                            color = themedColor(
                                darkColor = Color(0xFF202020),
                                lightColor = Color.White
                            )
                        )
                    )
                }

                // Close button
                ModalCloseButton(onClick = { navController.popBackStack() })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App info card
            AppInfoCard(uiConnection)

            Spacer(modifier = Modifier.height(8.dp))

            // Methods section
            val methods = getSessionItems(uiConnection) { it.methods }
            if (methods.isNotEmpty()) {
                InfoSection(title = "Methods", items = methods)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Events section
            val events = getSessionItems(uiConnection) { it.events }
            if (events.isNotEmpty()) {
                InfoSection(title = "Events", items = events)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    } ?: run {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connection not found",
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = Color(0xFF9A9A9A), lightColor = Color(0xFF9A9A9A))
                )
            )
        }
    }
}

@Composable
private fun AppInfoCard(connectionUI: ConnectionUI) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFF3F3F3)
                )
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        val iconModifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(12.dp),
                color = themedColor(
                    darkColor = Color(0xFF2A2A2A),
                    lightColor = Color(0xFFD0D0D0)
                )
            )

        if (connectionUI.icon?.isNotBlank() == true) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(connectionUI.icon)
                    .size(42)
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
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
                )
            )
            Text(
                text = formatDomain(connectionUI.uri),
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = 0xFF9A9A9A, lightColor = 0xFF9A9A9A)
                )
            )
        }

        // Chain icons
        ChainIcons(chainIds = getConnectionChainIds(connectionUI), size = 20)
    }
}

@Composable
private fun InfoSection(title: String, items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFF3F3F3)
                )
            )
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = WCTheme.typography.bodyLgRegular.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = items.joinToString(", "),
            style = WCTheme.typography.bodyMdRegular.copy(
                color = themedColor(darkColor = 0xFF9A9A9A, lightColor = 0xFF9A9A9A)
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
