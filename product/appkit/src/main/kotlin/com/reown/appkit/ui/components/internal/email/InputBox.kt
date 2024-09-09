package com.reown.appkit.ui.components.internal.email

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.appkit.ui.components.internal.commons.VerticalSpacer
import com.reown.appkit.ui.components.internal.commons.inputs.InputState
import com.reown.appkit.ui.theme.AppKitTheme

@Composable
internal fun InputValidationBox(
    inputState: InputState,
    errorMessage: String,
    errorAlign: TextAlign = TextAlign.Left,
    content: @Composable () -> Unit,
) {
    val hasError by inputState.hasError.collectAsState()

    Column(modifier = Modifier.animateContentSize()) {
        content()
        if (hasError) {
            VerticalSpacer(height = 4.dp)
            Text(
                text = errorMessage,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth(),
                style = AppKitTheme.typo.tiny400.copy(color = AppKitTheme.colors.error, textAlign = errorAlign)
            )
        }
    }
}

