package com.reown.sample.wallet.domain.account

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import com.reown.sample.wallet.domain.client.SuiUtils

object SuiAccountDelegate {
    lateinit var application: Application
    private val sharedPreferences: SharedPreferences by lazy { application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE) }
    private const val KEY_PAIR_TAG = "self_sui_key_pair"

    private val isInitialized: Boolean get() = sharedPreferences.getString(KEY_PAIR_TAG, null) != null

    private fun storeAccount(keyPair: String? = null): String =
        if (keyPair == null) {
            SuiUtils.generateKeyPair()
                .also { generatedKeyPair ->
                    sharedPreferences.edit { putString(KEY_PAIR_TAG, generatedKeyPair) }
                }
        } else {
            sharedPreferences.edit { putString(KEY_PAIR_TAG, keyPair) }
            keyPair
        }

    var keypair: String
        get() = if (isInitialized) sharedPreferences.getString(KEY_PAIR_TAG, null)!! else storeAccount()
        set(value) {
            storeAccount(value)
        }

    val address: String
        get() = getSuiAddressForKeyPair(keypair)

    val publicKey: String
        get() = getSuiPublicKeyForKeyPair(keypair)

    val mainnetAddress: String
        get() = "${Chain.SUI.id}:$address"

    val testnetAddress: String
        get() = "${Chain.SUI_TESTNET.id}:$address"


    private fun getSuiAddressForKeyPair(keyPair: String? = null): String {
        val currentKeyPair = keyPair ?: sharedPreferences.getString(KEY_PAIR_TAG, null)!!
        val publicKey = SuiUtils.getPublicKeyFromKeyPair(currentKeyPair)
        return SuiUtils.getAddressFromPublicKey(publicKey)
    }

    private fun getSuiPublicKeyForKeyPair(keyPair: String? = null): String {
        val currentKeyPair = keyPair ?: sharedPreferences.getString(KEY_PAIR_TAG, null)!!

        return SuiUtils.getPublicKeyFromKeyPair(currentKeyPair)
    }
}