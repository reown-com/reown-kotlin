package com.reown.sample.wallet.domain

import android.content.Context
import android.content.SharedPreferences
import com.reown.android.internal.common.scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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
    private lateinit var _themeMode: MutableStateFlow<Int>
    val themeMode by lazy { _themeMode.asStateFlow() }

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        _themeMode = MutableStateFlow(sharedPrefs.getInt(THEME_MODE_KEY, -1))
    }

    fun setDarkMode(enabled: Boolean) {
        val mode = if (enabled) 1 else 0
        _themeMode.value = mode
        scope.launch {
            supervisorScope {
                sharedPrefs.edit().putInt(THEME_MODE_KEY, mode).apply()
            }
        }
    }

    fun setFollowSystem() {
        _themeMode.value = -1
        scope.launch {
            supervisorScope {
                sharedPrefs.edit().putInt(THEME_MODE_KEY, -1).apply()
            }
        }
    }
}
