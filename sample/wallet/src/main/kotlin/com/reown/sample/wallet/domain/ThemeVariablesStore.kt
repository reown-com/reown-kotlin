package com.reown.sample.wallet.domain

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val THEME_VARIABLES_PREFS = "theme_variables_prefs"
private const val THEME_VARIABLES_KEY = "theme_variables"
private val THEME_VARIABLES_PREFIX = Regex("^themeVariables=", RegexOption.IGNORE_CASE)

/**
 * Persists the user-supplied `themeVariables` base64url string used to style the
 * WalletConnect Pay data-collection webview. Empty when unset (no custom theme).
 */
object ThemeVariablesStore {
    private var sharedPrefs: SharedPreferences? = null
    private val _themeVariables = MutableStateFlow("")
    val themeVariables = _themeVariables.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(THEME_VARIABLES_PREFS, Context.MODE_PRIVATE)
        sharedPrefs = prefs
        _themeVariables.value = prefs.getString(THEME_VARIABLES_KEY, "").orEmpty()
    }

    fun save(input: String) {
        val value = parseInput(input)
        _themeVariables.value = value
        sharedPrefs?.edit()?.putString(THEME_VARIABLES_KEY, value)?.apply()
    }

    fun clear() {
        _themeVariables.value = ""
        sharedPrefs?.edit()?.putString(THEME_VARIABLES_KEY, "")?.apply()
    }

    /**
     * Accepts either a raw base64url value or the dashboard export form
     * `themeVariables=<base64url>`, returning the bare base64url string.
     */
    fun parseInput(raw: String): String =
        raw.trim().replaceFirst(THEME_VARIABLES_PREFIX, "")
}
