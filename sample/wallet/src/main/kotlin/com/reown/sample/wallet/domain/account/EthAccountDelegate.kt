package com.reown.sample.wallet.domain.account

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.security.Security

// TODO Move to Common
object EthAccountDelegate {
    lateinit var application: Application

    // Hardcoded test user data for IC form prefill (PoC)
    const val PREFILL_FULL_NAME = "Test User"
    const val PREFILL_DOB = "1990-01-15"
    const val PREFILL_POB_ADDRESS = "123 Main Street, New York, NY 10001"
    private val sharedPreferences: SharedPreferences by lazy { application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE) }
    private const val ACCOUNT_TAG = "self_account_tag"
    private const val PRIVATE_KEY_TAG = "self_private_key"
    private const val PUBLIC_KEY_TAG = "self_public_key"

    private val isInitialized
        get() = (sharedPreferences.getString(ACCOUNT_TAG, null) != null) && (sharedPreferences.getString(PRIVATE_KEY_TAG, null) != null) && (sharedPreferences.getString(
            PUBLIC_KEY_TAG, null) != null)

    private fun storeAccount(privateKey: String? = null): Triple<String, String, String> = generateKeys(privateKey).also { (publicKey, privateKey, address) ->
        sharedPreferences.edit { putString(ACCOUNT_TAG, address) }
        sharedPreferences.edit { putString(PRIVATE_KEY_TAG, privateKey) }
        sharedPreferences.edit { putString(PUBLIC_KEY_TAG, publicKey) }
    }

    val ethAccount: String
        get() = "eip155:1:$address"

    val sepoliaAccount: String
        get() = "eip155:11155111:$address"

    val address: String
        get() = if (isInitialized) sharedPreferences.getString(ACCOUNT_TAG, null)!! else storeAccount().third

    var privateKey: String
        get() = (if (isInitialized) sharedPreferences.getString(PRIVATE_KEY_TAG, null)!! else storeAccount().second).run {
            if (this.length > 64) {
                this.removePrefix("00")
            } else {
                this
            }
        }
        set(value) {
            storeAccount(value)
        }

    val publicKey: String
        get() = (if (isInitialized) sharedPreferences.getString(PUBLIC_KEY_TAG, null)!! else storeAccount().first).run {
            if (this.length > 128) {
                this.removePrefix("00")
            } else {
                this
            }
        }
}

context(EthAccountDelegate)
fun generateKeys(privateKey: String? = null): Triple<String, String, String> {
    Security.getProviders().forEach { provider ->
        if (provider.name == "BC") {
            Security.removeProvider(provider.name)
        }
    }
    Security.addProvider(BouncyCastleProvider())

    val keypair = privateKey?.run { ECKeyPair.create(this.hexToBytes()) } ?: Keys.createEcKeyPair()
    val newPublicKey = keypair.publicKey.toByteArray().bytesToHex()
    val newPrivateKey = keypair.privateKey.toByteArray().bytesToHex()

    return Triple(newPublicKey, newPrivateKey, Keys.toChecksumAddress(Keys.getAddress(keypair)))
}