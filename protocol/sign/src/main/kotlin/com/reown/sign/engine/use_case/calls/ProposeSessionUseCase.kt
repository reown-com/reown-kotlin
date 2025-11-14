package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SessionProposer
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.utils.PROPOSAL_EXPIRY
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidNamespaceException
import com.reown.sign.common.exceptions.InvalidPropertiesException
import com.reown.sign.common.model.vo.clientsync.common.ProposalRequests
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.common.validator.SignValidator
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toCommon
import com.reown.sign.engine.model.mapper.toNamespacesVOOptional
import com.reown.sign.engine.model.mapper.toNamespacesVORequired
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.engine.use_case.utils.NamespaceMerger
import com.reown.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.supervisorScope

internal class ProposeSessionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val crypto: KeyManagementRepository,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val selfAppMetaData: AppMetaData,
    private val logger: Logger
) : ProposeSessionUseCaseInterface {

    override suspend fun proposeSession(
        requiredNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        optionalNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        properties: Map<String, String>?,
        scopedProperties: Map<String, String>?,
        pairing: Pairing,
        authentication: List<EngineDO.Authenticate>?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) = supervisorScope {
        val relay = RelayProtocolOptions(pairing.relayProtocol, pairing.relayData)
        // Map requiredNamespaces to optionalNamespaces if not null, ensuring no duplications
        val mergedOptionalNamespaces = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)
        runCatching { validate(null, mergedOptionalNamespaces, properties) }.fold(
            onSuccess = {
                val expiry = Expiry(PROPOSAL_EXPIRY)
                val selfPublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()

                val sessionProposal: SignParams.SessionProposeParams = SignParams.SessionProposeParams(
                    relays = listOf(relay),
                    proposer = SessionProposer(selfPublicKey.keyAsHex, selfAppMetaData),
                    requiredNamespaces = emptyMap(),
                    optionalNamespaces = mergedOptionalNamespaces?.toNamespacesVOOptional() ?: emptyMap(),
                    properties = properties,
                    scopedProperties = scopedProperties,
                    expiryTimestamp = expiry.seconds,
                    requests = if (!authentication.isNullOrEmpty()) {
                        ProposalRequests(authentication = authentication.map { it.toCommon() })
                    } else null
                )
                val request = SignRpc.SessionPropose(params = sessionProposal)
                proposalStorageRepository.insertProposal(sessionProposal.toVO(pairing.topic, request.id))
                logger.log("Sending proposal on topic: ${pairing.topic.value}")

                jsonRpcInteractor.proposeSession(
                    topic = pairing.topic, payload = request,
                    onSuccess = {
                        logger.log("Session proposal sent successfully, topic: ${pairing.topic}")
                        onSuccess()
                    },
                    onFailure = { error ->
                        logger.error("Failed to send a session proposal: $error")
                        onFailure(error)
                    }
                )
            },
            onFailure = { error ->
                logger.error("Failed to validate session proposal: $error")
                onFailure(error)
            }
        )
    }

    private fun validate(
        requiredNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        optionalNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        properties: Map<String, String>?
    ) {
        requiredNamespaces?.let { namespaces ->
            SignValidator.validateProposalNamespaces(namespaces.toNamespacesVORequired()) { error ->
                logger.error("Failed to send a session proposal - required namespaces error: $error")
                throw InvalidNamespaceException(error.message)
            }
        }

        optionalNamespaces?.let { namespaces ->
            SignValidator.validateProposalNamespaces(namespaces.toNamespacesVOOptional()) { error ->
                logger.error("Failed to send a session proposal - optional namespaces error: $error")
                throw InvalidNamespaceException(error.message)
            }
        }

        properties?.let {
            SignValidator.validateProperties(properties) { error ->
                logger.error("Failed to send a session proposal - session properties error: $error")
                throw InvalidPropertiesException(error.message)
            }
        }
    }
}

internal interface ProposeSessionUseCaseInterface {
    suspend fun proposeSession(
        requiredNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        optionalNamespaces: Map<String, EngineDO.Namespace.Proposal>?,
        properties: Map<String, String>?,
        scopedProperties: Map<String, String>?,
        pairing: Pairing,
        authentication: List<EngineDO.Authenticate>? = null,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}