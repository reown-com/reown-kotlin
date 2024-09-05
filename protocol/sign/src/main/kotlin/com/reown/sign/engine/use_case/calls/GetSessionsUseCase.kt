package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toEngineDO
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.utils.isSequenceValid
import kotlinx.coroutines.supervisorScope

internal class GetSessionsUseCase(
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val selfAppMetaData: AppMetaData
) : GetSessionsUseCaseInterface {

    override suspend fun getListOfSettledSessions(): List<EngineDO.Session> = supervisorScope {
        return@supervisorScope sessionStorageRepository.getListOfSessionVOsWithoutMetadata()
            .filter { session -> session.isAcknowledged && session.expiry.isSequenceValid() }
            .map { session -> session.copy(selfAppMetaData = selfAppMetaData, peerAppMetaData = metadataStorageRepository.getByTopicAndType(session.topic, AppMetaDataType.PEER)) }
            .map { session -> session.toEngineDO() }
    }
}

internal interface GetSessionsUseCaseInterface {
    suspend fun getListOfSettledSessions(): List<EngineDO.Session>
}