package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.android.internal.common.exception.Reason
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.utils.dayInSeconds
import com.reown.foundation.common.model.Topic
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.reown.sign.common.model.vo.clientsync.session.SignRpc
import com.reown.sign.common.model.vo.clientsync.session.params.SignParams
import com.reown.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.supervisorScope

internal class DisconnectSessionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
) : DisconnectSessionUseCaseInterface {
    override suspend fun disconnect(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            logger.error("Sending session disconnect error: invalid session $topic")
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }

        val deleteParams = SignParams.DeleteParams(Reason.UserDisconnected.code, Reason.UserDisconnected.message)
        val sessionDelete = SignRpc.SessionDelete(params = deleteParams)
        val irnParams = IrnParams(Tags.SESSION_DELETE, Ttl(dayInSeconds), correlationId = sessionDelete.id.toString())

        logger.log("Sending session disconnect on topic: $topic")
        jsonRpcInteractor.publishJsonRpcRequest(Topic(topic), irnParams, sessionDelete,
            onSuccess = {
                logger.log("Disconnect sent successfully on topic: $topic")
                sessionStorageRepository.deleteSession(Topic(topic))
                jsonRpcInteractor.unsubscribe(Topic(topic))
                onSuccess()
            },
            onFailure = { error ->
                logger.error("Sending session disconnect error: $error on topic: $topic")
                onFailure(error)
            }
        )
    }
}

internal interface DisconnectSessionUseCaseInterface {
    suspend fun disconnect(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}