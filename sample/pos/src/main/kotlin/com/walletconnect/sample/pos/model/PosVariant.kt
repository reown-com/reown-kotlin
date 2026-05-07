@file:JvmSynthetic

package com.walletconnect.sample.pos.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.walletconnect.sample.pos.R

enum class PosVariant(
    val displayName: String,
    @param:DrawableRes val partnerLogoRes: Int?,
    val partnerLogoWidthDp: Int,
    val partnerLogoHeightDp: Int,
    val accentColor: Color?,
    val textInvertOverride: Color?,
    val successTextColor: Color,
    val defaultTheme: ThemeMode?
) {
    DEFAULT(
        displayName = "None",
        partnerLogoRes = null,
        partnerLogoWidthDp = 0,
        partnerLogoHeightDp = 0,
        accentColor = null,
        textInvertOverride = null,
        successTextColor = Color.White,
        defaultTheme = null
    ),
    INGENICO(
        displayName = "Ingenico",
        partnerLogoRes = R.drawable.ic_ingenico_logo,
        partnerLogoWidthDp = 78,
        partnerLogoHeightDp = 22,
        accentColor = null,
        textInvertOverride = null,
        successTextColor = Color.White,
        defaultTheme = null
    ),
    SOLFLARE(
        displayName = "Solflare",
        partnerLogoRes = R.drawable.ic_solflare_brand,
        partnerLogoWidthDp = 76,
        partnerLogoHeightDp = 18,
        accentColor = Color(0xFFFFEF46),
        textInvertOverride = Color(0xFF202020),
        successTextColor = Color(0xFF202020),
        defaultTheme = ThemeMode.DARK
    ),
    BINANCE(
        displayName = "Binance",
        partnerLogoRes = R.drawable.ic_binance_brand,
        partnerLogoWidthDp = 129,
        partnerLogoHeightDp = 26,
        accentColor = Color(0xFFFCD533),
        textInvertOverride = Color(0xFF202020),
        successTextColor = Color(0xFF202020),
        defaultTheme = ThemeMode.LIGHT
    ),
    PHANTOM(
        displayName = "Phantom",
        partnerLogoRes = R.drawable.ic_phantom_brand,
        partnerLogoWidthDp = 143,
        partnerLogoHeightDp = 28,
        accentColor = Color(0xFFAB9FF2),
        textInvertOverride = null,
        successTextColor = Color.White,
        defaultTheme = ThemeMode.LIGHT
    ),
    SOLANA(
        displayName = "Solana",
        partnerLogoRes = R.drawable.ic_solana_brand,
        partnerLogoWidthDp = 125,
        partnerLogoHeightDp = 22,
        accentColor = Color(0xFF9945FF),
        textInvertOverride = Color.White,
        successTextColor = Color.White,
        defaultTheme = ThemeMode.DARK
    ),
    LEDGER(
        displayName = "Ledger",
        partnerLogoRes = R.drawable.ic_ledger_brand,
        partnerLogoWidthDp = 54,
        partnerLogoHeightDp = 18,
        accentColor = Color(0xFF000000),
        textInvertOverride = null,
        successTextColor = Color.White,
        defaultTheme = ThemeMode.LIGHT
    ),
    TREZOR(
        displayName = "Trezor",
        partnerLogoRes = R.drawable.ic_trezor_brand,
        partnerLogoWidthDp = 71,
        partnerLogoHeightDp = 18,
        accentColor = Color(0xFF60E198),
        textInvertOverride = Color(0xFF1F1F1F),
        successTextColor = Color(0xFF1F1F1F),
        defaultTheme = ThemeMode.LIGHT
    ),
    IMIN(
        displayName = "iMin",
        partnerLogoRes = R.drawable.ic_imin_brand,
        partnerLogoWidthDp = 58,
        partnerLogoHeightDp = 18,
        accentColor = Color(0xFF000000),
        textInvertOverride = null,
        successTextColor = Color.White,
        defaultTheme = ThemeMode.LIGHT
    )
}

val LocalPosVariant = staticCompositionLocalOf { PosVariant.DEFAULT }
