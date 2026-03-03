package com.reown.sample.wallet.domain.signer

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.utils.cacao.sign
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.walletkit.utils.CacaoSigner
import org.json.JSONArray
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric.hexStringToByteArray

object EthSigner {

    fun personalSign(message: String): String = CacaoSigner.sign(message, EthAccountDelegate.privateKey.hexToBytes(), SignatureType.EIP191).s

    fun extractMessageFromParams(params: String): String {
        val messageParam = extractMessageParam(params)
        val bytes = messageParam.toPersonalSignBytes()
        return String(bytes, Charsets.UTF_8)
    }

    fun personalSignFromParams(params: String): String {
        val messageParam = extractMessageParam(params)

        val dataToSign = messageParam.toPersonalSignBytes()
        val keyPair = ECKeyPair.create(EthAccountDelegate.privateKey.hexToBytes())
        val signatureData = Sign.signPrefixedMessage(dataToSign, keyPair)

        val rHex = signatureData.r.bytesToHex()
        val sHex = signatureData.s.bytesToHex()
        val vHex = (signatureData.v[0].toInt() and 0xFF).toString(16).padStart(2, '0')

        return "0x$rHex$sHex$vHex".lowercase()
    }

    fun signHash(hashToSign: String, privateKey: String): String {
        val dataToSign: ByteArray = if (hashToSign.startsWith("0x")) {
            hashToSign.drop(2).hexToBytes()
        } else {
            hashToSign.toByteArray(Charsets.UTF_8)
        }

        val ecKeyPair = ECKeyPair.create(privateKey.hexToBytes())
        val signatureData = Sign.signMessage(dataToSign, ecKeyPair, false)

        val rHex = signatureData.r.bytesToHex()
        val sHex = signatureData.s.bytesToHex()
        val vByte = signatureData.v[0]

        val v = (vByte.toInt() and 0xFF)
        val vHex = v.toString(16)
        val result = "0x$rHex$sHex$vHex"
        return result
    }

    private fun extractMessageParam(params: String): String {
        val jsonArray = JSONArray(params)
        require(jsonArray.length() > 0) { "personal_sign params are empty" }

        val first = jsonArray.getString(0)
        val second = jsonArray.optString(1).takeIf { it.isNotBlank() }

        return when {
            first.isLikelyEvmAddress() && second != null -> second
            second?.isLikelyEvmAddress() == true -> first
            else -> first
        }
    }

    private fun String.toPersonalSignBytes(): ByteArray {
        val hexPayload = removePrefix("0x")
        val isHexPayload = startsWith("0x") &&
                !isLikelyEvmAddress() &&
                hexPayload.length % 2 == 0 &&
                hexPayload.all { it.isHexCharacter() }

        return if (isHexPayload) {
            hexStringToByteArray(this)
        } else {
            toByteArray(Charsets.UTF_8)
        }
    }

    private fun String.isLikelyEvmAddress(): Boolean {
        val body = removePrefix("0x")
        return startsWith("0x") &&
                length == 42 &&
                body.all { it.isHexCharacter() }
    }

    private fun Char.isHexCharacter(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
