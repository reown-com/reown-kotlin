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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.ThemeManager
import com.reown.sample.wallet.domain.account.SmartAccountEnabler
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.ui.routes.host.WalletHeader

private data class SettingsSection(val title: String, val items: List<SettingItem>)
private data class SettingItem(val key: String, val value: String)

@Composable
fun SettingsRoute(navController: NavHostController) {
    val viewModel: SettingsViewModel = viewModel()
    val deviceToken = viewModel.deviceToken.collectAsState().value
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val sections = listOf(
        SettingsSection(
            "EIP155 Account", listOf(
                SettingItem("CAIP-10", viewModel.caip10),
                SettingItem("Private key", viewModel.privateKey),
            )
        ),
        SettingsSection(
            "TON Account", listOf(
                SettingItem("Friendly address", TONAccountDelegate.addressFriendly),
                SettingItem("Secret key", TONAccountDelegate.secretKey),
                SettingItem("Public key", TONAccountDelegate.publicKey),
            )
        ),
        SettingsSection(
            "Solana Account", listOf(
                SettingItem("Keypair", SolanaAccountDelegate.keyPair),
                SettingItem("Public key", SolanaAccountDelegate.keys.second),
            )
        ),
        SettingsSection(
            "Stacks Account", listOf(
                SettingItem("Wallet", StacksAccountDelegate.importedWallet),
                SettingItem("Address Mainnet", StacksAccountDelegate.mainnetAddress),
                SettingItem("Address Testnet", StacksAccountDelegate.testnetAddress),
            )
        ),
        SettingsSection(
            "SUI Account", listOf(
                SettingItem("Address", SuiAccountDelegate.address),
                SettingItem("Key pair", SuiAccountDelegate.keypair),
                SettingItem("Public key", SuiAccountDelegate.publicKey),
            )
        ),
        SettingsSection(
            "Tron Account", listOf(
                SettingItem("Address", TronAccountDelegate.address),
                SettingItem("Secret key", TronAccountDelegate.secretKey),
                SettingItem("Public key", TronAccountDelegate.publicKey),
            )
        ),
        SettingsSection(
            "Device", listOf(
                SettingItem("Client ID", viewModel.clientId),
                SettingItem("Device token", deviceToken),
                SettingItem("App Version", BuildConfig.VERSION_NAME),
            )
        ),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        WalletHeader(navController)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Theme toggle
            item { ThemeToggleCard() }

            // Smart Account toggle
            item { SmartAccountToggleCard() }

            // Account sections
            items(sections) { section ->
                AccountSectionCard(
                    section = section,
                    onCopy = { value ->
                        clipboardManager.setText(AnnotatedString(value))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ThemeToggleCard() {
    val themeMode by ThemeManager.themeMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF1A1A1A),
                    lightColor = Color(0xFFF5F5F5)
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Theme",
            style = WCTheme.typography.bodyMdMedium.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    color = themedColor(
                        darkColor = Color(0xFF252525),
                        lightColor = Color(0xFFE0E0E0)
                    )
                )
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val options = listOf("Light" to 0, "Dark" to 1, "System" to -1)
            options.forEach { (label, mode) ->
                val isSelected = themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) themedColor(
                                darkColor = Color(0xFF3A3A3A),
                                lightColor = Color.White
                            ) else Color.Transparent
                        )
                        .clickable {
                            if (mode == -1) ThemeManager.setFollowSystem()
                            else ThemeManager.setDarkMode(mode == 1)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = if (isSelected) {
                            WCTheme.typography.bodyMdMedium.copy(
                                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
                            )
                        } else {
                            WCTheme.typography.bodyMdRegular.copy(
                                color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
                            )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Safe Smart Account",
            style = WCTheme.typography.bodyMdMedium.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
            )
        )
        Switch(
            checked = isSafeEnabled,
            onCheckedChange = { SmartAccountEnabler.enableSmartAccount(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF3396FF),
                checkedTrackColor = Color(0xFF3396FF).copy(alpha = 0.5f),
                uncheckedThumbColor = WCTheme.colors.foregroundTertiary
            )
        )
    }
}

@Composable
private fun AccountSectionCard(
    section: SettingsSection,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF1A1A1A),
                    lightColor = Color(0xFFF5F5F5)
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = section.title,
            style = WCTheme.typography.bodyMdMedium.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
            )
        )

        section.items.forEach { item ->
            CopyableItemCard(item, onCopy)
        }
    }
}

@Composable
private fun CopyableItemCard(
    item: SettingItem,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFFFFFFF)
                )
            )
            .clickable { onCopy(item.value) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.key,
                style = WCTheme.typography.bodySmMedium.copy(
                    color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF141414)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_copy_small),
                contentDescription = "Copy",
                tint = themedColor(
                    darkColor = Color(0xFF788686),
                    lightColor = Color(0xFF788686)
                )
            )
        }
        Text(
            text = item.value,
            style = WCTheme.typography.bodySmRegular.copy(
                color = themedColor(darkColor = 0xFF788686, lightColor = 0xFF788686)
            )
        )
    }
}
