@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme

@Composable
fun MessageCard(
    message: String,
    showTitle: Boolean = true,
    title: String = "Message",
    maxHeight: Dp = 120.dp,
) {
    if (message.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(WCTheme.colors.foregroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(WCTheme.spacing.spacing5),
        verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        if (showTitle) {
            Text(
                text = title,
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = WCTheme.colors.textSecondary
                )
            )
        }
        Text(
            text = message,
            style = WCTheme.typography.bodyMdRegular.copy(
                color = WCTheme.colors.textPrimary
            )
        )
    }
}
