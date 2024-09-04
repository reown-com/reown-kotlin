package com.walletconnect.web3.modal.ui.components.internal.commons.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.walletconnect.web3.modal.ui.theme.AppKitTheme

@Composable
internal fun BaseTextInput(
    inputState: InputState,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isEnabled: Boolean = true,
    content: @Composable (innerTextField: @Composable () -> Unit, inputData: InputData) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val borderColor: Color
    val backgroundColor: Color
    val state by inputState.state.collectAsState()

    when {
        state.isFocused -> {
            borderColor = AppKitTheme.colors.accent100
            backgroundColor = AppKitTheme.colors.grayGlass10
        }

        isEnabled -> {
            borderColor = AppKitTheme.colors.grayGlass05
            backgroundColor = AppKitTheme.colors.grayGlass05
        }

        else -> {
            borderColor = AppKitTheme.colors.grayGlass10
            backgroundColor = AppKitTheme.colors.grayGlass15
        }
    }

    BasicTextField(value = state.text,
        onValueChange = inputState::onTextChange,
        textStyle = AppKitTheme.typo.paragraph400.copy(color = AppKitTheme.colors.foreground.color100),
        cursorBrush = SolidColor(AppKitTheme.colors.accent100),
        singleLine = true,
        keyboardActions = KeyboardActions { inputState.submit(state.text) },
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .onFocusChanged { inputState.onFocusChange(it.hasFocus) }
            .focusRequester(focusRequester),
        decorationBox = {
            content(it, state)
        })
}
