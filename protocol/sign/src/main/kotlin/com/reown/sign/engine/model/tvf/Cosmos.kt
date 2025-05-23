package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.util.TreeMap
import com.squareup.moshi.Moshi
import okio.Buffer
import java.security.MessageDigest

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

object CosmosSignAmino {

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
        val chain_id: String,
        val account_number: String,
        val sequence: String,
        val memo: String,
        val msgs: List<Map<String, Any>>,
        val fee: Fee
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        val amount: List<Amount>,
        val gas: String
    )

    @JsonClass(generateAdapter = true)
    data class Amount(
        val denom: String,
        val amount: String
    )

    fun computeTxHash(signed: Signed, signature: Signature): String {
        // Convert to canonical JSON
        val canonicalJson = toCanonicalJson(signed)

        // Calculate SHA-256 hash
        val hash = sha256(canonicalJson.toByteArray(Charsets.UTF_8))

        // Convert to uppercase hex
        return hash.joinToString("") { "%02X".format(it) }
    }

    private fun toCanonicalJson(signedDoc: Signed): String {
        // Create a sorted map to ensure canonical ordering
        val canonicalMap = sortedMapOf<String, Any>()

        canonicalMap["account_number"] = signedDoc.account_number
        canonicalMap["chain_id"] = signedDoc.chain_id
        canonicalMap["fee"] = createCanonicalFee(signedDoc.fee)
        canonicalMap["memo"] = signedDoc.memo
        canonicalMap["msgs"] = signedDoc.msgs.map { createCanonicalMsg(it) }
        canonicalMap["sequence"] = signedDoc.sequence

        // Convert to JSON without whitespace
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(Map::class.java)
        return adapter.toJson(canonicalMap)
    }

    private fun createCanonicalFee(fee: Fee): Map<String, Any> {
        val canonicalFee = sortedMapOf<String, Any>()
        canonicalFee["amount"] = fee.amount.map { coin ->
            sortedMapOf<String, String>(
                "amount" to coin.amount,
                "denom" to coin.denom
            )
        }
        canonicalFee["gas"] = fee.gas
        return canonicalFee
    }

    private fun createCanonicalMsg(msg: Map<String, Any>): Map<String, Any> {
        val canonicalMsg = sortedMapOf<String, Any>()

        // Sort all keys recursively
        msg.forEach { (key, value) ->
            canonicalMsg[key] = when (value) {
                is Map<*, *> -> createCanonicalMap(value as Map<String, Any>)
                is List<*> -> value.map { item ->
                    when (item) {
                        is Map<*, *> -> createCanonicalMap(item as Map<String, Any>)
                        else -> item
                    }
                }
                else -> value
            }
        }

        return canonicalMsg
    }

    private fun createCanonicalMap(map: Map<String, Any>): Map<String, Any> {
        val canonicalMap = sortedMapOf<String, Any>()

        map.forEach { (key, value) ->
            canonicalMap[key] = when (value) {
                is Map<*, *> -> createCanonicalMap(value as Map<String, Any>)
                is List<*> -> value.map { item ->
                    when (item) {
                        is Map<*, *> -> createCanonicalMap(item as Map<String, Any>)
                        else -> item
                    }
                }
                else -> value
            }
        }

        return canonicalMap
    }

    /**
     * Calculate SHA-256 hash
     */
    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }
}