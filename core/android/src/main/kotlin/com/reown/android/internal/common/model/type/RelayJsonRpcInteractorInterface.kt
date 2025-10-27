package com.reown.android.internal.common.model.type

import com.reown.android.internal.common.JsonRpcResponse
import com.reown.android.internal.common.model.EnvelopeType
import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Participants
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.params.CoreSignParams
import com.reown.android.relay.WSSConnectionState
import com.reown.foundation.common.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RelayJsonRpcInteractorInterface : JsonRpcInteractorInterface {
    val wssConnectionState: StateFlow<WSSConnectionState>
    val onResubscribe: Flow<Any?>
    fun checkNetworkConnectivity()

    fun subscribe(topic: Topic, onSuccess: (Topic) -> Unit = {}, onFailure: (Throwable) -> Unit = {})

    fun batchSubscribe(topics: List<String>, onSuccess: (List<String>) -> Unit = {}, onFailure: (Throwable) -> Unit = {})

    fun unsubscribe(topic: Topic, onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {})

    fun proposeSession(
        topic: Topic,
        payload: JsonRpcClientSync<*>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )

    fun approveSession(
        pairingTopic: Topic,
        sessionTopic: Topic,
        sessionProposalResponse: CoreSignParams.ApprovalParams,
        settleRequest: JsonRpcClientSync<*>,
        approvedChains: List<String>? = null,
        approvedMethods: List<String>? = null,
        approvedEvents: List<String>? = null,
        sessionProperties: Map<String, String>? = null,
        scopedProperties: Map<String, String>? = null,
        correlationId: Long,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )

    fun publishJsonRpcRequest(
        topic: Topic,
        params: IrnParams,
        payload: JsonRpcClientSync<*>,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )

    fun publishJsonRpcResponse(
        topic: Topic,
        params: IrnParams,
        response: JsonRpcResponse,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
        participants: Participants? = null,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
    )

    fun respondWithParams(
        request: WCRequest,
        clientParams: ClientParams,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onFailure: (Throwable) -> Unit,
        onSuccess: () -> Unit = {}
    )

    fun respondWithParams(
        requestId: Long,
        topic: Topic,
        clientParams: ClientParams,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onFailure: (Throwable) -> Unit,
        onSuccess: () -> Unit = {}
    )

    fun respondWithSuccess(
        request: WCRequest,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
    )

    fun respondWithError(
        request: WCRequest,
        error: Error,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onSuccess: (WCRequest) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )

    fun respondWithError(
        requestId: Long,
        topic: Topic,
        error: Error,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )
}
