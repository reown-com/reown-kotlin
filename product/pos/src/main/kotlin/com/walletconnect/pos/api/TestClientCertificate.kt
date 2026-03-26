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
