@file:JvmSynthetic

package com.reown.sign.storage.proposal

import android.database.sqlite.SQLiteException
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.scope
import com.reown.foundation.common.model.Topic
import com.reown.sign.common.model.vo.clientsync.common.PayloadParams
import com.reown.sign.common.model.vo.clientsync.common.ProposalRequests
import com.reown.sign.common.model.vo.proposal.ProposalVO
import com.reown.sign.storage.data.dao.optionalnamespaces.OptionalNamespaceDaoQueries
import com.reown.sign.storage.data.dao.proposal.ProposalDaoQueries
import com.reown.sign.storage.data.dao.proposalnamespace.ProposalNamespaceDaoQueries
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProposalStorageRepository(
    private val proposalDaoQueries: ProposalDaoQueries,
    private val requiredNamespaceDaoQueries: ProposalNamespaceDaoQueries,
    private val optionalNamespaceDaoQueries: OptionalNamespaceDaoQueries,
    private val moshi: Moshi
) {

    @JvmSynthetic
    @Throws(SQLiteException::class)
    internal fun insertProposal(proposal: ProposalVO) = with(proposal) {
        val requestsJson: List<String> = proposal.requests.authentication?.map {
            val fixed = normalizePayload(it)
            moshi.adapter(PayloadParams::class.java).toJson(fixed)
        } ?: emptyList()
        proposalDaoQueries.insertOrAbortSession(
            requestId,
            pairingTopic.value,
            name,
            description,
            url,
            icons,
            relayProtocol,
            relayData,
            proposerPublicKey,
            properties,
            redirect,
            expiry?.seconds,
            scopedProperties,
            requestsJson
        )

        insertRequiredNamespace(requiredNamespaces, requestId)
        insertOptionalNamespace(optionalNamespaces, requestId)
    }

    @JvmSynthetic
    @Throws(SQLiteException::class)
    internal fun getProposalByKey(proposerPubKey: String): ProposalVO {
        return proposalDaoQueries.getProposalByKey(proposerPubKey, mapper = this::mapProposalDaoToProposalVO).executeAsOne()
    }

    @JvmSynthetic
    @Throws(SQLiteException::class)
    internal fun getProposalByTopic(topic: String): ProposalVO {
        return proposalDaoQueries.getProposalByPairingTopic(topic, mapper = this::mapProposalDaoToProposalVO).executeAsOne()
    }

    @JvmSynthetic
    @Throws(SQLiteException::class)
    internal suspend fun getProposals(): List<ProposalVO> {
        return proposalDaoQueries.getListOfProposalDaos(this::mapProposalDaoToProposalVO).awaitAsList()
    }

    @JvmSynthetic
    internal fun deleteProposal(key: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                requiredNamespaceDaoQueries.deleteProposalNamespacesProposerKey(key)
                optionalNamespaceDaoQueries.deleteProposalNamespacesProposerKey(key)
                proposalDaoQueries.deleteProposal(key)
            }
        }
    }

    private fun mapProposalDaoToProposalVO(
        request_id: Long,
        pairingTopic: String,
        name: String,
        description: String,
        url: String,
        icons: List<String>,
        relay_protocol: String,
        relay_data: String?,
        proposer_key: String,
        properties: Map<String, String>?,
        redirect: String,
        expiry: Long?,
        scoped_properties: Map<String, String>?,
        authentication: List<String>?
    ): ProposalVO {
        val requiredNamespaces: Map<String, Namespace.Proposal> = getRequiredNamespaces(request_id)
        val optionalNamespaces: Map<String, Namespace.Proposal> = getOptionalNamespaces(request_id)
        val authenticationParams: List<PayloadParams> =
            authentication?.map { json -> moshi.adapter(PayloadParams::class.java).fromJson(json)!! }
                ?.map { normalizePayload(it) }
                ?: emptyList()

        return ProposalVO(
            requestId = request_id,
            pairingTopic = Topic(pairingTopic),
            name = name,
            description = description,
            url = url,
            icons = icons,
            redirect = redirect,
            relayProtocol = relay_protocol,
            relayData = relay_data,
            proposerPublicKey = proposer_key,
            properties = properties,
            scopedProperties = scoped_properties,
            requiredNamespaces = requiredNamespaces,
            optionalNamespaces = optionalNamespaces,
            expiry = if (expiry != null) Expiry(expiry) else null,
            requests = ProposalRequests(authentication = authenticationParams)
        )
    }

    private fun normalizePayload(params: PayloadParams): PayloadParams {
        val chains = params.chains
        val normalizedChains = if (chains.size == 1 && chains.first().contains(",")) {
            chains.first().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        } else chains

        val resources = params.resources
        val normalizedResources = if (resources != null && resources.size == 1 && resources.first().contains(",")) {
            resources.first().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        } else resources

        val sigTypes = params.signatureTypes
        val normalizedSigTypes = sigTypes?.mapValues { (_, list) ->
            if (list.size == 1 && list.first().contains(",")) {
                list.first().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            } else list
        }

        return params.copy(
            chains = normalizedChains,
            resources = normalizedResources,
            signatureTypes = normalizedSigTypes
        )
    }

    @Throws(SQLiteException::class)
    private fun insertRequiredNamespace(namespaces: Map<String, Namespace.Proposal>, proposalId: Long) {
        namespaces.forEach { (key, value) ->
            requiredNamespaceDaoQueries.insertOrAbortProposalNamespace(proposalId, key, value.chains, value.methods, value.events)
        }
    }

    @Throws(SQLiteException::class)
    private fun insertOptionalNamespace(namespaces: Map<String, Namespace.Proposal>?, proposalId: Long) {
        namespaces?.forEach { (key, value) ->
            optionalNamespaceDaoQueries.insertOrAbortOptionalNamespace(proposalId, key, value.chains, value.methods, value.events)
        }
    }

    private fun getRequiredNamespaces(id: Long): Map<String, Namespace.Proposal> {
        return requiredNamespaceDaoQueries.getProposalNamespaces(id) { key, chains, methods, events ->
            key to Namespace.Proposal(chains = chains, methods = methods, events = events)
        }.executeAsList().toMap()
    }

    private fun getOptionalNamespaces(id: Long): Map<String, Namespace.Proposal> {
        return optionalNamespaceDaoQueries.getOptionalNamespaces(id) { key, chains, methods, events ->
            key to Namespace.Proposal(chains = chains, methods = methods, events = events)
        }.executeAsList().toMap()
    }
}