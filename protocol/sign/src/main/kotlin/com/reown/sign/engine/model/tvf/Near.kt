package com.reown.sign.engine.model.tvf

import com.reown.sign.engine.model.tvf.TNV.Companion.toBase58
import com.squareup.moshi.JsonClass
import org.bouncycastle.crypto.digests.SHA256Digest

object NearSignTransaction {
    @JsonClass(generateAdapter = true)
    data class BufferData(
        val type: String?,
        val data: Any // Can be either List<Int> or Map<String, Int>
    ) {
        fun toByteArray(): ByteArray {
            return when (data) {
                is List<*> -> {
                    val intList = data
                    intList.map { 
                        when (it) {
                            is Number -> it.toInt().toByte()
                            else -> throw IllegalArgumentException("Invalid number type: ${it?.javaClass}")
                        }
                    }.toByteArray()
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val intMap = data as Map<String, *>
                    val sortedEntries = intMap.entries.sortedBy { it.key.toInt() }
                    sortedEntries.map { 
                        when (val value = it.value) {
                            is Number -> value.toInt().toByte()
                            else -> throw IllegalArgumentException("Invalid number type: ${value?.javaClass}")
                        }
                    }.toByteArray()
                }
                else -> {
                    throw IllegalArgumentException("Unsupported data format: ${data::class.java}")
                }
            }
        }
    }

    fun calculateTxID(bufferData: BufferData): String {
        val transactionBytes = bufferData.toByteArray()
        val sha256 = SHA256Digest()
        val hash = ByteArray(sha256.digestSize)
        
        sha256.update(transactionBytes, 0, transactionBytes.size)
        sha256.doFinal(hash, 0)
        
        return toBase58(hash)
    }
}