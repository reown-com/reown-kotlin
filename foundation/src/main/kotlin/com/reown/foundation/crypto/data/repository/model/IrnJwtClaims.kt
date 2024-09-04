package com.reown.foundation.crypto.data.repository.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.foundation.util.jwt.JwtClaims

@JsonClass(generateAdapter = true)
data class IrnJwtClaims(
    @Json(name = "iss") override val issuer: String,
    @Json(name = "sub") val subject: String,
    @Json(name = "aud") val audience: String,
    @Json(name = "iat") val issuedAt: Long,
    @Json(name = "exp") val expiration: Long,
) : JwtClaims