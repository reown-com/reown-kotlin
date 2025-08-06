package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.Validator
import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.pairing.PairingStorageRepositoryInterface
import com.reown.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.reown.android.pulse.domain.InsertTelemetryEventUseCase
import com.reown.foundation.util.Logger
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toProposalFfi
import com.reown.sign.engine.model.mapper.toProposalYttrium
import com.reown.sign.engine.model.mapper.toSettleYttrium
import com.reown.sign.engine.model.mapper.toYttrium
import com.reown.sign.storage.proposal.ProposalStorageRepository
import com.reown.sign.storage.sequence.SessionStorageRepository
import com.reown.util.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.Redirect
import uniffi.yttrium.Relay
import uniffi.yttrium.SessionProposalFfi
import uniffi.yttrium.SignClient

internal class ApproveSessionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val crypto: KeyManagementRepository,
    private val sessionStorageRepository: SessionStorageRepository,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val selfAppMetaData: AppMetaData,
    private val insertEventUseCase: InsertTelemetryEventUseCase,
    private val logger: Logger,
    private val signClient: SignClient,
    private val pairingRepository: PairingStorageRepositoryInterface
) : ApproveSessionUseCaseInterface {

    override suspend fun approve(
        proposerPublicKey: String,
        sessionNamespaces: Map<String, EngineDO.Namespace.Session>,
        sessionProperties: Map<String, String>?,
        scopedProperties: Map<String, String>?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) = supervisorScope {
//        val trace: MutableList<String> = mutableListOf()
//        trace.add(Trace.Session.SESSION_APPROVE_STARTED).also { logger.log(Trace.Session.SESSION_APPROVE_STARTED) }
        val proposal = proposalStorageRepository.getProposalByKey(proposerPublicKey)
        val result = async {
            try {
                signClient.approve(proposal = proposal.toProposalFfi(), approvedNamespaces = sessionNamespaces.toSettleYttrium(), selfMetadata = selfAppMetaData.toYttrium())
            } catch (e: Exception) {
                println("kobe: Approve Error: $e")
                onFailure(e)
            }

        }.await()
        println("kobe: Session Approve Result: $result")
        onSuccess()
    }
}
//        val request = proposal.toSessionProposeRequest()
//        val pairingTopic = proposal.pairingTopic.value
//        try {
//            proposal.expiry?.let {
//                if (it.isExpired()) {
//                    insertEventUseCase(Props(type = EventType.Error.PROPOSAL_EXPIRED, properties = Properties(trace = trace, topic = pairingTopic)))
//                        .also { logger.error("Proposal expired on approve, topic: $pairingTopic, id: ${proposal.requestId}") }
//                    throw SessionProposalExpiredException("Session proposal expired")
//                }
//            }
//            trace.add(Trace.Session.PROPOSAL_NOT_EXPIRED)
//            SignValidator.validateSessionNamespace(sessionNamespaces.toMapOfNamespacesVOSession(), proposal.requiredNamespaces) { error ->
//                insertEventUseCase(
//                    Props(
//                        type = EventType.Error.SESSION_APPROVE_NAMESPACE_VALIDATION_FAILURE,
//                        properties = Properties(trace = trace, topic = pairingTopic)
//                    )
//                )
//                    .also { logger.log("Session approve failure - invalid namespaces, error: ${error.message}") }
//                throw InvalidNamespaceException(error.message)
//            }
//            trace.add(Trace.Session.SESSION_NAMESPACE_VALIDATION_SUCCESS)
//            val selfPublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()
//            val sessionTopic = crypto.generateTopicFromKeyAgreement(selfPublicKey, PublicKey(proposerPublicKey))
//            trace.add(Trace.Session.CREATE_SESSION_TOPIC)
//            val approvalParams = proposal.toSessionApproveParams(selfPublicKey)
//
//            //settlement
//            val selfParticipant = SessionParticipant(selfPublicKey.keyAsHex, selfAppMetaData)
//            val sessionExpiry = ACTIVE_SESSION
//            val unacknowledgedSession =
//                SessionVO.createUnacknowledgedSession(
//                    sessionTopic,
//                    proposal,
//                    selfParticipant,
//                    sessionExpiry,
//                    sessionNamespaces,
//                    scopedProperties,
//                    sessionProperties,
//                    pairingTopic
//                )
//
//            sessionStorageRepository.insertSession(unacknowledgedSession, request.id)
//            metadataStorageRepository.insertOrAbortMetadata(sessionTopic, selfAppMetaData, AppMetaDataType.SELF)
//            metadataStorageRepository.insertOrAbortMetadata(sessionTopic, proposal.appMetaData, AppMetaDataType.PEER)
//            trace.add(Trace.Session.STORE_SESSION)
//            val params = proposal.toSessionSettleParams(selfParticipant, sessionExpiry, sessionNamespaces, sessionProperties, scopedProperties)
//            val sessionSettle = SignRpc.SessionSettle(params = params)
//            trace.add(Trace.Session.PUBLISHING_SESSION_APPROVE)
//            jsonRpcInteractor.approveSession(
//                pairingTopic = proposal.pairingTopic,
//                sessionTopic = sessionTopic,
//                sessionProposalResponse = approvalParams,
//                settleRequest = sessionSettle,
//                correlationId = proposal.requestId,
//                onSuccess = {
//                    onSuccess()
//                    scope.launch {
//                        supervisorScope {
//                            trace.add(Trace.Session.SESSION_APPROVE_SUCCESS)
//                            proposalStorageRepository.deleteProposal(proposerPublicKey)
//                            verifyContextStorageRepository.delete(proposal.requestId)
//                        }
//                    }
//                },
//                onFailure = { error ->
//                    onFailure(error)
//                    scope.launch {
//                        supervisorScope {
//                            insertEventUseCase(
//                                Props(
//                                    type = EventType.Error.SESSION_APPROVE_FAILURE,
//                                    properties = Properties(trace = trace, topic = sessionTopic.value)
//                                )
//                            ).also { logger.error("Session approve failure, topic: ${sessionTopic.value}") }
//                        }
//                    }
//                }
//            )
//        } catch (e: Exception) {
//            if (e is NoRelayConnectionException) {
//                insertEventUseCase(Props(type = EventType.Error.NO_WSS_CONNECTION, properties = Properties(trace = trace, topic = pairingTopic)))
//            }
//            if (e is NoInternetConnectionException) {
//                insertEventUseCase(Props(type = EventType.Error.NO_INTERNET_CONNECTION, properties = Properties(trace = trace, topic = pairingTopic)))
//            }
//            onFailure(e)
//        }
//    }
//}

internal interface ApproveSessionUseCaseInterface {
    suspend fun approve(
        proposerPublicKey: String,
        sessionNamespaces: Map<String, EngineDO.Namespace.Session>,
        sessionProperties: Map<String, String>? = null,
        scopedProperties: Map<String, String>? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )
}