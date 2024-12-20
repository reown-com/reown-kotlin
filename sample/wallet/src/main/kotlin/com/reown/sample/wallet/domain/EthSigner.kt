package com.reown.sample.wallet.domain

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.utils.cacao.sign
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.walletkit.utils.CacaoSigner
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign

object EthSigner {

    fun personalSign(message: String): String = CacaoSigner.sign(message, EthAccountDelegate.privateKey.hexToBytes(), SignatureType.EIP191).s

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
}