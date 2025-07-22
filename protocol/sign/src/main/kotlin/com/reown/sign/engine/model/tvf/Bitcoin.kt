package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoinTransactionResult(
    val txid: String?
)