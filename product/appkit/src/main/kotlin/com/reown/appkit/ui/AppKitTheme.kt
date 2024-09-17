package com.reown.appkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.reown.appkit.ui.theme.ColorPalette
import com.reown.appkit.ui.theme.CustomComposition
import com.reown.appkit.ui.theme.LocalCustomComposition
import com.reown.appkit.ui.theme.defaultDarkAppKitColors
import com.reown.appkit.ui.theme.defaultLightAppKitColors

@Composable
fun AppKitTheme(
    mode: AppKitTheme.Mode = AppKitTheme.Mode.AUTO,
    lightColors: AppKitTheme.Colors = AppKitTheme.provideLightAppKitColors(),
    darkColors: AppKitTheme.Colors = AppKitTheme.provideDarkAppKitColor(),
    content: @Composable () -> Unit
) {
    val customComposition = CustomComposition(
        mode = mode,
        lightColors = lightColors,
        darkColors = darkColors,
    )
    CompositionLocalProvider(
        LocalCustomComposition provides customComposition
    ) {
        content()
    }
}

object AppKitTheme {

    fun provideLightAppKitColors(
        accent100: Color = defaultLightAppKitColors.accent100,
        accent90: Color = defaultLightAppKitColors.accent90,
        accent80: Color = defaultLightAppKitColors.accent80,
        foreground: ColorPalette = defaultLightAppKitColors.foreground,
        background: ColorPalette = defaultLightAppKitColors.background,
        overlay: Color = defaultLightAppKitColors.grayGlass,
        success: Color = defaultLightAppKitColors.success,
        error: Color = defaultLightAppKitColors.error
    ): Colors = CustomAppKitColor(accent100, accent90, accent80, foreground, background, overlay, success, error)

    fun provideDarkAppKitColor(
        accent100: Color = defaultDarkAppKitColors.accent100,
        accent90: Color = defaultDarkAppKitColors.accent90,
        accent80: Color = defaultDarkAppKitColors.accent80,
        foreground: ColorPalette = defaultDarkAppKitColors.foreground,
        background: ColorPalette = defaultDarkAppKitColors.background,
        overlay: Color = defaultDarkAppKitColors.grayGlass,
        success: Color = defaultDarkAppKitColors.success,
        error: Color = defaultDarkAppKitColors.error
    ): Colors = CustomAppKitColor(accent100, accent90, accent80, foreground, background, overlay, success, error)

    fun provideForegroundLightColorPalette(
        color100: Color = defaultLightAppKitColors.foreground.color100,
        color125: Color = defaultLightAppKitColors.foreground.color125,
        color150: Color = defaultLightAppKitColors.foreground.color150,
        color175: Color = defaultLightAppKitColors.foreground.color175,
        color200: Color = defaultLightAppKitColors.foreground.color200,
        color225: Color = defaultLightAppKitColors.foreground.color225,
        color250: Color = defaultLightAppKitColors.foreground.color250,
        color275: Color = defaultLightAppKitColors.foreground.color275,
        color300: Color = defaultLightAppKitColors.foreground.color300,
    ) = ColorPalette(color100, color125, color150, color175, color200, color225, color250, color275, color300)

    fun provideForegroundDarkColorPalette(
        color100: Color = defaultDarkAppKitColors.foreground.color100,
        color125: Color = defaultDarkAppKitColors.foreground.color125,
        color150: Color = defaultDarkAppKitColors.foreground.color150,
        color175: Color = defaultDarkAppKitColors.foreground.color175,
        color200: Color = defaultDarkAppKitColors.foreground.color200,
        color225: Color = defaultDarkAppKitColors.foreground.color225,
        color250: Color = defaultDarkAppKitColors.foreground.color250,
        color275: Color = defaultDarkAppKitColors.foreground.color275,
        color300: Color = defaultDarkAppKitColors.foreground.color300,
    ) = ColorPalette(color100, color125, color150, color175, color200, color225, color250, color275, color300)

    fun provideBackgroundLightColorPalette(
        color100: Color = defaultLightAppKitColors.background.color100,
        color125: Color = defaultLightAppKitColors.background.color125,
        color150: Color = defaultLightAppKitColors.background.color150,
        color175: Color = defaultLightAppKitColors.background.color175,
        color200: Color = defaultLightAppKitColors.background.color200,
        color225: Color = defaultLightAppKitColors.background.color225,
        color250: Color = defaultLightAppKitColors.background.color250,
        color275: Color = defaultLightAppKitColors.background.color275,
        color300: Color = defaultLightAppKitColors.background.color300,
    ) = ColorPalette(color100, color125, color150, color175, color200, color225, color250, color275, color300)

    fun provideBackgroundDarkColorPalette(
        color100: Color = defaultDarkAppKitColors.background.color100,
        color125: Color = defaultDarkAppKitColors.background.color125,
        color150: Color = defaultDarkAppKitColors.background.color150,
        color175: Color = defaultDarkAppKitColors.background.color175,
        color200: Color = defaultDarkAppKitColors.background.color200,
        color225: Color = defaultDarkAppKitColors.background.color225,
        color250: Color = defaultDarkAppKitColors.background.color250,
        color275: Color = defaultDarkAppKitColors.background.color275,
        color300: Color = defaultDarkAppKitColors.background.color300,
    ) = ColorPalette(color100, color125, color150, color175, color200, color225, color250, color275, color300)


    enum class Mode {
        LIGHT, DARK, AUTO
    }

    interface Colors {
        val accent100: Color
        val accent90: Color
        val accent80: Color
        val foreground: ColorPalette
        val background: ColorPalette
        val grayGlass: Color
        val success: Color
        val error: Color
    }
}

private class CustomAppKitColor(
    override val accent100: Color,
    override val accent90: Color,
    override val accent80: Color,
    override val foreground: ColorPalette,
    override val background: ColorPalette,
    override val grayGlass: Color,
    override val success: Color,
    override val error: Color
) : AppKitTheme.Colors
