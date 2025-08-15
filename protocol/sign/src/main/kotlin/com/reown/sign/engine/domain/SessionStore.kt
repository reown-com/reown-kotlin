package com.reown.sign.engine.domain

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.sign.engine.model.mapper.toEngineDO
import com.reown.sign.engine.model.mapper.toSessionFfi
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.utils.isSequenceValid
import uniffi.yttrium.SessionFfi
import uniffi.yttrium.SessionStore

internal class SessionStore(
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val selfAppMetaData: AppMetaData
) : SessionStore {
    override fun addSession(session: SessionFfi) {
        println("kobe: addSession: $session")

        //TODO: request ID
        val sessionVO = session.toVO()
        sessionStorageRepository.insertSession(session = session.toVO(), requestId = session.requestId.toLong())
        metadataStorageRepository.insertOrAbortMetadata(
            topic = sessionVO.topic,
            appMetaData = selfAppMetaData,
            appMetaDataType = AppMetaDataType.SELF
        )
        metadataStorageRepository.insertOrAbortMetadata(
            topic = sessionVO.topic,
            appMetaData = sessionVO.peerAppMetaData!!,
            appMetaDataType = AppMetaDataType.PEER
        )
    }

    override fun getAllSessions(): List<SessionFfi> {
        println("kobe: get all sessions")

        return sessionStorageRepository.getListOfSessionVOsWithoutMetadata()
            .filter { session -> session.isAcknowledged && session.expiry.isSequenceValid() }
            .map { session ->
                session.copy(
                    selfAppMetaData = selfAppMetaData,
                    peerAppMetaData = metadataStorageRepository.getByTopicAndType(session.topic, AppMetaDataType.PEER)
                )
            }
            .map { session -> session.toSessionFfi() }
    }

    override fun deleteSession(topic: String) {
        println("kobe: deleteSession: $topic")
    }

    override fun getSession(topic: String): SessionFfi? {
        println("kobe: get session: $topic")
        TODO("Not yet implemented")
    }
}