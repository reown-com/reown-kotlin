package com.reown.sample.wallet.domain

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import com.reown.walletkit.utils.solanaGenerateKeyPair
import com.reown.walletkit.utils.solanaPublicKeyForKeypair
import com.reown.walletkit.utils.solanaSignPrehash
import io.ipfs.multibase.Base58
import java.util.Arrays

object SolanaAccountDelegate {
    lateinit var application: Application
    private val sharedPreferences: SharedPreferences by lazy { application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE) }
    private const val KEY_PAIR_TAG = "self_solana_key_pair"

    private val isInitialized: Boolean get() = sharedPreferences.getString(KEY_PAIR_TAG, null) != null

    private fun storeAccount(keyPair: String? = null): String =
        if (keyPair == null) {
            solanaGenerateKeyPair()
                .also { generatedKeyPair ->
                    sharedPreferences.edit { putString(KEY_PAIR_TAG, generatedKeyPair) }
                }
        } else {
            sharedPreferences.edit { putString(KEY_PAIR_TAG, keyPair) }
            keyPair
        }


    val keys: Triple<String, String, String>
        get() = (if (isInitialized) sharedPreferences.getString(KEY_PAIR_TAG, null)!! else storeAccount())
            .run {
                decodeKeyPair(this)
            }

    var keyPair: String
        get() = if (isInitialized) sharedPreferences.getString(KEY_PAIR_TAG, null)!! else storeAccount()
        set(value) {
            storeAccount(value)
        }

    fun getSolanaPubKeyForKeyPair(keyPair: String? = null): String {
        val currentKeyPair = keyPair ?: sharedPreferences.getString(KEY_PAIR_TAG, null)!!
        return solanaPublicKeyForKeypair(currentKeyPair)
    }

    fun signHash(hash: String): String {
        val keyPair = sharedPreferences.getString(KEY_PAIR_TAG, null)!!
        return solanaSignPrehash(keyPair, hash)
    }
}

context(SolanaAccountDelegate)
fun decodeKeyPair(keyPair: String): Triple<String, String, String> {
    println("kobe: keypair: $keyPair")
    // Decode from base58
    val keypairBytes = Base58.decode(keyPair)

    // First 32 bytes are the private key
    val privateKeyBytes = Arrays.copyOfRange(keypairBytes, 0, 32)

    // Last 32 bytes are the public key
    val publicKeyBytes = Arrays.copyOfRange(keypairBytes, 32, 64)

    // Convert to base58 strings
    val privateKeyBase58 = Base58.encode(privateKeyBytes)
    val publicKeyBase58 = Base58.encode(publicKeyBytes)

    return Triple(privateKeyBase58, publicKeyBase58, "${Chain.SOLANA.id}:$publicKeyBase58")
}