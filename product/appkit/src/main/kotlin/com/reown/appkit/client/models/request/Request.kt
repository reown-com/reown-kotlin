package com.reown.appkit.client.models.request

data class Request(
    val method: String,
    val params: String,
    val expiry: Long? = null,
)