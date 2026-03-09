package com.walletconnect.sample.pos.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reown.sample.common.ui.theme.WCTheme

@Composable
fun BigAmountDisplay(
    currencySymbol: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currencySymbol,
            style = WCTheme.typography.h1Regular,
            color = if (amount.isEmpty()) WCTheme.colors.textTertiary else WCTheme.colors.textPrimary
        )
        Text(
            text = amount.ifEmpty { "0.00" },
            style = WCTheme.typography.h1Regular,
            color = if (amount.isEmpty()) WCTheme.colors.textTertiary else WCTheme.colors.textPrimary
        )
    }
}
