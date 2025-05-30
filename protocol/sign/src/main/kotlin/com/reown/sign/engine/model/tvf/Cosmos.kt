package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream

object CosmosSignDirect {

    @JsonClass(generateAdapter = true)
    data class SignatureData(
        val signature: Signature,
        val signed: Signed
    )

    @JsonClass(generateAdapter = true)
    data class Signature(
        val pub_key: PubKey,
        val signature: String
    )

    @JsonClass(generateAdapter = true)
    data class PubKey(
        val type: String,
        val value: String
    )

    @JsonClass(generateAdapter = true)
    data class Signed(
        val chainId: String,
        val accountNumber: String,
        val authInfoBytes: String,
        val bodyBytes: String
    )

    fun computeTxHash(bodyBytesBase64: String, authInfoBytesBase64: String, signatureBase64: String): String =
        try {
            val baos = ByteArrayOutputStream()
            val bodyBytes = Base64.decode(bodyBytesBase64)
            val authInfoBytes = Base64.decode(authInfoBytesBase64)
            val signature = Base64.decode(signatureBase64)

            baos.write(0x0A)
            baos.write(encodeVarint(bodyBytes.size.toLong()))
            baos.write(bodyBytes)

            baos.write(0x12)
            baos.write(encodeVarint(authInfoBytes.size.toLong()))
            baos.write(authInfoBytes)

            baos.write(0x1A)
            baos.write(encodeVarint(signature.size.toLong()))
            baos.write(signature)

            val txRawBytes = baos.toByteArray()

            val digest = SHA256Digest()
            digest.update(txRawBytes, 0, txRawBytes.size)
            val hashBytes = ByteArray(digest.digestSize)
            digest.doFinal(hashBytes, 0)

            hashBytes.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            ""
        }

    private fun encodeVarint(value: Long): ByteArray {
        val bytes = mutableListOf<Byte>()
        var v = value
        while (v > 127) {
            bytes.add((v and 127 or 128).toByte())
            v = v ushr 7
        }
        bytes.add(v.toByte())
        return bytes.toByteArray()
    }
}