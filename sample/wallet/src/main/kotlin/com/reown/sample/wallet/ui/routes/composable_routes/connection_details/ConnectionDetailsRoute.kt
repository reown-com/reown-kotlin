package com.reown.sample.wallet.ui.routes.composable_routes.connection_details

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionType
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionUI
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.routes.composable_routes.connected_apps.chainInfo
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
                        lightColor = Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(16.dp)
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
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            color = themedColor(
                                darkColor = Color(0xFF2A2A2A),
                                lightColor = Color(0xFFE8E8E8)
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDisconnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = themedColor(
                                darkColor = Color(0xFFe3e7e7),
                                lightColor = Color(0xFF141414)
                            )
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_link_break),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = themedColor(
                                darkColor = Color(0xFFe3e7e7),
                                lightColor = Color(0xFF141414)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Disconnect",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = themedColor(
                                darkColor = Color(0xFFe3e7e7),
                                lightColor = Color(0xFF141414)
                            )
                        )
                    )
                }

                // Close button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            color = themedColor(
                                darkColor = Color(0xFF2A2A2A),
                                lightColor = Color(0xFFE8E8E8)
                            )
                        )
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = themedColor(
                            darkColor = Color(0xFFe3e7e7),
                            lightColor = Color(0xFF141414)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App info card
            AppInfoCard(uiConnection)

            Spacer(modifier = Modifier.height(12.dp))

            // Methods section
            val methods = getSessionMethods(uiConnection)
            if (methods.isNotEmpty()) {
                InfoSection(title = "Methods", items = methods)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Events section
            val events = getSessionEvents(uiConnection)
            if (events.isNotEmpty()) {
                InfoSection(title = "Events", items = events)
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                color = themedColor(darkColor = Color(0xFF788686), lightColor = Color(0xFF788686))
            )
        }
    }
}

@Composable
private fun AppInfoCard(connectionUI: ConnectionUI) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFFFFFFF)
                )
            )
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
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
                )
            )
            Text(
                text = connectionUI.uri,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                )
            )
        }

        // Chain icons
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
                        .border(
                            1.5.dp,
                            themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFFFFFFFF)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
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
                        .background(
                            themedColor(darkColor = Color(0xFF3A3A3A), lightColor = Color(0xFFBBBBBB))
                        )
                        .border(
                            1.5.dp,
                            themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFFFFFFFF)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${chains.size - 4}",
                        style = TextStyle(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoSection(title: String, items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFFFFFFF)
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = items.joinToString(", "),
            style = TextStyle(
                fontSize = 13.sp,
                color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
            )
        )
    }
}

private fun getSessionMethods(connectionUI: ConnectionUI): List<String> {
    return when (val type = connectionUI.type) {
        is ConnectionType.Sign -> {
            type.namespaces.values
                .flatMap { session -> session.methods }
                .distinct()
        }
    }
}

private fun getSessionEvents(connectionUI: ConnectionUI): List<String> {
    return when (val type = connectionUI.type) {
        is ConnectionType.Sign -> {
            type.namespaces.values
                .flatMap { session -> session.events }
                .distinct()
        }
    }
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
