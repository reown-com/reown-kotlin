package com.reown.sign.json_rpc.domain

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.storage.rpc.JsonRpcHistory
import com.reown.foundation.common.model.Topic
import com.reown.sign.common.model.Request
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.sign.json_rpc.model.toRequest
import kotlinx.coroutines.supervisorScope

internal class GetPendingRequestsUseCaseByTopic(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) : GetPendingRequestsUseCaseByTopicInterface {

    override suspend fun getPendingRequests(topic: Topic): List<Request<String>> = supervisorScope {
        jsonRpcHistory.getListOfPendingRecordsByTopic(topic)
            .filter { record -> record.method == JsonRpcMethod.WC_SESSION_REQUEST }
            .mapNotNull { record -> serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)?.toRequest(record) }
    }
}

internal interface GetPendingRequestsUseCaseByTopicInterface {
    suspend fun getPendingRequests(topic: Topic): List<Request<String>>
}