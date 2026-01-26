@file:JvmSynthetic

package com.reown.sample.wallet.domain.account

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import uniffi.yttrium_utils.TronKeypair
import uniffi.yttrium_utils.tronGenerateKeypair
import uniffi.yttrium_utils.tronGetAddress

object TronAccountDelegate {
    lateinit var application: Application
    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE)
    }

    const val mainnet = "tron:0x2b6653dc"

    private const val TRON_SK_TAG = "tron_secret_key"
    private const val TRON_PK_TAG = "tron_public_key"
    private const val TRON_ADDRESS_TAG = "tron_address"

    private val isInitialized: Boolean
        get() = sharedPreferences.getString(TRON_SK_TAG, null) != null &&
                sharedPreferences.getString(TRON_PK_TAG, null) != null &&
                sharedPreferences.getString(TRON_ADDRESS_TAG, null) != null

    private fun storeAccount(keypair: TronKeypair? = null): Map<String, String> {
        val kp = keypair ?: tronGenerateKeypair()
        val addr = tronGetAddress(kp)

        sharedPreferences.edit { putString(TRON_SK_TAG, kp.sk) }
        sharedPreferences.edit { putString(TRON_PK_TAG, kp.pk) }
        sharedPreferences.edit { putString(TRON_ADDRESS_TAG, addr.base58) }

        return mapOf(
            "sk" to kp.sk,
            "pk" to kp.pk,
            "address" to addr.base58
        )
    }

    val caip10MainnetAddress: String
        get() = "$mainnet:$address"

    val address: String
        get() = if (isInitialized) {
            sharedPreferences.getString(TRON_ADDRESS_TAG, null)!!
        } else {
            storeAccount()["address"]!!
        }

    var secretKey: String
        get() = if (isInitialized) {
            sharedPreferences.getString(TRON_SK_TAG, null)!!
        } else {
            storeAccount()["sk"]!!
        }
        set(value) {
            val currentPk = publicKey
            storeAccount(TronKeypair(value, currentPk))
        }

    var publicKey: String
        get() = if (isInitialized) {
            sharedPreferences.getString(TRON_PK_TAG, null)!!
        } else {
            storeAccount()["pk"]!!
        }
        set(value) {
            val currentSk = secretKey
            storeAccount(TronKeypair(currentSk, value))
        }

    val keypair: TronKeypair
        get() = TronKeypair(secretKey, publicKey)
}
