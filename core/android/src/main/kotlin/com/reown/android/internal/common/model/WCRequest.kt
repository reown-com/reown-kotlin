package com.reown.android.internal.common.model

import com.reown.android.internal.common.model.type.ClientParams
import com.reown.foundation.common.model.Topic
import com.reown.utils.Empty

data class WCRequest(
    val topic: Topic,
    val id: Long,
    val method: String,
    val params: ClientParams,
    val message: String = String.Empty,
    val publishedAt: Long = 0,
    val encryptedMessage: String = String.Empty,
    val attestation: String? = null,
    val transportType: TransportType
)