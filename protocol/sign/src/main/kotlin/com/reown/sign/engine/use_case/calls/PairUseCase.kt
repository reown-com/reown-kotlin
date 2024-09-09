package com.reown.sign.engine.use_case.calls

import com.reown.android.Core
import com.reown.android.pairing.client.PairingInterface
import kotlinx.coroutines.supervisorScope

internal class PairUseCase(private val pairingInterface: PairingInterface) : PairUseCaseInterface {

    override suspend fun pair(uri: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        pairingInterface.pair(pair = Core.Params.Pair(uri), onSuccess = { onSuccess() }, onError = { error -> onFailure(error.throwable) })
    }
}

internal interface PairUseCaseInterface {
    suspend fun pair(uri: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}