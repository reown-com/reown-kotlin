package com.walletconnect.web3.modal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

@Composable
internal fun ProvideAppKitThemeComposition(
    content: @Composable () -> Unit,
) {
    val composition = LocalCustomComposition.current
    val colors = provideAppKitColors(composition)
    val typography = provideDefaultTypography(colors)
    CompositionLocalProvider(
        LocalColorsComposition provides colors,
        LocalTypographyComposition provides typography,
        content = content
    )
}

internal object AppKitTheme {
    val colors: AppKitColors
        @Composable
        get() = LocalColorsComposition.current

    val typo: AppKitTypography
        @Composable
        get() = LocalTypographyComposition.current
}

private val LocalTypographyComposition = compositionLocalOf<AppKitTypography> {
    error("No typography provided")
}

private val LocalColorsComposition = compositionLocalOf<AppKitColors> {
    error("No colors provided")
}
