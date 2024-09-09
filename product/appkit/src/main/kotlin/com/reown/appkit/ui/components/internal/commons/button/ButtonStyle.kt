package com.reown.appkit.ui.components.internal.commons.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.reown.appkit.ui.theme.AppKitTheme

internal data class ButtonData(
    val size: ButtonSize,
    val style: ButtonStyle,
    val textStyle: TextStyle,
    val tint: Color,
    val background: Color
)

internal enum class ButtonStyle { MAIN, ACCENT, SHADE, LOADING, ACCOUNT, LINK }

internal enum class ButtonSize { M, S, ACCOUNT_M, ACCOUNT_S }

@Composable
internal fun ButtonSize.getTextStyle() = when (this) {
    ButtonSize.M, ButtonSize.ACCOUNT_M, ButtonSize.ACCOUNT_S -> AppKitTheme.typo.paragraph600
    ButtonSize.S -> AppKitTheme.typo.small600
}

@Composable
internal fun ButtonSize.getContentPadding() = when (this) {
    ButtonSize.M -> PaddingValues(horizontal = 16.dp)
    ButtonSize.S -> PaddingValues(horizontal = 12.dp)
    ButtonSize.ACCOUNT_M -> PaddingValues(start = 8.dp, end = 12.dp)
    ButtonSize.ACCOUNT_S -> PaddingValues(start = 6.dp, end = 12.dp)
}

@Composable
internal fun ButtonSize.getHeight() = when (this) {
    ButtonSize.M, ButtonSize.ACCOUNT_M -> 40.dp
    ButtonSize.S, ButtonSize.ACCOUNT_S -> 32.dp
}


@Composable
internal fun ButtonStyle.getTextColor(isEnabled: Boolean) = when (this) {
    ButtonStyle.MAIN -> if (isEnabled) AppKitTheme.colors.inverse100 else AppKitTheme.colors.foreground.color300
    ButtonStyle.ACCENT, ButtonStyle.LOADING -> if (isEnabled) AppKitTheme.colors.accent100 else AppKitTheme.colors.grayGlass20
    ButtonStyle.SHADE -> if (isEnabled) AppKitTheme.colors.foreground.color150 else AppKitTheme.colors.grayGlass15
    ButtonStyle.ACCOUNT -> if (isEnabled) AppKitTheme.colors.foreground.color100 else AppKitTheme.colors.grayGlass15
    ButtonStyle.LINK -> if(isEnabled) AppKitTheme.colors.foreground.color200 else AppKitTheme.colors.grayGlass15
}

@Composable
internal fun ButtonStyle.getBackgroundColor(isEnabled: Boolean) = when (this) {
    ButtonStyle.MAIN -> if (isEnabled) AppKitTheme.colors.accent100 else AppKitTheme.colors.grayGlass20
    ButtonStyle.ACCENT -> if (isEnabled) Color.Transparent else AppKitTheme.colors.grayGlass10
    ButtonStyle.SHADE, ButtonStyle.LINK -> if (isEnabled) Color.Transparent else AppKitTheme.colors.grayGlass05
    ButtonStyle.LOADING -> AppKitTheme.colors.grayGlass10
    ButtonStyle.ACCOUNT -> if (isEnabled) AppKitTheme.colors.grayGlass10 else AppKitTheme.colors.grayGlass15
}

@Composable
internal fun ButtonStyle.getBorder(isEnabled: Boolean) = when (this) {
    ButtonStyle.MAIN, ButtonStyle.LINK -> if (isEnabled) Color.Transparent else AppKitTheme.colors.grayGlass20
    ButtonStyle.ACCENT, ButtonStyle.SHADE, ButtonStyle.LOADING, ButtonStyle.ACCOUNT -> if (isEnabled) AppKitTheme.colors.grayGlass10 else AppKitTheme.colors.grayGlass05
}

internal data class ButtonPreview(
    val style: ButtonStyle,
    val size: ButtonSize,
    val isEnabled: Boolean
)
