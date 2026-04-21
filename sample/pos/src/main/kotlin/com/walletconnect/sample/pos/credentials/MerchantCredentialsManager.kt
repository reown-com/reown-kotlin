@file:JvmSynthetic

package com.walletconnect.sample.pos.credentials

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.walletconnect.sample.pos.BuildConfig
import timber.log.Timber
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom

internal class MerchantCredentialsManager(private val context: Context) {

    private val plainPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Timber.w(e, "EncryptedSharedPreferences corrupted — resetting")
            resetEncryptedPrefs()
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val masterKey = MasterKey.Builder(context)
            .setKeyGenParameterSpec(spec)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun resetEncryptedPrefs() {
        context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete master key from KeyStore")
        }
    }

    fun getMerchantId(): String {
        val stored = plainPrefs.getString(KEY_MERCHANT_ID, null)
        return if (!stored.isNullOrBlank()) stored else BuildConfig.MERCHANT_ID
    }

    fun getApiKey(): String {
        val stored = encryptedPrefs.getString(KEY_API_KEY, null)
        return if (!stored.isNullOrBlank()) stored else BuildConfig.MERCHANT_API_KEY
    }

    fun saveMerchantId(value: String) {
        plainPrefs.edit().putString(KEY_MERCHANT_ID, value).apply()
    }

    fun saveApiKey(value: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, value).apply()
    }

    fun getDeviceId(): String {
        return plainPrefs.getString(KEY_DEVICE_ID, null) ?: java.util.UUID.randomUUID().toString().also {
            plainPrefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    fun hasApiKey(): Boolean {
        val stored = encryptedPrefs.getString(KEY_API_KEY, null)
        return !stored.isNullOrBlank() || BuildConfig.MERCHANT_API_KEY.isNotBlank()
    }

    // --- PIN management ---

    fun isPinSet(): Boolean = encryptedPrefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        encryptedPrefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltBase64 = encryptedPrefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = encryptedPrefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        return hashPin(pin, salt) == storedHash
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "pos_settings"
        private const val SECURE_PREFS_NAME = "pos_secure_settings"
        private const val KEY_MERCHANT_ID = "merchant_id"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
    }
}
