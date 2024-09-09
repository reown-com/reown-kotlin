package com.reown.sign.engine.use_case.requests

import com.reown.android.internal.common.model.IrnParams
import com.reown.android.internal.common.model.Tags
import com.reown.android.internal.common.model.WCRequest
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.utils.thirtySeconds
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.util.Logger
import kotlinx.coroutines.supervisorScope

internal class OnPingUseCase(private val jsonRpcInteractor: RelayJsonRpcInteractorInterface, private val logger: Logger) {

    suspend operator fun invoke(request: WCRequest) = supervisorScope {
        val irnParams = IrnParams(Tags.SESSION_PING_RESPONSE, Ttl(thirtySeconds))
        logger.log("Session ping received on topic: ${request.topic}")
        jsonRpcInteractor.respondWithSuccess(request, irnParams)
    }
}