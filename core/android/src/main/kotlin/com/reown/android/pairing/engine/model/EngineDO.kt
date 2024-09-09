@file:JvmSynthetic

package com.reown.android.pairing.engine.model

import com.reown.android.internal.common.model.Pairing

internal sealed class EngineDO {

    @Deprecated(message = "This data object has been deprecated. It will be removed soon.")
    data class PairingDelete(
        val topic: String,
        val reason: String,
    ) : EngineDO()

    @Deprecated(message = "This data object has been deprecated. It will be removed soon.")
    data class PairingExpire(
        val pairing: Pairing
    ) : EngineDO()

    data class PairingState(
        val isPairingState: Boolean
    ) : EngineDO()
}