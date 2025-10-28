package com.reown.sign.common.model.vo.clientsync.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ProposalRequests(
    @param:Json(name = "authentication")
    val authentication: List<PayloadParams>
)
