@file:JvmSynthetic

package com.reown.android.internal.common.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


sealed class Namespace {
    abstract val chains: List<String>?
    abstract val methods: List<String>
    abstract val events: List<String>

    //    Proposal Namespaces -> Required or Optional Namespaces
    @JsonClass(generateAdapter = true)
    data class Proposal(
        @param:Json(name = "methods")
        override val methods: List<String>,
        @param:Json(name = "chains")
        override val chains: List<String>? = null,
        @param:Json(name = "events")
        override val events: List<String>
    ) : Namespace()

    @JsonClass(generateAdapter = true)
    data class Session(
        @param:Json(name = "chains")
        override val chains: List<String>? = null,
        @param:Json(name = "accounts")
        val accounts: List<String>,
        @param:Json(name = "methods")
        override val methods: List<String>,
        @param:Json(name = "events")
        override val events: List<String>
    ) : Namespace()
}