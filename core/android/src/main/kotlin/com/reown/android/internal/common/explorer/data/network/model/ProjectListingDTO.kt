package com.reown.android.internal.common.explorer.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProjectListingDTO(
    @Json(name = "projects")
    val projects: Map<String, ProjectDTO>,
    @Json(name = "count")
    val count: Int,
    @Json(name = "total")
    val total: Int,
)