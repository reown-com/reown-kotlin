package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HederaSignAndExecuteTransactionResult(
    val nodeId: String?,
    val transactionHash: String?,
    val transactionId: String?
)