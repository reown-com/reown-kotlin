package com.walletconnect.auth.use_case.requests

import com.walletconnect.android.internal.common.exception.Invalid
import com.walletconnect.android.internal.common.exception.Uncategorized
import com.walletconnect.android.internal.common.json_rpc.data.JsonRpcSerializer
import com.walletconnect.android.internal.common.model.IrnParams
import com.walletconnect.android.internal.common.model.SDKError
import com.walletconnect.android.internal.common.model.Tags
import com.walletconnect.android.internal.common.model.WCRequest
import com.walletconnect.android.internal.common.model.type.EngineEvent
import com.walletconnect.android.internal.common.model.type.JsonRpcInteractorInterface
import com.walletconnect.android.internal.common.scope
import com.walletconnect.android.internal.utils.CoreValidator
import com.walletconnect.android.internal.utils.DAY_IN_SECONDS
import com.walletconnect.android.verify.domain.ResolveAttestationIdUseCase
import com.walletconnect.auth.common.json_rpc.AuthParams
import com.walletconnect.auth.common.json_rpc.AuthRpc
import com.walletconnect.auth.common.model.Events
import com.walletconnect.foundation.common.model.Ttl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class OnAuthRequestUseCase(
    private val jsonRpcInteractor: JsonRpcInteractorInterface,
    private val serializer: JsonRpcSerializer,
    private val resolveAttestationIdUseCase: ResolveAttestationIdUseCase,
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    operator fun invoke(wcRequest: WCRequest, authParams: AuthParams.RequestParams) {
        val irnParams = IrnParams(Tags.AUTH_REQUEST_RESPONSE, Ttl(DAY_IN_SECONDS))
        try {
            if (!CoreValidator.isExpiryWithinBounds(authParams.expiry)) {
                jsonRpcInteractor.respondWithError(wcRequest, Invalid.RequestExpired, irnParams)
                return
            }

            val json = serializer.serialize(AuthRpc.AuthRequest(id = wcRequest.id, params = authParams)) ?: throw Exception("Error serializing session proposal")
            val url = authParams.requester.metadata.url
            resolveAttestationIdUseCase(wcRequest.id, json, url) { verifyContext ->
                scope.launch { _events.emit(Events.OnAuthRequest(wcRequest.id, wcRequest.topic.value, authParams.payloadParams, verifyContext)) }
            }
        } catch (e: Exception) {
            jsonRpcInteractor.respondWithError(
                wcRequest,
                Uncategorized.GenericError("Cannot handle a auth request: ${e.message}, topic: ${wcRequest.topic}"),
                irnParams
            )
            scope.launch { _events.emit(SDKError(e)) }
        }
    }
}