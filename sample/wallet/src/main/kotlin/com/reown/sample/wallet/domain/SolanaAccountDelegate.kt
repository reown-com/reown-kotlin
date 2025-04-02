package com.reown.sample.wallet.domain

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.walletkit.utils.solanaGenerateKeypair
import com.reown.walletkit.utils.solanaPublicKeyForKeypair
import com.reown.walletkit.utils.solanaSignPrehash
import io.ipfs.multibase.Base58
import java.util.Arrays

object SolanaAccountDelegate {
    lateinit var application: Application
    private val sharedPreferences: SharedPreferences by lazy { application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE) }
    private const val KEY_PAIR_TAG = "self_solana_key_pair"

    private val isInitialized: Boolean get() = sharedPreferences.getString(KEY_PAIR_TAG, null) != null

    private fun storeAccount(privateKey: String? = null): String =
        solanaGenerateKeypair()
            .also { keyPair ->
                sharedPreferences.edit { putString(KEY_PAIR_TAG, keyPair) }
            }

    val keys: Triple<String, String, String>
        get() = (if (isInitialized) sharedPreferences.getString(KEY_PAIR_TAG, null)!! else storeAccount())
            .run {
                decodeKeyPair(this)
            }

    fun getSolanaPubKeyForKeyPair(): String {
        val keyPair = sharedPreferences.getString(KEY_PAIR_TAG, null)!!
        return solanaPublicKeyForKeypair(keyPair)
    }

    fun signHash(hash: String): String {
        val keyPair = sharedPreferences.getString(KEY_PAIR_TAG, null)!!
        return solanaSignPrehash(keyPair, hash)
    }

    private fun decodeKeyPair(keyPair: String): Triple<String, String, String> {
        // Decode from base58
        val keypairBytes = Base58.decode(keyPair)

        // First 32 bytes are the private key
        val privateKeyBytes = Arrays.copyOfRange(keypairBytes, 0, 32)

        // Last 32 bytes are the public key
        val publicKeyBytes = Arrays.copyOfRange(keypairBytes, 32, 64)

        // Convert to base58 strings
        val privateKeyBase58 = Base58.encode(privateKeyBytes)
        val publicKeyBase58 = Base58.encode(publicKeyBytes)

        return Triple(privateKeyBase58, publicKeyBase58, "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp:$publicKeyBase58")
    }
}