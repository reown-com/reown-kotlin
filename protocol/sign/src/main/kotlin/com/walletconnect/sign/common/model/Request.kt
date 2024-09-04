package com.walletconnect.sign.common.model

import com.walletconnect.android.internal.common.model.Expiry
import com.walletconnect.android.internal.common.model.TransportType
import com.reown.foundation.common.model.Topic

internal data class Request<T>(
    val id: Long,
    val topic: Topic,
    val method: String,
    val chainId: String?,
    val params: T,
    val expiry: Expiry? = null,
    val transportType: TransportType?
)