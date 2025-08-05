package com.reown.android.pairing.handler

import com.reown.android.Core
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.model.SDKError
import com.reown.foundation.common.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import uniffi.yttrium.SessionProposalFfi

interface PairingControllerInterface {
    val findWrongMethodsFlow: Flow<SDKError>
    val storedPairingFlow: SharedFlow<Pair<Topic, MutableList<String>>>
    val checkVerifyKeyFlow: SharedFlow<Unit>

    fun initialize()

    fun setRequestReceived(activate: Core.Params.RequestReceived, onError: (Core.Model.Error) -> Unit = {})

    fun updateMetadata(updateMetadata: Core.Params.UpdateMetadata, onError: (Core.Model.Error) -> Unit = {})

    fun deleteAndUnsubscribePairing(deletePairing: Core.Params.Delete, onError: (Core.Model.Error) -> Unit = {})

    fun register(vararg method: String)

    fun getPairingByTopic(topic: Topic): Pairing?


    val sessionProposalFlow: SharedFlow<SessionProposalFfi>
}