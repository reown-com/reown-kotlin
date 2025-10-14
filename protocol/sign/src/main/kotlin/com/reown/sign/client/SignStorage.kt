package com.reown.sign.client

import com.reown.android.internal.common.json_rpc.model.JsonRpcHistoryRecord
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.AppMetaDataType
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.pairing.PairingStorageRepository
import com.reown.android.internal.common.storage.pairing.PairingStorageRepositoryInterface
import com.reown.android.pairing.model.pairingExpiry
import com.reown.android.sdk.storage.data.dao.JsonRpcHistoryQueries
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.engine.model.mapper.toSessionFfi
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.utils.isSequenceValid
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.reown.android.internal.common.model.TransportType as InternalTransportType
import uniffi.yttrium.PairingFfi
import uniffi.yttrium.SessionFfi
import uniffi.yttrium.StorageFfi
import uniffi.yttrium.TransportType

internal class SignStorage(
    private val metadataStorage: MetadataStorageRepositoryInterface,
    private val sessionStorage: SessionStorageRepository,
    private val pairingStorage: PairingStorageRepositoryInterface,
    private val selfAppMetaData: AppMetaData,
    private val jsonRpcHistoryQueries: JsonRpcHistoryQueries,
    private val logger: Logger,
) : StorageFfi {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("kobe: SignStorage coroutine error: $throwable")
    }

    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    override fun doesJsonRpcExist(requestId: ULong): Boolean {
        return runBlocking {
            println("bary: SessionStore: doesJsonRpcExist: $requestId")

            !jsonRpcHistoryQueries.doesJsonRpcNotExist(requestId.toLong()).executeAsOne().also { println("bary: does exist: ${!it}") }
        }
    }

    override fun insertJsonRpcHistory(
        requestId: ULong,
        topic: String,
        method: String,
        body: String,
        transportType: TransportType?
    ) {
        ioScope.launch {
            println("bary: SessionStore: insertJsonRpcHistory: $requestId, $topic, $method, $body, $transportType")

            try {
                jsonRpcHistoryQueries.insertOrAbortJsonRpcHistory(requestId.toLong(), topic, method, body, transportType.toInternal())
                jsonRpcHistoryQueries.selectLastInsertedRowId().executeAsOne() > 0L
            } catch (e: Exception) {
                logger.error("kobe: insert history error: $e")
            }
        }
    }

    override fun updateJsonRpcHistoryResponse(requestId: ULong, response: String) {
        ioScope.launch {
            println("bary: SessionStore: updateJsonRpcHistoryResponse: $requestId, $response")

            val record = jsonRpcHistoryQueries.getJsonRpcHistoryRecord(requestId.toLong(), mapper = ::toRecord).executeAsOneOrNull()
            
            when {
                record == null -> {
                    logger.log("bary: No JsonRpcRequest matching response")
                }
                record.response.isNullOrBlank() -> {
                    logger.log("bary: INSERTING RESPONSE: ${requestId.toLong()}")
                    jsonRpcHistoryQueries.updateJsonRpcHistory(response = response, request_id = requestId.toLong())
                }
                else -> {
                    logger.log("bary: Duplicated JsonRpc RequestId: $requestId")
                }
            }
        }
    }

    override fun deleteJsonRpcHistoryByTopic(topic: String) {
        ioScope.launch {
            println("bary: SessionStore: deleteJsonRpcHistoryByTopic: $topic")

            jsonRpcHistoryQueries.deleteJsonRpcHistory(topic)
        }
    }

    override fun addSession(session: SessionFfi) {
        ioScope.launch {
            println("kobe: SessionStore: addSession: $session")

            val sessionVO = session.toVO()

            println("kobe: insert: ${sessionVO.symKey}")

            sessionStorage.insertSession(session = sessionVO, requestId = session.requestId.toLong())


            println("kobe: insert self metadata: ${selfAppMetaData}")
            metadataStorage.insertOrAbortMetadata(
                topic = sessionVO.topic,
                appMetaData = selfAppMetaData,
                appMetaDataType = AppMetaDataType.SELF
            )

            println("kobe: insert peer metadata: ${sessionVO.peerAppMetaData!!}")
            metadataStorage.insertOrAbortMetadata(
                topic = sessionVO.topic,
                appMetaData = sessionVO.peerAppMetaData,
                appMetaDataType = AppMetaDataType.PEER
            )
        }
    }

    override fun getAllSessions(): List<SessionFfi> {
        return runBlocking {
            println("kobe: SessionStore: get all sessions")

            sessionStorage.getListOfSessionVOsWithoutMetadata()
                .filter { session -> session.isAcknowledged && session.expiry.isSequenceValid() }
                .map { session ->
                    session.copy(
                        selfAppMetaData = selfAppMetaData,
                        peerAppMetaData = metadataStorage.getByTopicAndType(session.topic, AppMetaDataType.PEER)
                    )
                }
                .map { session -> session.toSessionFfi() }
        }
    }

    override fun getSession(topic: String): SessionFfi? {
        return runBlocking {
            println("kobe: SessionStore: get session: $topic")

            sessionStorage.getSessionWithoutMetadataByTopic(topic = Topic(topic))
                .run {
                    println("kobe: session: $this")
                    val peerAppMetaData = metadataStorage.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                    println("kobe: metadata: $peerAppMetaData")
                    this.copy(
                        selfAppMetaData = selfAppMetaData,
                        peerAppMetaData = peerAppMetaData
                    )
                }.toSessionFfi().also { println("kobe: sessionFfi: $it") }
        }
    }

    override fun getAllTopics(): List<uniffi.yttrium.Topic> {
        return runBlocking {
            println("kobe: SessionStore: getAllTopics")

            val sessionTopics =
                sessionStorage.getListOfSessionVOsWithoutMetadata().map { it.topic.value }.also { println("kobe: session topics: $it") }
            val pairingTopics = pairingStorage.getListOfPairings().map { it.topic.value }.also { println("kobe: pairing topics: $it") }

            (sessionTopics + pairingTopics).also { println("kobe: all topics: $it") }
        }
    }


    override fun getDecryptionKeyForTopic(topic: String): ByteArray? {
        return runBlocking {
            println("kobe: SessionStore: getDecryptionKeyForTopic: $topic")

            try {
                val symKeyHex = sessionStorage.getSymKeyByTopic(Topic(topic))

                if (symKeyHex.isNullOrBlank()) {
                    pairingStorage.getPairingOrNullByTopic(Topic(topic))?.symKey?.also { println("kobe: Pairing symKey: $it") }?.hexToBytes()
                } else {
                    println("kobe: SymKey: $symKeyHex")

                    symKeyHex.hexToBytes()
                }

            } catch (e: Exception) {
                println("kobe: error: $e")
                ByteArray(0)
            }
        }
    }

    override fun savePartialSession(topic: String, symKey: ByteArray) {
        ioScope.launch {
            println("kobe: SessionStore: savePartialSession: topic=$topic")

            sessionStorage.insertPartialSession(topic = topic, symKey = symKey.bytesToHex())
        }
    }

    override fun deleteSession(topic: String) {
        ioScope.launch {
            println("kobe: SessionStore: deleteSession: $topic")

            jsonRpcHistoryQueries.deleteJsonRpcHistory(topic)
            metadataStorage.deleteMetaData(Topic(topic))
            sessionStorage.deleteSession(topic = Topic(topic))
        }
    }

    override fun getPairing(topic: String, rpcId: ULong): PairingFfi? {
        return runBlocking {
            println("kobe: SessionStore: getPairing: topic=$topic, rpcId=$rpcId")

            val pairing = pairingStorage.getPairingOrNullByTopicAndRpcId(Topic(topic), rpcId.toLong())
            pairing?.toPairingFfi()
        }
    }

    override fun savePairing(topic: String, rpcId: ULong, symKey: ByteArray, selfKey: ByteArray) {
        ioScope.launch {

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

    // Map yttrium TransportType to internal TransportType
    private fun TransportType?.toInternal(): InternalTransportType? = when (this) {
        null -> null
        TransportType.RELAY -> InternalTransportType.RELAY
        TransportType.LINK_MODE -> InternalTransportType.LINK_MODE
    }

    private fun toRecord(requestId: Long, topic: String, method: String, body: String, response: String?, transportType: InternalTransportType?): JsonRpcHistoryRecord =
        JsonRpcHistoryRecord(requestId, topic, method, body, response, transportType)
}