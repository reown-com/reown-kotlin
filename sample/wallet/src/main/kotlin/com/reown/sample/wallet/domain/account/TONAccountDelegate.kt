package com.reown.sample.wallet.domain.account

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.sample.wallet.domain.client.TONClient
import com.reown.sample.wallet.domain.client.Keypair
import com.reown.sample.wallet.domain.client.Wallet

object TONAccountDelegate {
    lateinit var application: Application
    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE)
    }
    private const val TON_PUBLIC_KEY_TAG = "ton_public_key"
    private const val TON_SECRET_KEY_TAG = "ton_secret_key"
    private const val TON_ADDRESS_RAW_TAG = "ton_address_raw"
    private const val TON_ADDRESS_FRIENDLY_TAG = "ton_address_friendly"

    private val isInitialized: Boolean
        get() = (sharedPreferences.getString(TON_ADDRESS_RAW_TAG, null) != null) &&
                (sharedPreferences.getString(TON_ADDRESS_FRIENDLY_TAG, null) != null) &&
                (sharedPreferences.getString(TON_PUBLIC_KEY_TAG, null) != null) &&
                (sharedPreferences.getString(TON_SECRET_KEY_TAG, null) != null)

    private fun storeAccount(keypair: Keypair? = null): HashMap<String, String> {
        val tonKeypair = keypair ?: TONClient.generateKeyPair()
        val wallet = TONClient.getAddressFromKeyPair(tonKeypair)
        sharedPreferences.edit { putString(TON_PUBLIC_KEY_TAG, tonKeypair.publicKey) }
        sharedPreferences.edit { putString(TON_SECRET_KEY_TAG, tonKeypair.secretKey) }
        sharedPreferences.edit { putString(TON_ADDRESS_RAW_TAG, wallet.raw) }
        sharedPreferences.edit { putString(TON_ADDRESS_FRIENDLY_TAG, wallet.friendly) }

        return hashMapOf(
            "publicKey" to tonKeypair.publicKey,
            "secretKey" to tonKeypair.secretKey,
            "addressRaw" to wallet.raw,
            "addressFriendly" to wallet.friendly
        )
    }

    val caip10MainnetAddress: String
        get() = "ton:-239:$addressFriendly"

    val addressRaw: String
        get() = if (isInitialized) sharedPreferences.getString(TON_ADDRESS_RAW_TAG, null)!! else storeAccount()["addressRaw"] ?: ""

    val addressFriendly: String
        get() = if (isInitialized) sharedPreferences.getString(TON_ADDRESS_FRIENDLY_TAG, null)!! else storeAccount()["addressFriendly"] ?: ""

    var publicKey: String
        get() = if (isInitialized) sharedPreferences.getString(TON_PUBLIC_KEY_TAG, null)!! else storeAccount()["publicKey"] ?: ""
        set(value) {
            val currentSecretKey = secretKey
            storeAccount(Keypair(currentSecretKey, value))
        }

    var secretKey: String
        get() = if (isInitialized) sharedPreferences.getString(TON_SECRET_KEY_TAG, null)!! else storeAccount()["secretKey"] ?: ""
        set(value) {
            val currentPublicKey = publicKey
            storeAccount(Keypair(value, currentPublicKey))
        }

    val keypair: Keypair
        get() = Keypair(secretKey, publicKey)

    val wallet: Wallet
        get() = Wallet(addressRaw, addressFriendly)
}