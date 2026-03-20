package com.walletconnect.sample.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.compositeOver
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.walletconnect.sample.pos.ui.theme.LocalWCColors
import com.walletconnect.sample.pos.ui.theme.WCSampleAppTheme
import com.walletconnect.sample.pos.ui.theme.WCTheme
import com.walletconnect.sample.pos.nfc.NfcManager
import com.walletconnect.sample.pos.nfc.UsdkServiceHelper
import com.walletconnect.pos.PosClient
import com.walletconnect.sample.pos.model.LocalPosVariant
import com.walletconnect.sample.pos.model.ThemeMode

class POSActivity : AppCompatActivity() {
    private val viewModel: POSViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        UsdkServiceHelper.bind(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.selectedThemeMode.collectAsState()
            val variant by viewModel.selectedVariant.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WCSampleAppTheme(darkTheme = darkTheme) {
                val baseColors = WCTheme.colors
                val effectiveColors = if (variant.accentColor != null || variant.textInvertOverride != null) {
                    baseColors.copy(
                        bgAccentPrimary = variant.accentColor ?: baseColors.bgAccentPrimary,
                        iconAccentPrimary = variant.accentColor ?: baseColors.iconAccentPrimary,
                        textAccentPrimary = variant.accentColor ?: baseColors.textAccentPrimary,
                        borderAccentPrimary = variant.accentColor ?: baseColors.borderAccentPrimary,
                        foregroundAccentPrimary10 = variant.accentColor?.copy(alpha = 0.1f) ?: baseColors.foregroundAccentPrimary10,
                        foregroundAccentPrimary10Solid = variant.accentColor?.copy(alpha = 0.1f)?.compositeOver(baseColors.bgPrimary) ?: baseColors.foregroundAccentPrimary10Solid,
                        foregroundAccentPrimary40 = variant.accentColor?.copy(alpha = 0.4f) ?: baseColors.foregroundAccentPrimary40,
                        foregroundAccentPrimary60 = variant.accentColor?.copy(alpha = 0.6f) ?: baseColors.foregroundAccentPrimary60,
                        textInvert = variant.textInvertOverride ?: baseColors.textInvert,
                    )
                } else baseColors
                CompositionLocalProvider(
                    LocalWCColors provides effectiveColors,
                    LocalPosVariant provides variant,
                ) {
                    POSSampleHost(viewModel, onClose = { finish() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NfcManager.enable()
    }

    override fun onPause() {
        super.onPause()
        NfcManager.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        UsdkServiceHelper.unbind(this)
    }

    override fun onStart() {
        super.onStart()
        if (POSApplication.initError == null) {
            try { PosClient.resume() } catch (_: IllegalStateException) { /* not yet initialized */ }
        }
    }

    override fun onStop() {
        super.onStop()
        if (POSApplication.initError == null) {
            try { PosClient.pause() } catch (_: IllegalStateException) { /* not yet initialized */ }
        }
    }
}
