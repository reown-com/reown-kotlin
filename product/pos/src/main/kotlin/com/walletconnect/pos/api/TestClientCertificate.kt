@file:JvmSynthetic

package com.walletconnect.pos.api

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.InputStream

internal object TestClientCertificate {
    const val PASSWORD = "changeit"

    private const val P12_BASE64 =
        "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"

    fun asInputStream(): InputStream =
        ByteArrayInputStream(Base64.decode(P12_BASE64, Base64.NO_WRAP))
}
