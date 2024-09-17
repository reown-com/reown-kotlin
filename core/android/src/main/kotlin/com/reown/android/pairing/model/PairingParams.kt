package com.reown.android.pairing.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.android.internal.common.model.type.ClientParams
import com.reown.android.utils.DefaultId

sealed class PairingParams : ClientParams {

    @JsonClass(generateAdapter = true)
    class DeleteParams(
        @Json(name = "code")
        val code: Int = Int.DefaultId,
        @Json(name = "message")
        val message: String,
    ) : PairingParams()

    @Suppress("CanSealedSubClassBeObject")
    class PingParams : PairingParams()
}