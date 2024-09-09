package com.reown.sign.json_rpc.domain

import com.reown.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.rpc.JsonRpcHistory
import com.reown.foundation.common.model.Topic
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toSessionRequest
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.sign.json_rpc.model.toRequest
import kotlinx.coroutines.supervisorScope

internal class GetPendingSessionRequestByTopicUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
) : GetPendingSessionRequestByTopicUseCaseInterface {

    override suspend fun getPendingSessionRequests(topic: Topic): List<EngineDO.SessionRequest> = supervisorScope {
        jsonRpcHistory.getListOfPendingRecordsByTopic(topic)
            .filter { record -> record.method == JsonRpcMethod.WC_SESSION_REQUEST }
            .mapNotNull { record ->
                serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)?.toRequest(record)
                    ?.toSessionRequest(metadataStorageRepository.getByTopicAndType(Topic(record.topic), AppMetaDataType.PEER))
            }
    }
}

internal interface GetPendingSessionRequestByTopicUseCaseInterface {
    suspend fun getPendingSessionRequests(topic: Topic): List<EngineDO.SessionRequest>
}