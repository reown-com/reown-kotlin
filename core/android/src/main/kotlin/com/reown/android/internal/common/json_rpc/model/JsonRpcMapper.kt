@file:JvmSynthetic

package com.reown.android.internal.common.json_rpc.model

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.json_rpc.domain.relay.Subscription
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.WCResponse
import com.reown.android.internal.common.model.sync.ClientJsonRpc
import com.reown.android.internal.common.model.type.ClientParams
import com.reown.foundation.common.model.Topic
import com.reown.foundation.network.model.Relay

@JvmSynthetic
internal fun JsonRpcHistoryRecord.toWCResponse(result: JsonRpcResponse, params: ClientParams): WCResponse =
    WCResponse(Topic(topic), method, result, params)

@JvmSynthetic
internal fun IrnParams.toRelay(): Relay.Model.IrnParams =
    Relay.Model.IrnParams(tag.id, ttl.seconds, prompt)

internal fun Subscription.toWCRequest(clientJsonRpc: ClientJsonRpc, params: ClientParams, transportType: TransportType): WCRequest =
    WCRequest(topic, clientJsonRpc.id, clientJsonRpc.method, params, decryptedMessage, publishedAt, encryptedMessage, attestation, transportType)