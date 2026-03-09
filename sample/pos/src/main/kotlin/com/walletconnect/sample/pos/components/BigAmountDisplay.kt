package com.walletconnect.sample.pos.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.model.SymbolPosition

@Composable
fun BigAmountDisplay(
    currencySymbol: String,
    amount: String,
    symbolPosition: SymbolPosition = SymbolPosition.LEFT,
    modifier: Modifier = Modifier
) {
    val color = if (amount.isEmpty()) WCTheme.colors.textTertiary else WCTheme.colors.textPrimary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (symbolPosition == SymbolPosition.LEFT) {
            Text(
                text = currencySymbol,
                style = WCTheme.typography.h1Regular,
                color = color
            )
        }
        Text(
            text = amount.ifEmpty { "0.00" },
            style = WCTheme.typography.h1Regular,
            color = color
        )
        if (symbolPosition == SymbolPosition.RIGHT) {
            Text(
                text = currencySymbol,
                style = WCTheme.typography.h1Regular,
                color = color
            )
        }
    }
}
