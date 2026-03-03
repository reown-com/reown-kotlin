@file:JvmSynthetic

package com.reown.sample.wallet.ui.routes.composable_routes.secret_keys

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.ui.routes.CopyableItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecretKeysRoute(navController: NavHostController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val borderRadius = WCTheme.borderRadius
    val onCopy: (String) -> Unit = { value ->
        clipboardManager.setText(AnnotatedString(value))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.spacing1, vertical = spacing.spacing2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = "Back",
                    tint = colors.iconDefault
                )
            }
            Text(
                text = "Secret Keys & Phrases",
                style = WCTheme.typography.h6Medium.copy(color = colors.textPrimary)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(borderRadius.radius3))
                        .background(colors.bgWarning)
                        .padding(spacing.spacing3)
                ) {
                    Text(
                        text = "Secret keys are for development purposes only and should not be used elsewhere",
                        style = WCTheme.typography.bodySmMedium.copy(color = colors.textWarning)
                    )
                }
            }

            item { EvmSecretSection(onCopy = onCopy) }

            item {
                SecretSectionCard(
                    title = "TON Account",
                    items = listOf(
                        "Friendly address" to TONAccountDelegate.addressFriendly,
                        "Secret key" to TONAccountDelegate.secretKey,
                        "Public key" to TONAccountDelegate.publicKey,
                    ),
                    onCopy = onCopy
                )
            }

            item {
                SecretSectionCard(
                    title = "Solana Account",
                    items = listOf(
                        "Keypair" to SolanaAccountDelegate.keyPair,
                        "Public key" to SolanaAccountDelegate.keys.second,
                    ),
                    onCopy = onCopy
                )
            }

            item {
                SecretSectionCard(
                    title = "Stacks Account",
                    items = listOf(
                        "Wallet" to StacksAccountDelegate.importedWallet,
                        "Address Mainnet" to StacksAccountDelegate.mainnetAddress,
                        "Address Testnet" to StacksAccountDelegate.testnetAddress,
                    ),
                    onCopy = onCopy
                )
            }

            item {
                SecretSectionCard(
                    title = "SUI Account",
                    items = listOf(
                        "Address" to SuiAccountDelegate.address,
                        "Key pair" to SuiAccountDelegate.keypair,
                        "Public key" to SuiAccountDelegate.publicKey,
                    ),
                    onCopy = onCopy
                )
            }

            item {
                SecretSectionCard(
                    title = "Tron Account",
                    items = listOf(
                        "Address" to TronAccountDelegate.address,
                        "Secret key" to TronAccountDelegate.secretKey,
                        "Public key" to TronAccountDelegate.publicKey,
                    ),
                    onCopy = onCopy
                )
            }

            item { Spacer(modifier = Modifier.height(spacing.spacing4)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EvmSecretSection(onCopy: (String) -> Unit) {
    val mnemonic = EthAccountDelegate.mnemonic
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
            text = "EIP155 Account",
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )

        if (mnemonic != null) {
            Text(
                text = "Recovery phrase",
                style = WCTheme.typography.bodySmMedium.copy(color = colors.textSecondary)
            )

            val words = mnemonic.split(" ")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1)
            ) {
                words.forEachIndexed { index, word ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(borderRadius.radius2))
                            .background(color = colors.foregroundPrimary)
                            .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}.",
                                style = WCTheme.typography.bodySmRegular.copy(color = colors.textSecondary)
                            )
                            Spacer(modifier = Modifier.width(spacing.spacing1))
                            Text(
                                text = word,
                                style = WCTheme.typography.bodySmMedium.copy(color = colors.textPrimary)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(borderRadius.radius3))
                    .background(color = colors.foregroundPrimary)
                    .clickable { onCopy(mnemonic) }
                    .padding(spacing.spacing3),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Copy recovery phrase",
                    style = WCTheme.typography.bodySmMedium.copy(color = colors.textPrimary)
                )
                Spacer(modifier = Modifier.width(spacing.spacing2))
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy_small),
                    contentDescription = "Copy",
                    tint = colors.iconDefault
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(borderRadius.radius3))
                    .background(color = colors.foregroundPrimary)
                    .padding(spacing.spacing3)
            ) {
                Text(
                    text = "Imported via private key - no recovery phrase",
                    style = WCTheme.typography.bodySmRegular.copy(color = colors.textSecondary)
                )
            }
        }

        CopyableItem(key = "CAIP-10", value = EthAccountDelegate.ethAccount, onCopy = onCopy)
        CopyableItem(key = "Private key", value = EthAccountDelegate.privateKey, onCopy = onCopy)
    }
}

@Composable
private fun SecretSectionCard(
    title: String,
    items: List<Pair<String, String>>,
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
            text = title,
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )

        items.forEach { (key, value) ->
            CopyableItem(key = key, value = value, onCopy = onCopy)
        }
    }
}
