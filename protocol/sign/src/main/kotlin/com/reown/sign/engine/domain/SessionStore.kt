package com.reown.sign.engine.domain

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.foundation.common.model.Topic
import com.reown.sign.engine.model.mapper.toSessionFfi
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.utils.isSequenceValid
import uniffi.yttrium.PairingFfi
import uniffi.yttrium.SessionFfi
import uniffi.yttrium.StorageFfi
import kotlin.collections.filter

internal class SessionStore(
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val selfAppMetaData: AppMetaData
) : StorageFfi {
    override fun addSession(session: SessionFfi) {
        println("kobe: SessionStore: addSession: $session")

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
        println("kobe: SessionStore: get all sessions")

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

    override fun getSession(topic: String): SessionFfi? {
        println("kobe: SessionStore: get session: $topic")

        return sessionStorageRepository.getSessionWithoutMetadataByTopic(topic = Topic(topic))
            .run {
                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(peerAppMetaData = peerAppMetaData)
            }.toSessionFfi()
    }

    override fun savePairing(topic: String, rpcId: ULong, symKey: ByteArray, selfKey: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun savePartialSession(topic: String, symKey: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun getAllTopics(): List<uniffi.yttrium.Topic> {
        TODO("Not yet implemented")
    }

    override fun getDecryptionKeyForTopic(topic: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun getPairing(topic: String, rpcId: ULong): PairingFfi? {
        TODO("Not yet implemented")
    }

    override fun deleteSession(topic: String) {
        println("kobe: SessionStore: deleteSession: $topic")

        sessionStorageRepository.deleteSession(topic = Topic(topic))
    }
}
