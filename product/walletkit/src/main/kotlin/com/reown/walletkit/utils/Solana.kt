//package com.reown.walletkit.utils
//
//import uniffi.yttrium.solanaDeriveKeypairFromMnemonic
//import uniffi.yttrium.solanaGenerateKeypair
//import uniffi.yttrium.solanaPhantomDerivationPathWithAccount
//import uniffi.yttrium.solanaPubkeyForKeypair
//import uniffi.yttrium.solanaSignPrehash
//
//fun solanaGenerateKeyPair(): String = solanaGenerateKeypair()
//fun solanaDeriveKeypair(mnemonic: String, derivationPath: String?): String = solanaDeriveKeypairFromMnemonic(mnemonic, derivationPath)
//fun solanaPhantomDerivationPathWithAccount(account: Int): String = solanaPhantomDerivationPathWithAccount(account.toUInt())
//fun solanaPublicKeyForKeypair(keyPair: String): String = solanaPubkeyForKeypair(keyPair)
//fun solanaSignPrehash(keyPair: String, message: String): String = solanaSignPrehash(keyPair, message)