package com.reown.appkit.data.model

import com.squareup.moshi.Json

internal data class IdentityDTO(
    @Json(name = "name")
    val name: String?,
    @Json(name = "avatar")
    val avatar: String?
)
