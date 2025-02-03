package com.reown.sign.engine.use_case.requests

import com.reown.android.Core
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.exception.Uncategorized
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.internal.utils.fiveMinutesInSeconds
import com.reown.android.pairing.handler.PairingControllerInterface
import com.reown.android.pulse.domain.InsertTelemetryEventUseCase
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Properties
import com.reown.android.pulse.model.properties.Props
import com.reown.android.verify.domain.ResolveAttestationIdUseCase
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.common.validator.SignValidator
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.mapper.toEngineDO
import com.reown.sign.engine.model.mapper.toPeerError
import com.reown.sign.engine.model.mapper.toVO
import com.reown.sign.json_rpc.model.JsonRpcMethod
import com.reown.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named

internal class OnSessionProposalUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val resolveAttestationIdUseCase: ResolveAttestationIdUseCase,
    private val pairingController: PairingControllerInterface,
    private val insertEventUseCase: InsertTelemetryEventUseCase,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()
    private val isAuthenticateEnabled: Boolean by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.ENABLE_AUTHENTICATE)) }

    suspend operator fun invoke(request: WCRequest, payloadParams: SignParams.SessionProposeParams) = supervisorScope {
        val irnParams = IrnParams(Tags.SESSION_PROPOSE_RESPONSE_AUTO_REJECT, Ttl(fiveMinutesInSeconds), correlationId = request.id)
        try {
            if (isSessionAuthenticateImplemented(request)) {
                logger.error("Session proposal received error: pairing supports authenticated sessions")
                return@supervisorScope
            }
            logger.log("Session proposal received: ${request.topic}")
            SignValidator.validateProposalNamespaces(payloadParams.requiredNamespaces) { error ->
                logger.error("Session proposal received error: required namespace validation: ${error.message}")
                insertEventUseCase(Props(type = EventType.Error.REQUIRED_NAMESPACE_VALIDATION_FAILURE, properties = Properties(topic = request.topic.value)))
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            SignValidator.validateProposalNamespaces(payloadParams.optionalNamespaces ?: emptyMap()) { error ->
                logger.error("Session proposal received error: optional namespace validation: ${error.message}")
                insertEventUseCase(Props(type = EventType.Error.OPTIONAL_NAMESPACE_VALIDATION_FAILURE, properties = Properties(topic = request.topic.value)))
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            payloadParams.properties?.let {
                SignValidator.validateProperties(payloadParams.properties) { error ->
                    logger.error("Session proposal received error: session properties validation: ${error.message}")
                    insertEventUseCase(Props(type = EventType.Error.SESSION_PROPERTIES_VALIDATION_FAILURE, properties = Properties(topic = request.topic.value)))
                    jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                    return@supervisorScope
                }
            }
            proposalStorageRepository.insertProposal(payloadParams.toVO(request.topic, request.id))
            pairingController.setRequestReceived(Core.Params.RequestReceived(request.topic.value))
            val url = payloadParams.proposer.metadata.url

            logger.log("Resolving session proposal attestation: ${System.currentTimeMillis()}")
            resolveAttestationIdUseCase(request, url, linkMode = request.transportType == TransportType.LINK_MODE, appLink = payloadParams.proposer.metadata.redirect?.universal) { verifyContext ->
                logger.log("Session proposal attestation resolved: ${System.currentTimeMillis()}")
                val sessionProposalEvent = EngineDO.SessionProposalEvent(proposal = payloadParams.toEngineDO(request.topic), context = verifyContext.toEngineDO())
                logger.log("Session proposal received on topic: ${request.topic} - emitting")
                scope.launch { _events.emit(sessionProposalEvent) }
            }
        } catch (e: Exception) {
            logger.error("Session proposal received error: $e")
            jsonRpcInteractor.respondWithError(
                request,
                Uncategorized.GenericError("Cannot handle a session proposal: ${e.message}, topic: ${request.topic}"),
                irnParams
            )
            _events.emit(SDKError(e))
        }
    }

    private fun isSessionAuthenticateImplemented(request: WCRequest): Boolean =
        pairingController.getPairingByTopic(request.topic)?.methods?.contains(JsonRpcMethod.WC_SESSION_AUTHENTICATE) == true && isAuthenticateEnabled
}