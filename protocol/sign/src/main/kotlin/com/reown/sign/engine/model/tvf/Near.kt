package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import io.ipfs.multibase.Multibase
import org.bouncycastle.crypto.digests.SHA256Digest

object NearSignTransaction {
    @JsonClass(generateAdapter = true)
    data class BufferData(
        val type: String?,
        val data: List<Int>
    ) {
        fun toByteArray(): ByteArray = data.map { it.toByte() }.toByteArray()
    }

    fun calculateTransactionHash(signedTxBytes: ByteArray): String {
        val digest = SHA256Digest()
        digest.update(signedTxBytes, 0, signedTxBytes.size)
        val hash = ByteArray(32)
        digest.doFinal(hash, 0)
        return Multibase.encode(Multibase.Base.Base58BTC, hash)
    }
}