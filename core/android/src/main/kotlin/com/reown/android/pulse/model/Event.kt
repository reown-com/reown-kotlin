package com.reown.android.pulse.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.android.internal.utils.currentTimeInSeconds
import com.reown.android.pulse.model.properties.Props
import com.reown.util.generateId

@JsonClass(generateAdapter = true)
data class Event(
    @Json(name = "eventId")
    val eventId: Long = generateId(),
    @Json(name = "bundleId")
    val bundleId: String,
    @Json(name = "timestamp")
    val timestamp: Long = currentTimeInSeconds,
    @Json(name = "props")
    val props: Props
)