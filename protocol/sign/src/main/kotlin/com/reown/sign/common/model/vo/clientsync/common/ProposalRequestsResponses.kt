package com.reown.sign.common.model.vo.clientsync.common

import com.reown.android.internal.common.signing.cacao.Cacao
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProposalRequestsResponses(
    @param:Json(name = "authentication")
    val authentication: List<Cacao>?
)