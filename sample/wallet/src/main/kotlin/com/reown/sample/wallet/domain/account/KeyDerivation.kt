@file:JvmSynthetic

package com.reown.sample.wallet.domain.account

import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT
import org.web3j.crypto.MnemonicUtils

private const val PRIVATE_KEY_HEX_LENGTH = 64

internal fun normalizePrivateKeyHex(privateKey: String): String {
    val value = privateKey.removePrefix("0x").trim()
    require(value.isNotEmpty() && value.all(Char::isHexChar)) {
        "Private key must be a hex string"
    }

    val trimmed = value.trimStart('0')
    val significant = if (trimmed.isEmpty()) "0" else trimmed
    require(significant.length <= PRIVATE_KEY_HEX_LENGTH) {
        "Private key must be at most 64 hex characters"
    }

    return significant.padStart(PRIVATE_KEY_HEX_LENGTH, '0')
}

internal fun derivePrivateKeyFromMnemonic(mnemonic: String, coinType: Int): String {
    require(MnemonicUtils.validateMnemonic(mnemonic)) { "Invalid BIP39 mnemonic phrase" }

    val seed = MnemonicUtils.generateSeed(mnemonic, "")
    val masterKeypair = Bip32ECKeyPair.generateKeyPair(seed)
    val path = intArrayOf(44 or HARDENED_BIT, coinType or HARDENED_BIT, 0 or HARDENED_BIT, 0, 0)
    val derivedKeypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path)

    return normalizePrivateKeyHex(derivedKeypair.privateKey.toString(16))
}

private fun Char.isHexChar(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
