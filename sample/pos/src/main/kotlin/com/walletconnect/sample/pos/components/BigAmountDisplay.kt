package com.walletconnect.sample.pos.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.model.SymbolPosition

@Composable
fun BigAmountDisplay(
    currencySymbol: String,
    amount: String,
    symbolPosition: SymbolPosition = SymbolPosition.LEFT,
    modifier: Modifier = Modifier
) {
    val isEmpty = amount.isEmpty()
    val primaryColor = if (isEmpty) WCTheme.colors.textTertiary else WCTheme.colors.textPrimary
    val decimalColor = WCTheme.colors.textTertiary
    val displayText = amount.ifEmpty { "0.00" }
    val dotIndex = displayText.indexOf('.')

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (symbolPosition == SymbolPosition.LEFT) {
            Text(
                text = currencySymbol,
                style = WCTheme.typography.h1Medium,
                color = primaryColor
            )
        }
        if (dotIndex >= 0) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = primaryColor)) {
                        append(displayText.substring(0, dotIndex + 1))
                    }
                    withStyle(SpanStyle(color = decimalColor)) {
                        append(displayText.substring(dotIndex + 1))
                    }
                },
                style = WCTheme.typography.h1Medium
            )
        } else {
            Text(
                text = displayText,
                style = WCTheme.typography.h1Medium,
                color = primaryColor
            )
        }
        if (symbolPosition == SymbolPosition.RIGHT) {
            Text(
                text = currencySymbol,
                style = WCTheme.typography.h1Medium,
                color = primaryColor
            )
        }
    }
}
