package com.reown.sign.common.model.vo.clientsync.common

import com.reown.android.internal.common.signing.cacao.CacaoType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PayloadParams(
    @param:Json(name = "type")
    val type: String? = CacaoType.CAIP222.header,
    @param:Json(name = "chains")
    val chains: List<String>,
    @param:Json(name = "domain")
    val domain: String,
    @param:Json(name = "aud")
    val aud: String,
    @param:Json(name = "nonce")
    val nonce: String,
    @param:Json(name = "version")
    val version: String,
    @param:Json(name = "iat")
    val iat: String,
    @param:Json(name = "nbf")
    val nbf: String? = null,
    @param:Json(name = "exp")
    val exp: String? = null,
    @param:Json(name = "statement")
    val statement: String? = null,
    @param:Json(name = "requestId")
    val requestId: String? = null,
    @param:Json(name = "resources")
    val resources: List<String>? = null,
    @param:Json(name = "signatureTypes")
    val signatureTypes: Map<String, List<String>>? = null
)