package com.reown.sample.wallet.domain.account

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.sample.wallet.BuildConfig
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
    const val PREFILL_POB_ADDRESS = "New York, NY"
    const val PREFILL_POB_COUNTRY = "US"
    const val PREFILL_POR_ADDRESS = "New York, NY"
    const val PREFILL_POR_COUNTRY = "US"
    private val sharedPreferences: SharedPreferences by lazy { application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE) }
    private const val ACCOUNT_TAG = "self_account_tag"
    private const val PRIVATE_KEY_TAG = "self_private_key"
    private const val PUBLIC_KEY_TAG = "self_public_key"
    private const val MNEMONIC_TAG = "self_mnemonic"

    private val testPrivateKey: String?
        get() = BuildConfig.TEST_WALLET_PRIVATE_KEY.ifEmpty { null }

    private val isInitialized
        get() = (sharedPreferences.getString(ACCOUNT_TAG, null) != null) && (sharedPreferences.getString(PRIVATE_KEY_TAG, null) != null) && (sharedPreferences.getString(
            PUBLIC_KEY_TAG, null) != null)

    private fun storeAccount(privateKey: String? = null): Triple<String, String, String> = generateKeys(privateKey).also { (publicKey, privateKey, address) ->
        sharedPreferences.edit {
            putString(ACCOUNT_TAG, address)
            putString(PRIVATE_KEY_TAG, privateKey)
            putString(PUBLIC_KEY_TAG, publicKey)
        }
    }

    private fun initializeAccount(): Triple<String, String, String> = storeAccount(testPrivateKey)

    val ethAccount: String
        get() = "eip155:1:$address"

    val sepoliaAccount: String
        get() = "eip155:11155111:$address"

    val address: String
        get() = if (isInitialized) sharedPreferences.getString(ACCOUNT_TAG, null)!! else initializeAccount().third

    val mnemonic: String?
        get() = sharedPreferences.getString(MNEMONIC_TAG, null)

    fun importFromMnemonic(mnemonic: String) {
        val derivedPrivateKey = derivePrivateKeyFromMnemonic(mnemonic, coinType = 60)
        storeAccount(derivedPrivateKey)
        sharedPreferences.edit { putString(MNEMONIC_TAG, mnemonic) }
    }

    var privateKey: String
        get() = normalizePrivateKeyHex(
            if (isInitialized) {
                sharedPreferences.getString(PRIVATE_KEY_TAG, null)!!
            } else {
                initializeAccount().second
            }
        )
        set(value) {
            storeAccount(normalizePrivateKeyHex(value))
            sharedPreferences.edit { remove(MNEMONIC_TAG) }
        }

    val publicKey: String
        get() = (if (isInitialized) sharedPreferences.getString(PUBLIC_KEY_TAG, null)!! else initializeAccount().first).run {
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

    val keypair = privateKey?.run { ECKeyPair.create(normalizePrivateKeyHex(this).hexToBytes()) } ?: Keys.createEcKeyPair()
    val newPublicKey = keypair.publicKey.toByteArray().bytesToHex()
    val newPrivateKey = normalizePrivateKeyHex(keypair.privateKey.toString(16))

    return Triple(newPublicKey, newPrivateKey, Keys.toChecksumAddress(Keys.getAddress(keypair)))
}
