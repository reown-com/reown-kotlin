package com.reown.android.internal.common.model

import com.reown.foundation.common.model.Ttl

data class IrnParams(
    val tag: Tags,
    val ttl: Ttl,
    val correlationId: String? = null,
    val rpcMethods: List<String>? = null,
    val chainId: String? = null,
    val txHashes: List<String>? = null,
    val contractAddresses: List<String>? = null,
    val prompt: Boolean = false
)