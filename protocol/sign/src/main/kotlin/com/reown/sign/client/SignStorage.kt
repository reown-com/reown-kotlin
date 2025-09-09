package com.reown.sign.client

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.pairing.PairingStorageRepository
import com.reown.foundation.common.model.Topic
import com.reown.sign.engine.model.mapper.toSessionFfi
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.util.hexToBytes
import com.reown.utils.isSequenceValid
import uniffi.yttrium.PairingFfi
import uniffi.yttrium.SessionFfi
import uniffi.yttrium.StorageFfi

internal class SignStorage(
    private val metadataStorage: MetadataStorageRepositoryInterface,
    private val sessionStorage: SessionStorageRepository,
    private val pairingStorage: PairingStorageRepository,
    private val selfAppMetaData: AppMetaData,
) : StorageFfi {
    override fun addSession(session: SessionFfi) {
        println("kobe: SessionStore: addSession: $session")

        val sessionVO = session.toVO()
        sessionStorage.insertSession(session = session.toVO(), requestId = session.requestId.toLong())
        metadataStorage.insertOrAbortMetadata(
            topic = sessionVO.topic,
            appMetaData = selfAppMetaData,
            appMetaDataType = AppMetaDataType.SELF
        )
        metadataStorage.insertOrAbortMetadata(
            topic = sessionVO.topic,
            appMetaData = sessionVO.peerAppMetaData!!,
            appMetaDataType = AppMetaDataType.PEER
        )
    }

    override fun getAllSessions(): List<SessionFfi> {
        println("kobe: SessionStore: get all sessions")

        return sessionStorage.getListOfSessionVOsWithoutMetadata()
            .filter { session -> session.isAcknowledged && session.expiry.isSequenceValid() }
            .map { session ->
                session.copy(
                    selfAppMetaData = selfAppMetaData,
                    peerAppMetaData = metadataStorage.getByTopicAndType(session.topic, AppMetaDataType.PEER)
                )
            }
            .map { session -> session.toSessionFfi() }
    }

    override fun getSession(topic: String): SessionFfi? {
        println("kobe: SessionStore: get session: $topic")

        return sessionStorage.getSessionWithoutMetadataByTopic(topic = Topic(topic))
            .run {
                val peerAppMetaData = metadataStorage.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(peerAppMetaData = peerAppMetaData)
            }.toSessionFfi()
    }

    override fun getAllTopics(): List<uniffi.yttrium.Topic> {
        println("kobe: SessionStore: getAllTopics")

        return sessionStorage.getListOfSessionVOsWithoutMetadata().map { it.topic.value }
    }

    override fun getDecryptionKeyForTopic(topic: String): ByteArray? {
        println("kobe: SessionStore: getDecryptionKeyForTopic: $topic")

        return sessionStorage.getSessionWithoutMetadataByTopic(Topic(topic)).symKey?.hexToBytes() ?: ByteArray(0)
    }

    override fun deleteSession(topic: String) {
        println("kobe: SessionStore: deleteSession: $topic")

        sessionStorage.deleteSession(topic = Topic(topic))
    }

    override fun getPairing(topic: String, rpcId: ULong): PairingFfi? {
        TODO("Not yet implemented")
    }

    override fun savePairing(topic: String, rpcId: ULong, symKey: ByteArray, selfKey: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun savePartialSession(topic: String, symKey: ByteArray) {
        TODO("Not yet implemented")
    }
}