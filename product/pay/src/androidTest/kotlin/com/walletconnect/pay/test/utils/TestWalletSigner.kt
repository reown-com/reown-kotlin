package com.walletconnect.pay.test.utils

import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

class TestWalletSigner(privateKeyHex: String) {
    private val keyPair: ECKeyPair
    val address: String

    init {
        val cleanKey = if (privateKeyHex.startsWith("0x")) privateKeyHex.drop(2) else privateKeyHex
        keyPair = ECKeyPair.create(BigInteger(cleanKey, 16))
        address = Keys.toChecksumAddress(Keys.getAddress(keyPair))
    }

    fun signTypedDataV4(typedDataJson: String): String {
        val encoder = StructuredDataEncoder(typedDataJson)
        val hash = encoder.hashStructuredData()
        return signHash(hash)
    }

    fun personalSign(message: String): String {
        val messageBytes = if (message.startsWith("0x")) {
            hexToBytes(message)
        } else {
            message.toByteArray(Charsets.UTF_8)
        }
        val signatureData = Sign.signPrefixedMessage(messageBytes, keyPair)
        return formatSignature(signatureData)
    }

    private fun signHash(hash: ByteArray): String {
        val signatureData = Sign.signMessage(hash, keyPair, false)
        return formatSignature(signatureData)
    }

    private fun formatSignature(sig: Sign.SignatureData): String {
        val r = bytesToHex(sig.r)
        val s = bytesToHex(sig.s)
        val v = (sig.v[0].toInt() and 0xFF).toString(16).padStart(2, '0')
        return "0x$r$s$v"
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = if (hex.startsWith("0x")) hex.drop(2) else hex
        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
