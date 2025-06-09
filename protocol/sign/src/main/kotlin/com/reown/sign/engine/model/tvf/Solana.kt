package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass
import io.ipfs.multibase.Base58
import org.bouncycastle.util.encoders.Base64

@JsonClass(generateAdapter = true)
data class SolanaSignAndSendTransactionResult(
    val signature: String
)

@JsonClass(generateAdapter = true)
data class SolanaSignTransactionResult(
    val signature: String,
    val transaction: String? = null
)

@JsonClass(generateAdapter = true)
data class SolanaSignAllTransactionsResult(
    val transactions: List<String>
)

internal fun extractSignature(transaction: String): String {
    val transactionBuffer = try {
        Base64.decode(transaction)
    } catch (e: Exception) {
        Base58.decode(transaction)
    }

    if (transactionBuffer.isEmpty()) {
        throw IllegalArgumentException("Transaction buffer is empty")
    }

    val numSignatures = transactionBuffer[0].toInt() and 0xFF
    if (numSignatures > 0 && transactionBuffer.size >= 65) {
        val signatureBuffer = transactionBuffer.copyOfRange(1, 65)
        return Base58.encode(signatureBuffer)
    } else {
        throw IllegalArgumentException("No signatures found in transaction")
    }
}