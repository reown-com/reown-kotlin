package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import io.ipfs.multibase.Multibase
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.util.encoders.Base64

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
        val signature: String?,
        val transactionBytes: String
    )

    fun calculateTransactionDigest(txBytesBase64: String): String {
        val txBytes = try {
            Base64.decode(txBytesBase64)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid base64 string: ${e.message}")
        }
        val prefixedBytes = byteArrayOf(0) + txBytes

        val digest = Blake2bDigest(256)
        digest.update(prefixedBytes, 0, prefixedBytes.size)
        val hash = ByteArray(32)
        digest.doFinal(hash, 0)

        return Multibase.encode(Multibase.Base.Base58BTC, hash)
    }
}