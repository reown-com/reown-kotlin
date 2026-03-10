package com.reown.sample.wallet.ui.routes.composable_routes.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.ThemeManager
import com.reown.sample.wallet.domain.account.SmartAccountEnabler
import com.reown.sample.wallet.ui.routes.CopyableItem
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.host.WalletHeader

@Composable
fun SettingsRoute(navController: NavHostController) {
    val viewModel: SettingsViewModel = viewModel()
    val deviceToken = viewModel.deviceToken.collectAsState().value
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val spacing = WCTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        WalletHeader(navController)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
        ) {
            item { Spacer(modifier = Modifier.height(spacing.spacing1)) }

            item { ThemeToggleCard() }
            item { SmartAccountToggleCard() }

            item {
                NavigationCard(
                    title = "Secret Keys & Phrases",
                    onClick = { navController.navigate(Route.SecretKeysAndPhrases.path) }
                )
            }

            item {
                NavigationCard(
                    title = "Import Wallet",
                    onClick = { navController.navigate(Route.ImportWallet.path) }
                )
            }

            item {
                DeviceSectionCard(
                    clientId = viewModel.clientId,
                    deviceToken = deviceToken,
                    appVersion = BuildConfig.VERSION_NAME,
                    onCopy = { value ->
                        clipboardManager.setText(AnnotatedString(value))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(spacing.spacing2)) }
        }
    }
}

@Composable
private fun NavigationCard(title: String, onClick: () -> Unit) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(color = colors.foregroundSecondary)
            .clickable { onClick() }
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.iconDefault
        )
    }
}

@Composable
private fun DeviceSectionCard(
    clientId: String,
    deviceToken: String,
    appVersion: String,
    onCopy: (String) -> Unit,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(color = colors.foregroundSecondary)
            .padding(spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3)
    ) {
        Text(
            text = "Device",
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )

        CopyableItem(key = "Client ID", value = clientId, onCopy = onCopy)
        CopyableItem(key = "Device token", value = deviceToken, onCopy = onCopy)
        CopyableItem(key = "App Version", value = appVersion, onCopy = onCopy)
    }
}

@Composable
private fun ThemeToggleCard() {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(color = colors.foregroundSecondary)
            .padding(spacing.spacing4)
    ) {
        Text(
            text = "Theme",
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )
        Spacer(modifier = Modifier.height(spacing.spacing3))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(borderRadius.radius3))
                .background(color = colors.foregroundPrimary)
                .padding(spacing.spacing1),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)
        ) {
            val options = listOf("Light" to 0, "Dark" to 1, "System" to -1)
            options.forEach { (label, mode) ->
                val isSelected = themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(borderRadius.radius2))
                        .background(
                            if (isSelected) colors.foregroundTertiary else Color.Transparent
                        )
                        .clickable {
                            if (mode == -1) ThemeManager.setFollowSystem()
                            else ThemeManager.setDarkMode(mode == 1)
                        }
                        .padding(vertical = spacing.spacing2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = if (isSelected) {
                            WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
                        } else {
                            WCTheme.typography.bodyMdRegular.copy(color = colors.textSecondary)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartAccountToggleCard() {
    val isSafeEnabled by SmartAccountEnabler.isSmartAccountEnabled.collectAsState()
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(color = colors.foregroundSecondary)
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Safe Smart Account",
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )
        Switch(
            checked = isSafeEnabled,
            onCheckedChange = { SmartAccountEnabler.enableSmartAccount(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.bgAccentPrimary,
                checkedTrackColor = colors.foregroundAccentPrimary40,
                uncheckedThumbColor = colors.foregroundTertiary
            )
        )
    }
}
