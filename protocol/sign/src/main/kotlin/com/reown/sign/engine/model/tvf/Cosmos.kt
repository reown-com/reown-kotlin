package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.util.TreeMap
import com.squareup.moshi.Moshi
import okio.Buffer

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

    fun computeTxHash(bodyBytesBase64: String, authInfoBytesBase64: String, signatureBase64: String): String {
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

        return hashBytes.joinToString("") { "%02X".format(it) }
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
        val msgs: List<Any>,
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
        val txMap = TreeMap<String, Any?>().apply {
            put("fee", signed.fee.toSortedMap())
            put("memo", signed.memo)
            put("msg", signed.msgs.map { it.toSortedMap() })
            put("signatures", listOf(signature.toSortedMap()))
        }

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
        val buffer = Buffer()
        adapter.toJson(buffer, txMap)
        val txBytes = buffer.readByteArray()

        val digest = SHA256Digest()
        digest.update(txBytes, 0, txBytes.size)
        val hashBytes = ByteArray(digest.digestSize)
        digest.doFinal(hashBytes, 0)

        return hashBytes.joinToString("") { "%02X".format(it) }
    }

    private fun Any?.toSortedMap(): Any? {
        return when (this) {
            null -> null
            is Map<*, *> -> {
                val map = this as? Map<String, Any?> ?: return this
                TreeMap<String, Any?>().apply {
                    map.forEach { (k, v) -> put(k, v.toSortedMap()) }
                }
            }

            is List<*> -> this.map { it.toSortedMap() }
            is Signed -> TreeMap<String, Any?>().apply {
                put("chain_id", this@toSortedMap.chain_id)
                put("account_number", this@toSortedMap.account_number)
                put("sequence", this@toSortedMap.sequence)
                put("fee", this@toSortedMap.fee.toSortedMap())
                put("memo", this@toSortedMap.memo)
                put("msg", this@toSortedMap.msgs.map { it.toSortedMap() })
            }

            is Fee -> TreeMap<String, Any?>().apply {
                put("amount", this@toSortedMap.amount.map { it.toSortedMap() })
                put("gas", this@toSortedMap.gas)
            }

            is Amount -> TreeMap<String, Any?>().apply {
                put("amount", this@toSortedMap.amount)
                put("denom", this@toSortedMap.denom)
            }

            is Signature -> TreeMap<String, Any?>().apply {
                put("pub_key", this@toSortedMap.pub_key.toSortedMap())
                put("signature", this@toSortedMap.signature)
            }

            is PubKey -> TreeMap<String, Any?>().apply {
                put("type", this@toSortedMap.type)
                put("value", this@toSortedMap.value)
            }

            else -> this
        }
    }
}