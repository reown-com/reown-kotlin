package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EthSendTransaction(
    val from: String,
    val to: String,
    val data: String,
    val gasLimit: String,
    val gasPrice: String,
    val value: String,
    val nonce: String
)
