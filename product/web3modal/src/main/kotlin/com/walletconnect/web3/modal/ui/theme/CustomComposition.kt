package com.walletconnect.web3.modal.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.walletconnect.web3.modal.ui.AppKitTheme

internal data class CustomComposition(
    val mode: AppKitTheme.Mode = AppKitTheme.Mode.AUTO,
    val lightColors: AppKitTheme.Colors = AppKitTheme.provideLightAppKitColors(),
    val darkColors: AppKitTheme.Colors = AppKitTheme.provideDarkAppKitColor(),
)

internal val LocalCustomComposition = compositionLocalOf { CustomComposition() }