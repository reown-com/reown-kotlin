package com.reown.sign.common.model.vo.clientsync.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PayloadParams(
    @param:Json(name = "type")
    val type: String,
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
    val nbf: String?,
    @param:Json(name = "exp")
    val exp: String?,
    @param:Json(name = "statement")
    val statement: String?,
    @param:Json(name = "requestId")
    val requestId: String?,
    @param:Json(name = "resources")
    val resources: List<String>?,
    @param:Json(name = "signatureTypes")
    val signatureTypes: Map<String, List<String>>? = null
)