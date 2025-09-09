package com.reown.sign.client

import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.pairing.PairingStorageRepository
import com.reown.android.internal.common.storage.pairing.PairingStorageRepositoryInterface
import com.reown.android.pairing.model.pairingExpiry
import com.reown.foundation.common.model.Topic
import com.reown.sign.engine.model.mapper.toSessionFfi
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.utils.isSequenceValid
import uniffi.yttrium.PairingFfi
import uniffi.yttrium.SessionFfi
import uniffi.yttrium.StorageFfi

internal class SignStorage(
    private val metadataStorage: MetadataStorageRepositoryInterface,
    private val sessionStorage: SessionStorageRepository,
    private val pairingStorage: PairingStorageRepositoryInterface,
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

        return sessionStorage.getListOfSessionVOsWithoutMetadata().map { it.topic.value }.also { println("kobe: $it") }
    }

    override fun getDecryptionKeyForTopic(topic: String): ByteArray? {
        println("kobe: SessionStore: getDecryptionKeyForTopic: $topic")

        return sessionStorage.getSymKeyByTopic(Topic(topic))
            .also { println("kobe: session symkey: $it") }
            ?.hexToBytes() ?: ByteArray(0)
    }

    override fun savePartialSession(topic: String, symKey: ByteArray) {
        println("kobe: SessionStore: savePartialSession: topic=$topic")

        sessionStorage.insertPartialSession(topic = topic, symKey = symKey.bytesToHex())
    }

    override fun deleteSession(topic: String) {
        println("kobe: SessionStore: deleteSession: $topic")

        sessionStorage.deleteSession(topic = Topic(topic))
    }

    override fun getPairing(topic: String, rpcId: ULong): PairingFfi? {
        println("kobe: SessionStore: getPairing: topic=$topic, rpcId=$rpcId")

        val pairing = pairingStorage.getPairingOrNullByTopicAndRpcId(Topic(topic), rpcId.toLong())
        return pairing?.toPairingFfi()
    }

    override fun savePairing(topic: String, rpcId: ULong, symKey: ByteArray, selfKey: ByteArray) {
        println("kobe: SessionStore: savePairing: topic=$topic, rpcId=$rpcId")

        val pairing = Pairing(
            topic = Topic(topic),
            expiry = Expiry(pairingExpiry),
            relayProtocol = "irn", // Default relay protocol
            relayData = null,
            uri = "", // Will be set when needed
            isProposalReceived = false,
            methods = null,
            selfPublicKey = selfKey.bytesToHex(),
            symKey = symKey.bytesToHex(),
            rpcId = rpcId.toLong()
        )

        pairingStorage.insertPairing(pairing)
    }
}

// Extension function to convert Pairing to PairingFfi
private fun Pairing.toPairingFfi(): PairingFfi {
    return PairingFfi(
        rpcId = rpcId?.toULong() ?: (-1L).toULong(),
        selfKey = selfPublicKey?.hexToBytes() ?: ByteArray(0),
        symKey = symKey?.hexToBytes() ?: ByteArray(0)
    )
}