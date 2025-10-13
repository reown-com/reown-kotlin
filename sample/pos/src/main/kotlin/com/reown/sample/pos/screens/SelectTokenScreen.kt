package com.reown.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.pos.POSViewModel

@Composable
fun SelectTokenScreen(
    viewModel: POSViewModel,
    modifier: Modifier = Modifier
) {
    val brandGreen = Color(0xFF0A8F5B)
    var selectedTokenTitle by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // --- Green header ---
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

        // --- Content ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text("Select Token", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Step 3: Choose payment token",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // --- Single USDC card ---
            TokenCard(
                title = "USDC",
                subtitle = "USD Coin",
                selected = selectedTokenTitle == "USDC",
                onClick = {
                    selectedTokenTitle = if (selectedTokenTitle == "USDC") null else "USDC"
                }
            )

            // --- Single USDC card ---
            TokenCard(
                title = "USDT",
                subtitle = "USDT Coin",
                selected = selectedTokenTitle == "USDT",
                onClick = {
                    selectedTokenTitle = if (selectedTokenTitle == "USDT") null else "USDT"
                }
            )
            Spacer(Modifier.weight(1f))

            Button(
                onClick = { selectedTokenTitle?.let { viewModel.navigateToNetworkScreen(it) } },
                enabled = selectedTokenTitle != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandGreen)
            ) {
                Text("Select Network", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }

        // --- Footer strip ---
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Powered by DTC Pay", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TokenCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val container = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(16.dp)

    Surface(
        shape = shape,
        color = container,
        tonalElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = SolidColor(borderColor))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simple emoji as icon placeholder; swap for real asset later.
            Text("💵", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Text("✓", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}