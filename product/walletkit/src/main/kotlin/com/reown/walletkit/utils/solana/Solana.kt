package com.reown.walletkit.utils.solana

import uniffi.yttrium.solanaDeriveKeypairFromMnemonic
import uniffi.yttrium.solanaGenerateKeypair
import uniffi.yttrium.solanaPhantomDerivationPathWithAccount
import uniffi.yttrium.solanaPubkeyForKeypair

object SolanaUtils {
    fun generateKeyPair(): String = solanaGenerateKeypair()
    fun deriveKeypair(mnemonic: String, derivationPath: String?): String = solanaDeriveKeypairFromMnemonic(mnemonic, derivationPath)
    fun phantomDerivationPathWithAccount(account: Int): String = solanaPhantomDerivationPathWithAccount(account.toUInt())
    fun publicKeyForKeypair(keyPair: String): String = solanaPubkeyForKeypair(keyPair)
    fun signPrehash(keyPair: String, message: String): String = uniffi.yttrium.solanaSignPrehash(keyPair, message)
}