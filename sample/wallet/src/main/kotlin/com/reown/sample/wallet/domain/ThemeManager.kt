package com.reown.sample.wallet.domain

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val THEME_PREFS = "theme_prefs"
private const val THEME_MODE_KEY = "theme_mode"

/**
 * Theme mode values:
 * -1 = follow system
 *  0 = light
 *  1 = dark
 */
object ThemeManager {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var appContext: Context
    private val _themeMode = MutableStateFlow(-1)
    val themeMode = _themeMode.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        sharedPrefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        _themeMode.value = sharedPrefs.getInt(THEME_MODE_KEY, -1)
    }

    /**
     * Resolves the effective dark/light state, mirroring [WalletKitActivity]:
     * an explicit user preference (0/1) wins, otherwise we follow the system.
     */
    fun isDarkTheme(): Boolean = when (_themeMode.value) {
        0 -> false
        1 -> true
        else -> {
            if (!::appContext.isInitialized) return false // safe default before init()
            val uiMode = appContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
            uiMode == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun setDarkMode(enabled: Boolean) {
        val mode = if (enabled) 1 else 0
        _themeMode.value = mode
        sharedPrefs.edit().putInt(THEME_MODE_KEY, mode).apply()
    }

    fun setFollowSystem() {
        _themeMode.value = -1
        sharedPrefs.edit().putInt(THEME_MODE_KEY, -1).apply()
    }
}
