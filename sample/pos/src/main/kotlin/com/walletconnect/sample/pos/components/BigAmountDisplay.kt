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
    val primary = WCTheme.colors.textPrimary
    val tertiary = WCTheme.colors.textTertiary

    val dotIndex = amount.indexOf('.')
    // Determine how many decimals the user has typed after the dot
    val decimalCount = if (dotIndex >= 0) amount.length - dotIndex - 1 else -1
    // LEFT symbol ($): always primary except placeholder
    // RIGHT symbol (€): primary only when all decimals filled
    val leftSymbolColor = if (amount.isEmpty()) tertiary else primary
    val rightSymbolColor = when {
        amount.isEmpty() -> tertiary
        decimalCount >= 2 -> primary
        dotIndex >= 0 -> tertiary
        else -> primary
    }

    val displayText = amount.ifEmpty { "0.00" }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (symbolPosition == SymbolPosition.LEFT) {
            Text(
                text = currencySymbol,
                style = WCTheme.typography.h1Medium,
                color = leftSymbolColor
            )
        }
        if (amount.isEmpty()) {
            Text(
                text = displayText,
                style = WCTheme.typography.h1Medium,
                color = tertiary
            )
        } else if (dotIndex < 0) {
            // No decimal separator yet — whole part is primary
            Text(
                text = displayText,
                style = WCTheme.typography.h1Medium,
                color = primary
            )
        } else {
            // Has decimal separator — color each part accordingly
            val wholePart = amount.substring(0, dotIndex + 1) // includes dot
            val typedDecimals = amount.substring(dotIndex + 1)
            val paddedDecimals = typedDecimals.padEnd(2, '0')
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = primary)) {
                        append(wholePart)
                    }
                    // First decimal
                    withStyle(SpanStyle(color = if (typedDecimals.isNotEmpty()) primary else tertiary)) {
                        append(paddedDecimals[0].toString())
                    }
                    // Second decimal
                    withStyle(SpanStyle(color = if (typedDecimals.length >= 2) primary else tertiary)) {
                        append(paddedDecimals[1].toString())
                    }
                },
                style = WCTheme.typography.h1Medium
            )
        }
        if (symbolPosition == SymbolPosition.RIGHT) {
            Text(
                text = currencySymbol,
                style = WCTheme.typography.h1Medium,
                color = rightSymbolColor
            )
        }
    }
}
