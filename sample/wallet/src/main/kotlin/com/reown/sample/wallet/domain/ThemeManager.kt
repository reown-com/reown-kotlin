package com.reown.sample.wallet.domain

import android.content.Context
import android.content.SharedPreferences
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
    private val _themeMode = MutableStateFlow(-1)
    val themeMode = _themeMode.asStateFlow()

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        _themeMode.value = sharedPrefs.getInt(THEME_MODE_KEY, -1)
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
