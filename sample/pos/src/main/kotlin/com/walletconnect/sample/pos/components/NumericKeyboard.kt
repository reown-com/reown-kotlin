package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCTheme

@Composable
fun NumericKeyboard(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = WCTheme.spacing.spacing2
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        onClick = {
                            when (key) {
                                "." -> onDecimal()
                                "⌫" -> onBackspace()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = WCTheme.colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = key,
                style = WCTheme.typography.h5Regular,
                color = WCTheme.colors.textPrimary
            )
        }
    }
}
