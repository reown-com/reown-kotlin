@file:JvmSynthetic

package com.reown.sign.common.model.vo.clientsync.session

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.android.internal.common.model.type.JsonRpcClientSync
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.util.generateId

internal sealed class SignRpc : JsonRpcClientSync<SignParams> {

    @JsonClass(generateAdapter = true)
    internal data class SessionPropose(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_PROPOSE,
        @Json(name = "params")
        override val params: SignParams.SessionProposeParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionAuthenticate(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_AUTHENTICATE,
        @Json(name = "params")
        override val params: SignParams.SessionAuthenticateParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionSettle(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_SETTLE,
        @Json(name = "params")
        override val params: SignParams.SessionSettleParams
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionRequest(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_REQUEST,
        @Json(name = "params")
        override val params: SignParams.SessionRequestParams
    ) : SignRpc() {
        val rpcMethod = params.request.method
        val rpcParams = params.request.params
    }

    @JsonClass(generateAdapter = true)
    internal data class SessionDelete(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_DELETE,
        @Json(name = "params")
        override val params: SignParams.DeleteParams
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionPing(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_PING,
        @Json(name = "params")
        override val params: SignParams.PingParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionEvent(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_EVENT,
        @Json(name = "params")
        override val params: SignParams.EventParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionUpdate(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_UPDATE,
        @Json(name = "params")
        override val params: SignParams.UpdateNamespacesParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionExtend(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_EXTEND,
        @Json(name = "params")
        override val params: SignParams.ExtendParams,
    ) : SignRpc()
}