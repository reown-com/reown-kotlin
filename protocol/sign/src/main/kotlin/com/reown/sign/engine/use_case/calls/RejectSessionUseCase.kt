package com.reown.sign.engine.use_case.calls

import com.reown.android.Core
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.scope
import com.reown.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.reown.android.internal.utils.CoreValidator.isExpired
import com.reown.android.internal.utils.fiveMinutesInSeconds
import com.reown.android.pairing.handler.PairingControllerInterface
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.PeerError
import com.reown.sign.common.exceptions.SessionProposalExpiredException
import com.reown.sign.engine.model.mapper.toSessionProposeRequest
import com.reown.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RejectSessionUseCase(
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val pairingController: PairingControllerInterface,
    private val logger: Logger
) : RejectSessionUseCaseInterface {

    override suspend fun reject(proposerPublicKey: String, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        val proposal = proposalStorageRepository.getProposalByKey(proposerPublicKey)
        proposal.expiry?.let {
            if (it.isExpired()) {
                logger.error("Proposal expired on reject, topic: ${proposal.pairingTopic.value}, id: ${proposal.requestId}")
                throw SessionProposalExpiredException("Session proposal expired")
            }
        }

        logger.log("Sending session rejection, topic: ${proposal.pairingTopic.value}")
        jsonRpcInteractor.respondWithError(
            proposal.toSessionProposeRequest(),
            PeerError.EIP1193.UserRejectedRequest(reason),
            IrnParams(Tags.SESSION_PROPOSE_RESPONSE_REJECT, Ttl(fiveMinutesInSeconds)),
            onSuccess = {
                logger.log("Session rejection sent successfully, topic: ${proposal.pairingTopic.value}")
                scope.launch {
                    proposalStorageRepository.deleteProposal(proposerPublicKey)
                    verifyContextStorageRepository.delete(proposal.requestId)
                    pairingController.deleteAndUnsubscribePairing(Core.Params.Delete(proposal.pairingTopic.value))
                }
                onSuccess()
            },
            onFailure = { error ->
                logger.error("Session rejection sent failure, topic: ${proposal.pairingTopic.value}. Error: $error")
                onFailure(error)
            })
    }
}

internal interface RejectSessionUseCaseInterface {
    suspend fun reject(proposerPublicKey: String, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit = {})
}