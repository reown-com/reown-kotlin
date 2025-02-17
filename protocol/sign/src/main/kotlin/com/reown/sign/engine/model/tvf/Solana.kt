package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

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