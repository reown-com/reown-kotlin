package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.util.encoders.Base64
import com.reown.sign.engine.model.tvf.TVF.Companion.toBase58

object SignAndExecute {
    @JsonClass(generateAdapter = true)
    data class SuiTransactionResponse(
        val digest: String,
        val effects: Effects?,
        val events: List<Event>?,
        val object_changes: List<ObjectChange>?,
        val confirmed_local_execution: Boolean?
    )

    @JsonClass(generateAdapter = true)
    data class Effects(
        val status: Status?,
        val gas_used: GasUsed?
    )

    @JsonClass(generateAdapter = true)
    data class Status(
        val status: String?
    )

    @JsonClass(generateAdapter = true)
    data class GasUsed(
        val computation_cost: Long?,
        val storage_cost: Long?,
        val storage_rebate: Long?
    )

    @JsonClass(generateAdapter = true)
    data class Event(
        val coinBalanceChange: CoinBalanceChange?
    )

    @JsonClass(generateAdapter = true)
    data class CoinBalanceChange(
        val owner: String?,
        val coin_type: String?,
        val amount: Long?
    )

    @JsonClass(generateAdapter = true)
    data class ObjectChange(
        val type: String?,
        val object_id: String?,
        val owner: String?,
        val version: Long?
    )
}

object SignTransaction {
    @JsonClass(generateAdapter = true)
    data class SignatureResult(
        val signature: String,
        val transactionBytes: String
    )

    fun calculateTransactionDigest(txBytesBase64: String): String {
        try {
            val txBytes = Base64.decode(txBytesBase64)
            val typeTagBytes = "TransactionData::".toByteArray(Charsets.UTF_8)
            val dataWithTag = ByteArray(typeTagBytes.size + txBytes.size)
            System.arraycopy(typeTagBytes, 0, dataWithTag, 0, typeTagBytes.size)
            System.arraycopy(txBytes, 0, dataWithTag, typeTagBytes.size, txBytes.size)
            val hash = blake2b(dataWithTag, 32)
            return toBase58(hash)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid base64 string: ${e.message}")
        }
    }

    private fun blake2b(data: ByteArray, outputLength: Int): ByteArray {
        val digest = Blake2bDigest(outputLength * 8)
        digest.update(data, 0, data.size)

        val hash = ByteArray(outputLength)
        digest.doFinal(hash, 0)

        return hash
    }
}