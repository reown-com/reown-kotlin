@file:JvmSynthetic

package com.reown.sign.di

import com.reown.sign.common.adapters.SessionEventVOJsonAdapter
import com.reown.sign.common.adapters.SessionRequestVOJsonAdapter
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionEventVO
import com.reown.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.utils.addDeserializerEntry
import com.reown.utils.addJsonAdapter
import com.reown.utils.addSerializerEntry
import org.koin.dsl.module

@JvmSynthetic
internal fun signJsonRpcModule() = module {
    addSerializerEntry(SignRpc.SessionPropose::class)
    addSerializerEntry(SignRpc.SessionPing::class)
    addSerializerEntry(SignRpc.SessionEvent::class)
    addSerializerEntry(SignRpc.SessionUpdate::class)
    addSerializerEntry(SignRpc.SessionRequest::class)
    addSerializerEntry(SignRpc.SessionDelete::class)
    addSerializerEntry(SignRpc.SessionSettle::class)
    addSerializerEntry(SignRpc.SessionExtend::class)
    addSerializerEntry(SignRpc.SessionAuthenticate::class)

    addDeserializerEntry(JsonRpcMethod.WC_SESSION_PROPOSE, SignRpc.SessionPropose::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_SETTLE, SignRpc.SessionSettle::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_REQUEST, SignRpc.SessionRequest::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_DELETE, SignRpc.SessionDelete::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_PING, SignRpc.SessionPing::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_EVENT, SignRpc.SessionEvent::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_UPDATE, SignRpc.SessionUpdate::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_EXTEND, SignRpc.SessionExtend::class)
    addDeserializerEntry(JsonRpcMethod.WC_SESSION_AUTHENTICATE, SignRpc.SessionAuthenticate::class)

    addJsonAdapter(SessionEventVO::class.java, ::SessionEventVOJsonAdapter)
    addJsonAdapter(SessionRequestVO::class.java, ::SessionRequestVOJsonAdapter)
}