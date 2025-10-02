package com.reown.sample.wallet.domain.account

import android.content.Context
import android.content.SharedPreferences
import com.reown.android.internal.common.scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

const val SA_PREFS = "sa_prefs"
const val SAFE_ENABLED_KEY = "safe_enabled"

object SmartAccountEnabler {
    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var _isSmartAccountEnabled: MutableStateFlow<Boolean>
    val isSmartAccountEnabled by lazy { _isSmartAccountEnabled.asStateFlow() }

    fun init(context: Context) {
        this.context = context
        this.sharedPrefs = context.getSharedPreferences(SA_PREFS, Context.MODE_PRIVATE)
        this._isSmartAccountEnabled = MutableStateFlow(sharedPrefs.getBoolean(SAFE_ENABLED_KEY, false))
    }

    fun enableSmartAccount(isEnabled: Boolean) {
        _isSmartAccountEnabled.value = isEnabled
        scope.launch {
            supervisorScope {
                sharedPrefs.edit().putBoolean(SAFE_ENABLED_KEY, isEnabled).apply()
            }
        }
    }
}
