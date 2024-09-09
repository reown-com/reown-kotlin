package com.reown.android.internal.common.crypto.codec

import com.reown.android.internal.common.model.EnvelopeType
import com.reown.android.internal.common.model.Participants
import com.reown.foundation.common.model.Topic

interface Codec {
    fun encrypt(topic: Topic, payload: String, envelopeType: EnvelopeType, participants: Participants? = null): ByteArray
    fun decrypt(topic: Topic, cipherText: ByteArray): String
}