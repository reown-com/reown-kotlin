@file:JvmSynthetic

package com.reown.sign.json_rpc.model

import com.reown.android.internal.common.json_rpc.model.JsonRpcHistoryRecord
import com.reown.android.internal.common.model.Expiry
import com.reown.foundation.common.model.Topic
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams

@JvmSynthetic
internal fun SignRpc.SessionRequest.toRequest(entry: JsonRpcHistoryRecord): Request<String> =
    Request(
        entry.id,
        Topic(entry.topic),
        params.request.method,
        params.chainId,
        params.request.params,
        if (params.request.expiryTimestamp != null) Expiry(params.request.expiryTimestamp) else null,
        transportType = entry.transportType
    )

@JvmSynthetic
internal fun JsonRpcHistoryRecord.toRequest(params: SignParams.SessionRequestParams): Request<SignParams.SessionRequestParams> =
    Request(
        id,
        Topic(topic),
        method,
        params.chainId,
        params,
        transportType = transportType
    )

@JvmSynthetic
internal fun JsonRpcHistoryRecord.toRequest(params: SignParams.SessionAuthenticateParams): Request<SignParams.SessionAuthenticateParams> =
    Request(
        id,
        Topic(topic),
        method,
        null,
        params,
        Expiry(params.expiryTimestamp),
        transportType = transportType
    )

@JvmSynthetic
internal fun SignRpc.SessionAuthenticate.toRequest(entry: JsonRpcHistoryRecord): Request<SignParams.SessionAuthenticateParams> =
    Request(
        entry.id,
        Topic(entry.topic),
        entry.method,
        null,
        params,
        Expiry(params.expiryTimestamp),
        transportType = entry.transportType
    )