package com.reown.foundation.common.model

import com.reown.util.hexToBytes

interface Key {
    val keyAsHex: String
    val keyAsBytes: ByteArray
        get() = keyAsHex.hexToBytes()
}

@JvmInline
value class PublicKey(override val keyAsHex: String) : Key

@JvmInline
value class PrivateKey(override val keyAsHex: String) : Key