package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Wallet(
    val id: String,
    val capabilities: WalletCapabilities?
)

@JsonClass(generateAdapter = true)
data class WalletCapabilities(
    val caip345: CAIP345?
)

@JsonClass(generateAdapter = true)
data class CAIP345(
    val caip2: String?,
    val transactionHashes: List<String>?
)
