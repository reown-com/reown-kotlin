package com.reown.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.pos.POSViewModel

@Composable
fun SelectNetworkScreen(
    viewModel: POSViewModel,
    modifier: Modifier = Modifier
) {
    val brandGreen = Color(0xFF0A8F5B)

    val networks = listOf(
        NetItem("Ethereum", "$2.50", Badge.Ethereum, "eip155:1"),
        NetItem("Base",     "$0.05", Badge.Base,     "eip155:8453"),
        NetItem("Polygon",  "$0.01", Badge.Polygon,  "eip155:137")
    )

    // ✅ store the selected CHAIN ID
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().imePadding()
    ) {
        // header ...
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brandGreen)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DTC Pay", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Crypto Payment Terminal", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.95f))
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Select Network", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Step 4: Choose blockchain network", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            networks.forEach { item ->
                NetworkCard(
                    item = item,
                    selected = selectedId == item.chainId,          // ✅ compare to chainId
                    onClick = { selectedId = item.chainId }         // ✅ set chainId
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { selectedId?.let { viewModel.createPaymentIntent(it) } },
                enabled = selectedId != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandGreen)
            ) {
                Text("Generate QR Code", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("Powered by DTC Pay", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/* ---------- UI Pieces ---------- */

private data class NetItem(val name: String, val fee: String, val badge: Badge, val chainId: String)
private enum class Badge { Ethereum, Base, Polygon }

@Composable
private fun NetworkCard(
    item: NetItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val green = Color(0xFF0A8F5B)

    val containerColor = if (selected) green else MaterialTheme.colorScheme.surface
    val contentColor   = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val feeColor       = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,          // ✅ flips default text/icon color
        tonalElevation = if (selected) 2.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetworkBadge(item.badge, tint = LocalContentColor.current) // ✅ uses contentColor
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Text("Fee: ${item.fee}", style = MaterialTheme.typography.bodyMedium, color = feeColor)
        }
    }
}

@Composable
private fun NetworkBadge(badge: Badge, tint: Color) {
    when (badge) {
        Badge.Ethereum -> Text("◇", style = MaterialTheme.typography.titleLarge, color = tint)
        Badge.Base -> Box(
            modifier = Modifier.size(24.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF4DA3FF), Color(0xFF0A51C2))), shape = CircleShape)
        )
        Badge.Polygon -> Box(
            modifier = Modifier.size(24.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFB16CEA), Color(0xFF6A00F4))), shape = CircleShape)
        )
    }
}