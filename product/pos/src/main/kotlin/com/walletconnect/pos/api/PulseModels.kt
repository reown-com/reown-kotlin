package com.walletconnect.pos.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PulseEvent(
    @param:Json(name = "eventId") val eventId: String,
    @param:Json(name = "bundleId") val bundleId: String = "pos-android",
    @param:Json(name = "timestamp") val timestamp: Long,
    @param:Json(name = "props") val props: PulseProps
)

@JsonClass(generateAdapter = true)
internal data class PulseProps(
    @param:Json(name = "event") val event: String = "ERROR",
    @param:Json(name = "type") val type: String,
    @param:Json(name = "properties") val properties: PulseErrorProperties? = null
)

@JsonClass(generateAdapter = true)
internal data class PulseErrorProperties(
    @param:Json(name = "message") val message: String?,
    @param:Json(name = "method") val method: String?
)

internal object PulseErrorType {
    const val NETWORK_ERROR = "NETWORK_ERROR"
    const val PARSE_ERROR = "PARSE_ERROR"
    const val SDK_ERROR = "SDK_ERROR"
}
