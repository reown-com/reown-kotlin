@file:JvmSynthetic

package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCTheme

@Composable
fun EditSettingBottomSheet(
    title: String,
    currentValue: String,
    isSecret: Boolean = false,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember(title) { mutableStateOf(currentValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        BottomSheetHeader(title = title, onDismiss = onDismiss)

        Spacer(Modifier.height(WCTheme.spacing.spacing5))

        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            textStyle = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textPrimary),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = WCTheme.colors.foregroundPrimary,
                cursorColor = WCTheme.colors.iconAccentPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(WCTheme.borderRadius.shapeMedium)
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing4))

        TextButton(
            onClick = { onSave(textValue) },
            enabled = textValue.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(WCTheme.borderRadius.shapeMedium)
                .background(
                    if (textValue.isNotBlank()) WCTheme.colors.bgAccentPrimary
                    else WCTheme.colors.bgAccentPrimary.copy(alpha = 0.5f)
                )
        ) {
            Text(
                text = "Save",
                style = WCTheme.typography.bodyLgMedium,
                color = WCTheme.colors.textInvert
            )
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}
