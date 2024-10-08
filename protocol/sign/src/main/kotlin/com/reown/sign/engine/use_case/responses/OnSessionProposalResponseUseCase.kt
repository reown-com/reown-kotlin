package com.reown.sign.engine.use_case.responses

import com.reown.android.Core
import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.SDKError
import com.reown.android.internal.common.model.WCResponse
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.android.internal.common.model.type.EngineEvent
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.pairing.handler.PairingControllerInterface
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class OnSessionProposalResponseUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val pairingController: PairingControllerInterface,
    private val crypto: KeyManagementRepository,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcResponse: WCResponse, params: SignParams.SessionProposeParams) = supervisorScope {
        try {
            logger.log("Session proposal response received on topic: ${wcResponse.topic}")
            val pairingTopic = wcResponse.topic
            pairingController.deleteAndUnsubscribePairing(Core.Params.Delete(pairingTopic.value))
            when (val response = wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> {
                    logger.log("Session proposal approval received on topic: ${wcResponse.topic}")
                    val selfPublicKey = PublicKey(params.proposer.publicKey)
                    val approveParams = response.result as CoreSignParams.ApprovalParams
                    val responderPublicKey = PublicKey(approveParams.responderPublicKey)
                    val sessionTopic = crypto.generateTopicFromKeyAgreement(selfPublicKey, responderPublicKey)

                    jsonRpcInteractor.subscribe(sessionTopic,
                        onSuccess = { logger.log("Session proposal approval subscribed on session topic: $sessionTopic") },
                        onFailure = { error ->
                            logger.error("Session proposal approval subscribe error on session topic: $sessionTopic - $error")
                            scope.launch { _events.emit(SDKError(error)) }
                        }
                    )
                }

                is JsonRpcResponse.JsonRpcError -> {
                    proposalStorageRepository.deleteProposal(params.proposer.publicKey)
                    logger.log("Session proposal rejection received on topic: ${wcResponse.topic}")
                    _events.emit(EngineDO.SessionRejected(pairingTopic.value, response.errorMessage))
                }
            }
        } catch (e: Exception) {
            logger.error("Session proposal response received failure on topic: ${wcResponse.topic}: $e")
            _events.emit(SDKError(e))
        }
    }
}