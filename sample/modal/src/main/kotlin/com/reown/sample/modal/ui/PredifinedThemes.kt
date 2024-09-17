package com.reown.sample.modal.ui

import androidx.compose.ui.graphics.Color
import com.reown.appkit.ui.AppKitTheme

val predefinedOrangeLightTheme = AppKitTheme.provideLightAppKitColors(
    accent100 = Color(0xFFFFA500),
    accent90 = Color(0xFFC7912F),
    accent80 = Color(0xFFA5864E),
    foreground = AppKitTheme.provideForegroundLightColorPalette(color100 = Color(0xFFBE7B00))
)
val predefinedOrangeDarkTheme = AppKitTheme.provideDarkAppKitColor(
    accent100 = Color(0xFFFFA500),
    accent90 = Color(0xFFC7912F),
    accent80 = Color(0xFFA5864E),
    foreground = AppKitTheme.provideForegroundDarkColorPalette(color100 = Color(0xFFFFA500))
)

val predefinedRedLightTheme = AppKitTheme.provideLightAppKitColors(
    accent100 = Color(0xFFB7342B),
    accent90 = Color(0xFFA54740),
    accent80 = Color(0xFF94504B),
    background = AppKitTheme.provideBackgroundLightColorPalette(color100 = Color(0xFFFFCECA))
)
val predefinedRedDarkTheme = AppKitTheme.provideDarkAppKitColor(
    accent100 = Color(0xFFB7342B),
    accent90 = Color(0xFFA54740),
    accent80 = Color(0xFF94504B),
    background = AppKitTheme.provideBackgroundDarkColorPalette(color100 = Color(0xFF350400))
)

val predefinedGreenLightTheme = AppKitTheme.provideLightAppKitColors(
    accent100 = Color(0xFF10B124),
    accent90 = Color(0xFF31AD41),
    accent80 = Color(0xFF3B7242),
    overlay = Color(0xFF10B124)
)
val predefinedGreenDarkTheme = AppKitTheme.provideDarkAppKitColor(
    accent100 = Color(0xFF10B124),
    accent90 = Color(0xFF31AD41),
    accent80 = Color(0xFF3B7242),
    overlay = Color(0xFF10B124)
)