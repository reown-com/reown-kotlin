@file:JvmSynthetic

package com.reown.sample.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.reown.sample.common.R

val KhTekaFontFamily = FontFamily(
    Font(R.font.kh_teka_regular, FontWeight.Normal),
    Font(R.font.kh_teka_medium, FontWeight.Medium),
)

@Immutable
data class WCTypography(
    // Headings
    val h1Regular: TextStyle,
    val h1Medium: TextStyle,
    val h2Regular: TextStyle,
    val h2Medium: TextStyle,
    val h3Regular: TextStyle,
    val h3Medium: TextStyle,
    val h4Regular: TextStyle,
    val h4Medium: TextStyle,
    val h5Regular: TextStyle,
    val h5Medium: TextStyle,
    val h6Regular: TextStyle,
    val h6Medium: TextStyle,

    // Body
    val bodyXlRegular: TextStyle,
    val bodyXlMedium: TextStyle,
    val bodyLgRegular: TextStyle,
    val bodyLgMedium: TextStyle,
    val bodyMdRegular: TextStyle,
    val bodyMdMedium: TextStyle,
    val bodySmRegular: TextStyle,
    val bodySmMedium: TextStyle,
)

private val noFontPadding = PlatformTextStyle(includeFontPadding = false)
private val trimLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private fun khTekaStyle(weight: FontWeight, size: Float, tracking: Float) = TextStyle(
    fontFamily = KhTekaFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = tracking.sp,
    lineHeight = size.sp,
    lineHeightStyle = trimLineHeight,
    platformStyle = noFontPadding,
)

val DefaultWCTypography = WCTypography(
    h1Regular = khTekaStyle(FontWeight.Normal, 50f, -1f),
    h1Medium = khTekaStyle(FontWeight.Medium, 50f, -1f),
    h2Regular = khTekaStyle(FontWeight.Normal, 44f, -0.88f),
    h2Medium = khTekaStyle(FontWeight.Medium, 44f, -0.88f),
    h3Regular = khTekaStyle(FontWeight.Normal, 38f, -0.76f),
    h3Medium = khTekaStyle(FontWeight.Medium, 38f, -0.76f),
    h4Regular = khTekaStyle(FontWeight.Normal, 32f, -0.32f),
    h4Medium = khTekaStyle(FontWeight.Medium, 32f, -0.32f),
    h5Regular = khTekaStyle(FontWeight.Normal, 28f, -0.28f),
    h5Medium = khTekaStyle(FontWeight.Medium, 28f, -0.28f),
    h6Regular = khTekaStyle(FontWeight.Normal, 20f, -0.6f),
    h6Medium = khTekaStyle(FontWeight.Medium, 20f, -0.6f),

    bodyXlRegular = khTekaStyle(FontWeight.Normal, 18f, -0.18f),
    bodyXlMedium = khTekaStyle(FontWeight.Medium, 18f, -0.18f),
    bodyLgRegular = khTekaStyle(FontWeight.Normal, 16f, -0.16f),
    bodyLgMedium = khTekaStyle(FontWeight.Medium, 16f, -0.16f),
    bodyMdRegular = khTekaStyle(FontWeight.Normal, 14f, -0.14f),
    bodyMdMedium = khTekaStyle(FontWeight.Medium, 14f, -0.14f),
    bodySmRegular = khTekaStyle(FontWeight.Normal, 12f, -0.12f),
    bodySmMedium = khTekaStyle(FontWeight.Medium, 12f, -0.12f),
)

// Material Typography with KH Teka as default font for backward compatibility
val Typography = androidx.compose.material.Typography(
    defaultFontFamily = KhTekaFontFamily
)

val LocalWCTypography = staticCompositionLocalOf { DefaultWCTypography }
