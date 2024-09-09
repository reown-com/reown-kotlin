package com.reown.sign.json_rpc.domain

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.json_rpc.model.JsonRpcHistoryRecord
import com.reown.android.internal.common.storage.rpc.JsonRpcHistory
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.json_rpc.model.toRequest

class GetPendingSessionAuthenticateRequest(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {

    internal operator fun invoke(id: Long): Request<SignParams.SessionAuthenticateParams>? {
        val record: JsonRpcHistoryRecord? = jsonRpcHistory.getPendingRecordById(id)
        var entry: Request<SignParams.SessionAuthenticateParams>? = null

        if (record != null) {
            val authRequest: SignRpc.SessionAuthenticate? = serializer.tryDeserialize<SignRpc.SessionAuthenticate>(record.body)
            if (authRequest != null) {
                entry = record.toRequest(authRequest.params)
            }
        }

        return entry
    }
}