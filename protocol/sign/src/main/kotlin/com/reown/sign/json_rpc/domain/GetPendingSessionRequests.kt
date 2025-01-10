package com.reown.sign.json_rpc.domain

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.storage.rpc.JsonRpcHistory
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.sign.json_rpc.model.toRequest
import kotlinx.coroutines.supervisorScope

internal class GetPendingSessionRequests(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {

    suspend operator fun invoke(): List<Request<String>> = supervisorScope {
        jsonRpcHistory.getListOfPendingSessionRequests()
            .mapNotNull { record -> serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)?.toRequest(record) }
    }
}