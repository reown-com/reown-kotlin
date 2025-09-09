package com.reown.android.internal.common.storage.pairing

import android.database.sqlite.SQLiteException
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.model.Redirect
import com.reown.android.sdk.storage.data.dao.PairingQueries
import com.reown.foundation.common.model.Topic
import com.reown.utils.Empty

class PairingStorageRepository(private val pairingQueries: PairingQueries) : PairingStorageRepositoryInterface {

    @Throws(SQLiteException::class)
    override fun insertPairing(pairing: Pairing) {
        with(pairing) {
            pairingQueries.insertOrAbortPairing(
                topic = topic.value,
                expiry = expiry.seconds,
                relay_protocol = relayProtocol,
                relay_data = relayData,
                uri = uri,
                methods = methods ?: String.Empty,
                is_active = true,
                is_proposal_received = isProposalReceived,
                rpc_id = rpcId,
                self_public_key = selfPublicKey,
                sym_key = symKey
            )
        }
    }

    @Throws(SQLiteException::class)
    override fun deletePairing(topic: Topic) {
        pairingQueries.deletePairing(topic.value)
    }

    override fun hasTopic(topic: Topic): Boolean = pairingQueries.hasTopic(topic = topic.value).executeAsOneOrNull() != null

    @Throws(SQLiteException::class)
    override suspend fun getListOfPairings(): List<Pairing> = pairingQueries.getListOfPairing(mapper = this::toPairing).awaitAsList()

    @Throws(SQLiteException::class)
    override suspend fun getListOfPairingsWithoutRequestReceived(): List<Pairing> =
        pairingQueries.getListOfPairingsWithoutRequestReceived(mapper = this::toPairing).awaitAsList()

    @Throws(SQLiteException::class)
    override fun setRequestReceived(topic: Topic) {
        pairingQueries.setRequestReceived(is_proposal_received = true, topic = topic.value)
    }

    @Throws(SQLiteException::class)
    override fun getPairingOrNullByTopic(topic: Topic): Pairing? =
        pairingQueries.getPairingByTopic(topic = topic.value, mapper = this::toPairing).executeAsOneOrNull()

    @Throws(SQLiteException::class)
    override fun getPairingOrNullByTopicAndRpcId(topic: Topic, rpcId: Long): Pairing? =
        pairingQueries.getPairingByTopicAndRpcId(topic = topic.value, rpc_id = rpcId, mapper = this::toPairingWithoutMetadata).executeAsOneOrNull()

    private fun toPairingWithoutMetadata(
        rpc_id: Long?,
        self_public_key: String?,
        sym_key: String?,
    ): Pairing {
        return Pairing(
            topic = Topic(""),
            expiry = Expiry(0),
            peerAppMetaData = null,
            relayProtocol = "irn",
            relayData = null,
            uri = "",
            selfPublicKey = self_public_key,
            rpcId = rpc_id,
            symKey = sym_key
        )
    }

    private fun toPairing(
        topic: String,
        expirySeconds: Long,
        relay_protocol: String,
        relay_data: String?,
        uri: String,
        methods: String,
        is_proposal_received: Boolean?,
        rpc_id: Long?,
        self_public_key: String?,
        sym_key: String?,
        peerName: String?,
        peerDesc: String?,
        peerUrl: String?,
        peerIcons: List<String>?,
        native: String?
    ): Pairing {
        val peerAppMetaData: AppMetaData? = if (peerName != null && peerDesc != null && peerUrl != null && peerIcons != null) {
            AppMetaData(name = peerName, description = peerDesc, url = peerUrl, icons = peerIcons, redirect = Redirect(native = native))
        } else {
            null
        }

        return Pairing(
            topic = Topic(topic),
            expiry = Expiry(expirySeconds),
            peerAppMetaData = peerAppMetaData,
            relayProtocol = relay_protocol,
            relayData = relay_data,
            uri = uri,
            isProposalReceived = is_proposal_received ?: true,
            methods = methods,
            selfPublicKey = self_public_key,
            rpcId = rpc_id,
            symKey = sym_key
        )
    }
}