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
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_PROPOSE,
        @param:Json(name = "params")
        override val params: SignParams.SessionProposeParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionAuthenticate(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_AUTHENTICATE,
        @param:Json(name = "params")
        override val params: SignParams.SessionAuthenticateParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionSettle(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_SETTLE,
        @param:Json(name = "params")
        override val params: SignParams.SessionSettleParams
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionRequest(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_REQUEST,
        @param:Json(name = "params")
        override val params: SignParams.SessionRequestParams
    ) : SignRpc() {
        val rpcMethod = params.request.method
        val rpcParams = params.request.params
    }

    @JsonClass(generateAdapter = true)
    internal data class SessionDelete(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_DELETE,
        @param:Json(name = "params")
        override val params: SignParams.DeleteParams
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionPing(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_PING,
        @param:Json(name = "params")
        override val params: SignParams.PingParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionEvent(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_EVENT,
        @param:Json(name = "params")
        override val params: SignParams.EventParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionUpdate(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_UPDATE,
        @param:Json(name = "params")
        override val params: SignParams.UpdateNamespacesParams,
    ) : SignRpc()

    @JsonClass(generateAdapter = true)
    internal data class SessionExtend(
        @param:Json(name = "id")
        override val id: Long = generateId(),
        @param:Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @param:Json(name = "method")
        override val method: String = JsonRpcMethod.WC_SESSION_EXTEND,
        @param:Json(name = "params")
        override val params: SignParams.ExtendParams,
    ) : SignRpc()
}