package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StacksTransactionData(
    val txId: String?,
    val txRaw: String?
)