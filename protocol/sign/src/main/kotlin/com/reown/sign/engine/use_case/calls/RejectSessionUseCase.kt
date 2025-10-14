package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.reown.android.internal.utils.CoreValidator.isExpired
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.SessionProposalExpiredException
import com.reown.sign.engine.model.mapper.toProposalFfi
import com.reown.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import uniffi.yttrium.RejectionReason
import uniffi.yttrium.SignClient

internal class RejectSessionUseCase(
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val logger: Logger,
    private val signClient: SignClient,
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
        try {
            async {
                signClient.reject(
                    proposal = proposal.toProposalFfi(),
                    reason = RejectionReason.USER_REJECTED
                )
            }.await()
            
            logger.log("Session rejection sent successfully, topic: ${proposal.pairingTopic.value}")
            
            // Cleanup storage
            launch {
                proposalStorageRepository.deleteProposal(proposerPublicKey)
                verifyContextStorageRepository.delete(proposal.requestId)
            }
            
            onSuccess()
        } catch (e: Exception) {
            logger.error("Session rejection sent failure, topic: ${proposal.pairingTopic.value}. Error: $e")
            onFailure(e)
        }
    }
}

internal interface RejectSessionUseCaseInterface {
    suspend fun reject(proposerPublicKey: String, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit = {})
}