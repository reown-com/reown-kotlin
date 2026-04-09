@file:JvmSynthetic

package com.walletconnect.sample.pos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WCColors(
    // Background
    val bgPrimary: Color,
    val bgInvert: Color,
    val bgAccentPrimary: Color,
    val bgAccentCertified: Color,
    val bgSuccess: Color,
    val bgError: Color,
    val bgWarning: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInvert: Color,
    val textAccentPrimary: Color,
    val textAccentSecondary: Color,
    val textSuccess: Color,
    val textError: Color,
    val textWarning: Color,

    // Border
    val borderPrimary: Color,
    val borderSecondary: Color,
    val borderAccentPrimary: Color,
    val borderAccentSecondary: Color,
    val borderSuccess: Color,
    val borderError: Color,
    val borderWarning: Color,

    // Foreground (surface)
    val foregroundPrimary: Color,
    val foregroundSecondary: Color,
    val foregroundTertiary: Color,
    val foregroundAccentPrimary10: Color,
    val foregroundAccentPrimary10Solid: Color,
    val foregroundAccentPrimary40: Color,
    val foregroundAccentPrimary60: Color,
    val foregroundAccentSecondary10: Color,
    val foregroundAccentSecondary40: Color,
    val foregroundAccentSecondary60: Color,

    // Icon
    val iconDefault: Color,
    val iconInvert: Color,
    val iconAccentPrimary: Color,
    val iconAccentSecondary: Color,
    val iconSuccess: Color,
    val iconError: Color,
    val iconWarning: Color,

    // Brand – same value in both themes by design
    val accentBrand: Color,
)

val LightWCColors = WCColors(
    bgPrimary = Color(0xFFFFFFFF),
    bgInvert = Color(0xFF202020),
    bgAccentPrimary = Color(0xFF0988F0),
    bgAccentCertified = Color(0xFFC7B994),
    bgSuccess = Color(0x3330A46B),
    bgError = Color(0x33DF4A34),
    bgWarning = Color(0x33F3A13F),
    textPrimary = Color(0xFF202020),
    textSecondary = Color(0xFF9A9A9A),
    textTertiary = Color(0xFF6C6C6C),
    textInvert = Color(0xFFFFFFFF),
    textAccentPrimary = Color(0xFF0988F0),
    textAccentSecondary = Color(0xFFC7B994),
    textSuccess = Color(0xFF30A46B),
    textError = Color(0xFFDF4A34),
    textWarning = Color(0xFFF3A13F),
    borderPrimary = Color(0xFFE9E9E9),
    borderSecondary = Color(0xFFD0D0D0),
    borderAccentPrimary = Color(0xFF0988F0),
    borderAccentSecondary = Color(0xFFC7B994),
    borderSuccess = Color(0xFF30A46B),
    borderError = Color(0xFFDF4A34),
    borderWarning = Color(0xFFF3A13F),
    foregroundPrimary = Color(0xFFF3F3F3),
    foregroundSecondary = Color(0xFFE9E9E9),
    foregroundTertiary = Color(0xFFD0D0D0),
    foregroundAccentPrimary10 = Color(0x1A0988F0),
    foregroundAccentPrimary10Solid = Color(0xFFE6F3FE),
    foregroundAccentPrimary40 = Color(0x660988F0),
    foregroundAccentPrimary60 = Color(0x990988F0),
    foregroundAccentSecondary10 = Color(0x1AC7B994),
    foregroundAccentSecondary40 = Color(0x66C7B994),
    foregroundAccentSecondary60 = Color(0x99C7B994),
    iconDefault = Color(0xFF9A9A9A),
    iconInvert = Color(0xFF202020),
    iconAccentPrimary = Color(0xFF0988F0),
    iconAccentSecondary = Color(0xFFC7B994),
    iconSuccess = Color(0xFF30A46B),
    iconError = Color(0xFFDF4A34),
    iconWarning = Color(0xFFF3A13F),
    accentBrand = Color(0xFFB8F53D),
)

val DarkWCColors = WCColors(
    bgPrimary = Color(0xFF202020),
    bgInvert = Color(0xFFFFFFFF),
    bgAccentPrimary = Color(0xFF0988F0),
    bgAccentCertified = Color(0xFFC7B994),
    bgSuccess = Color(0x3330A46B),
    bgError = Color(0x33DF4A34),
    bgWarning = Color(0x33F3A13F),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9A9A9A),
    textTertiary = Color(0xFFBBBBBB),
    textInvert = Color(0xFF202020),
    textAccentPrimary = Color(0xFF0988F0),
    textAccentSecondary = Color(0xFFC7B994),
    textSuccess = Color(0xFF30A46B),
    textError = Color(0xFFDF4A34),
    textWarning = Color(0xFFF3A13F),
    borderPrimary = Color(0xFF363636),
    borderSecondary = Color(0xFF4F4F4F),
    borderAccentPrimary = Color(0xFF0988F0),
    borderAccentSecondary = Color(0xFFC7B994),
    borderSuccess = Color(0xFF30A46B),
    borderError = Color(0xFFDF4A34),
    borderWarning = Color(0xFFF3A13F),
    foregroundPrimary = Color(0xFF252525),
    foregroundSecondary = Color(0xFF2A2A2A),
    foregroundTertiary = Color(0xFF363636),
    foregroundAccentPrimary10 = Color(0x1A0988F0),
    foregroundAccentPrimary10Solid = Color(0xFF222F39),
    foregroundAccentPrimary40 = Color(0x660988F0),
    foregroundAccentPrimary60 = Color(0x990988F0),
    foregroundAccentSecondary10 = Color(0x1AC7B994),
    foregroundAccentSecondary40 = Color(0x66C7B994),
    foregroundAccentSecondary60 = Color(0x99C7B994),
    iconDefault = Color(0xFF9A9A9A),
    iconInvert = Color(0xFFFFFFFF),
    iconAccentPrimary = Color(0xFF0988F0),
    iconAccentSecondary = Color(0xFFC7B994),
    iconSuccess = Color(0xFF30A46B),
    iconError = Color(0xFFDF4A34),
    iconWarning = Color(0xFFF3A13F),
    accentBrand = Color(0xFFB8F53D),
)

val LocalWCColors = staticCompositionLocalOf { LightWCColors }
