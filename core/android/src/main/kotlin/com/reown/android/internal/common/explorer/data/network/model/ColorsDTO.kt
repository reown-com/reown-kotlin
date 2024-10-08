package com.reown.android.internal.common.explorer.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ColorsDTO(
    @Json(name = "primary")
    val primary: String?,
    @Json(name = "secondary")
    val secondary: String?
)