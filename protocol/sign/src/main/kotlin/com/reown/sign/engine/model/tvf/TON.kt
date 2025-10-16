package com.reown.sign.engine.model.tvf

import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.bouncycastle.util.encoders.Base64

@JsonClass(generateAdapter = true)
data class TonSendMessageParams(
    val valid_until: Long?,
    val from: String?,
    val messages: List<TonMessage>?
)

@JsonClass(generateAdapter = true)
data class TonMessage(
    val address: String?,
    val amount: String?,
    val stateInit: String?,
    val payload: String?
)

@JsonClass(generateAdapter = true)
data class TonBoc(
    val sender: String,
    val boc: String
)

fun buildTonBocBase64(moshi: Moshi, rpcParams: String, boc: String): List<String>? {
    val adapter = moshi.newBuilder().addLast(KotlinJsonAdapterFactory()).build().adapter(TonSendMessageParams::class.java)
    val params = adapter.fromJson(rpcParams)
    val sender = params?.from ?: return null

    val json = moshi.adapter(TonBoc::class.java).toJson(TonBoc(sender = sender, boc = boc)).trim()
    val base64 = Base64.toBase64String(json.toByteArray(Charsets.UTF_8))
    return listOf(base64)
}
