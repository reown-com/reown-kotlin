package com.walletconnect.sample.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.reown.sample.common.ui.theme.WCSampleAppTheme
import com.walletconnect.pos.PosClient
import com.walletconnect.sample.pos.model.ThemeMode

class POSActivity : AppCompatActivity() {
    private val viewModel: POSViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.selectedThemeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WCSampleAppTheme(darkTheme = darkTheme) {
                POSSampleHost(viewModel, onClose = { finish() })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (POSApplication.initError == null) PosClient.resume()
    }

    override fun onStop() {
        super.onStop()
        if (POSApplication.initError == null) PosClient.pause()
    }
}