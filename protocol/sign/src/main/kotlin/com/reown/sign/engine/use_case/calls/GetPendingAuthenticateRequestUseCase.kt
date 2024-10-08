package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.storage.rpc.JsonRpcHistory
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.sign.json_rpc.model.toRequest

internal class GetPendingAuthenticateRequestUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) : GetPendingAuthenticateRequestUseCaseInterface {
    override suspend fun getPendingAuthenticateRequests(): List<Request<SignParams.SessionAuthenticateParams>> {
        return jsonRpcHistory.getListOfPendingRecords()
            .filter { record -> record.method == JsonRpcMethod.WC_SESSION_AUTHENTICATE }
            .mapNotNull { record -> serializer.tryDeserialize<SignRpc.SessionAuthenticate>(record.body)?.toRequest(record) }
    }
}

internal interface GetPendingAuthenticateRequestUseCaseInterface {
    suspend fun getPendingAuthenticateRequests(): List<Request<SignParams.SessionAuthenticateParams>>
}