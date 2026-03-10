package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.BuildConfig
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PosHeader(onBack = onClose)

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

        // Currency setting
        SettingsItem(label = "Currency", value = "USD")

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

        // SDK Version
        SettingsItem(label = "SDK Version", value = BuildConfig.BOM_VERSION)

        Spacer(Modifier.weight(1f))

        CloseButton(onClick = onClose)

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(WCTheme.spacing.spacing12)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundPrimary)
            .padding(horizontal = WCTheme.spacing.spacing4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary
        )
        Text(
            text = value,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textSecondary
        )
    }
}
