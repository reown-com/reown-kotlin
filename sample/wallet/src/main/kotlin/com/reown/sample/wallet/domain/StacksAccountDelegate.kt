package com.reown.sample.wallet.domain

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import com.reown.sample.wallet.domain.client.Stacks

object StacksAccountDelegate {
    lateinit var application: Application
    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences("Wallet_Sample_Shared_Prefs", Context.MODE_PRIVATE)
    }
    private const val WALLET_TAG = "self_stacks_wallet"

    private val isInitialized: Boolean
        get() = sharedPreferences.getString(WALLET_TAG, null) != null

    private fun storeWallet(wallet: String? = null): String =
        if (wallet == null) {
            Stacks.generateWallet().also { generatedWallet ->
                sharedPreferences.edit { putString(WALLET_TAG, generatedWallet) }
            }
        } else {
            sharedPreferences.edit { putString(WALLET_TAG, wallet) }
            wallet
        }

    val wallet: String
        get() = if (isInitialized) sharedPreferences.getString(WALLET_TAG, null)!! else storeWallet()

    var importedWallet: String
        get() = wallet
        set(value) {
            storeWallet(value)
        }

    val mainnetAddress: String
        get() = "${Chain.STACKS_MAINNET.id}:${Stacks.getAddress(wallet, Stacks.Version.mainnetP2PKH)}"

    val testnetAddress: String
        get() = "${Chain.STACKS_TESTNET.id}:${Stacks.getAddress(wallet, Stacks.Version.testnetP2PKH)}"
} 