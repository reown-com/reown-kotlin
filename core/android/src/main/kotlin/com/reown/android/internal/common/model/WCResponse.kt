package com.reown.android.internal.common.model

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.model.type.ClientParams
import com.reown.foundation.common.model.Topic

data class WCResponse(
    val topic: Topic,
    val method: String,
    val response: JsonRpcResponse,
    val params: ClientParams,
)