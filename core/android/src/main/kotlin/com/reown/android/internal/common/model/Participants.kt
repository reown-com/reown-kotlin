package com.reown.android.internal.common.model

import com.reown.foundation.common.model.PublicKey

data class Participants(
    val senderPublicKey: PublicKey,
    val receiverPublicKey: PublicKey,
)