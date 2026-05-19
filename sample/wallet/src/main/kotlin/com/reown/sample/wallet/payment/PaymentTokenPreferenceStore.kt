@file:JvmSynthetic

package com.reown.sample.wallet.payment

import android.content.Context
import androidx.core.content.edit
import com.reown.sample.wallet.domain.account.EthAccountDelegate

internal object PaymentTokenPreferenceStore {

    private const val SHARED_PREFS_NAME = "Wallet_Sample_Shared_Prefs"
    private const val LAST_PAID_TOKEN_UNIT_KEY = "pay_last_token_unit"

    private val sharedPreferences by lazy {
        EthAccountDelegate.application.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLastPaidTokenUnit(): String? {
        return sharedPreferences.getString(LAST_PAID_TOKEN_UNIT_KEY, null)
    }

    fun saveLastPaidTokenUnit(unit: String) {
        sharedPreferences.edit { putString(LAST_PAID_TOKEN_UNIT_KEY, unit) }
    }

    fun clearLastPaidTokenUnit() {
        sharedPreferences.edit { remove(LAST_PAID_TOKEN_UNIT_KEY) }
    }
}
