@file:JvmSynthetic

package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.walletconnect.sample.pos.ui.theme.WCTheme

@Composable
fun PinDialog(
    title: String,
    subtitle: String,
    onPinComplete: (String) -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null
) {
    var pin by remember(title) { mutableStateOf("") }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = WCTheme.spacing.spacing7)
                .clip(RoundedCornerShape(WCTheme.spacing.spacing5))
                .background(WCTheme.colors.foregroundPrimary)
                .padding(WCTheme.spacing.spacing5),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = WCTheme.typography.h6Regular,
                color = WCTheme.colors.textPrimary
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing1))

            Text(
                text = subtitle,
                style = WCTheme.typography.bodySmRegular,
                color = WCTheme.colors.textTertiary
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing5))

            // PIN dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(PIN_LENGTH) { index ->
                    val filled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .then(
                                if (filled) {
                                    Modifier.background(WCTheme.colors.iconAccentPrimary, CircleShape)
                                } else {
                                    Modifier
                                        .border(1.dp, WCTheme.colors.borderSecondary, CircleShape)
                                        .background(WCTheme.colors.foregroundPrimary, CircleShape)
                                }
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
                Text(
                    text = errorMessage,
                    style = WCTheme.typography.bodySmRegular,
                    color = WCTheme.colors.textError
                )
            }

            Spacer(Modifier.height(WCTheme.spacing.spacing5))

            // PIN keyboard
            PinKeyboard(
                onDigit = { digit ->
                    if (pin.length < PIN_LENGTH) {
                        pin += digit
                        if (pin.length == PIN_LENGTH) {
                            val completedPin = pin
                            pin = ""
                            onPinComplete(completedPin)
                        }
                    }
                },
                onBackspace = {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                    }
                }
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing4))

            // Cancel button
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(WCTheme.borderRadius.shapeMedium)
                    .background(WCTheme.colors.foregroundSecondary)
            ) {
                Text(
                    text = "Cancel",
                    style = WCTheme.typography.bodyLgRegular,
                    color = WCTheme.colors.textPrimary
                )
            }
        }
    }
}

private const val PIN_LENGTH = 4
