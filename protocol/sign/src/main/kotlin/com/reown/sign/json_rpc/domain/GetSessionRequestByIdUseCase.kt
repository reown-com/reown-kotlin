package com.reown.sign.json_rpc.domain

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.json_rpc.model.JsonRpcHistoryRecord
import com.reown.android.internal.common.model.TransportType
import com.reown.android.sdk.storage.data.dao.JsonRpcHistoryQueries
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.json_rpc.model.toRequest

internal class GetSessionRequestByIdUseCase(
    private val jsonRpcHistoryQueries: JsonRpcHistoryQueries,
    private val serializer: JsonRpcSerializer
) {
    operator fun invoke(id: Long): Request<SignParams.SessionRequestParams>? {
        val record: JsonRpcHistoryRecord? = jsonRpcHistoryQueries.getJsonRpcHistoryRecord(id, mapper = ::toRecord).executeAsOneOrNull()
        var entry: Request<SignParams.SessionRequestParams>? = null

        println("kobe: RECORD: $record")

        if (record != null) {
            val sessionRequest: SignRpc.SessionRequest? = serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)
            if (sessionRequest != null) {
                entry = record.toRequest(sessionRequest.params)
            }
        }

        return entry
    }

    private fun toRecord(
        requestId: Long,
        topic: String,
        method: String,
        body: String,
        response: String?,
        transportType: TransportType?
    ): JsonRpcHistoryRecord =
        JsonRpcHistoryRecord(requestId, topic, method, body, response, transportType)
}