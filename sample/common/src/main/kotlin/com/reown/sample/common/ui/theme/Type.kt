@file:JvmSynthetic

package com.reown.sample.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

val DefaultWCTypography = WCTypography(
    h1Regular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 50.sp, letterSpacing = (-1).sp),
    h1Medium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 50.sp, letterSpacing = (-1).sp),
    h2Regular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 44.sp, letterSpacing = (-0.88).sp),
    h2Medium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 44.sp, letterSpacing = (-0.88).sp),
    h3Regular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 38.sp, letterSpacing = (-0.76).sp),
    h3Medium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 38.sp, letterSpacing = (-0.76).sp),
    h4Regular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, letterSpacing = (-0.32).sp),
    h4Medium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 32.sp, letterSpacing = (-0.32).sp),
    h5Regular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp, letterSpacing = (-0.28).sp),
    h5Medium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.28).sp),
    h6Regular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 20.sp, letterSpacing = (-0.6).sp),
    h6Medium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = (-0.6).sp),

    bodyXlRegular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, letterSpacing = (-0.18).sp),
    bodyXlMedium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = (-0.18).sp),
    bodyLgRegular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = (-0.16).sp),
    bodyLgMedium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = (-0.16).sp),
    bodyMdRegular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = (-0.14).sp),
    bodyMdMedium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = (-0.14).sp),
    bodySmRegular = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = (-0.12).sp),
    bodySmMedium = TextStyle(fontFamily = KhTekaFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = (-0.12).sp),
)

// Material Typography with KH Teka as default font for backward compatibility
val Typography = androidx.compose.material.Typography(
    defaultFontFamily = KhTekaFontFamily
)

val LocalWCTypography = staticCompositionLocalOf { DefaultWCTypography }
