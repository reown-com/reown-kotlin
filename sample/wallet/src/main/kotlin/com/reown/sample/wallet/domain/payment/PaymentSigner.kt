package com.reown.sample.wallet.domain.payment

import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder

object PaymentSigner {
    fun signTypedDataV4(typedDataJson: String): SignedTypedData {
        val encoder = StructuredDataEncoder(typedDataJson)
        val hash = encoder.hashStructuredData()
        val keyPair = ECKeyPair.create(EthAccountDelegate.privateKey.hexToBytes())
        val signatureData = Sign.signMessage(hash, keyPair, false)

        val r = "0x${signatureData.r.bytesToHex()}"
        val s = "0x${signatureData.s.bytesToHex()}"
        val v = signatureData.v[0].toInt() and 0xff
        val vHex = v.toString(16).padStart(2, '0')
        val signature = "0x" + r.removePrefix("0x") + s.removePrefix("0x") + vHex

        return SignedTypedData(
            signature = signature.lowercase(),
            r = r.lowercase(),
            s = s.lowercase(),
            v = v
        )
    }
}

data class SignedTypedData(
    val signature: String,
    val r: String,
    val s: String,
    val v: Int
)
