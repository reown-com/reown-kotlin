package com.walletconnect.sample.pos.printer

import android.graphics.Bitmap

internal interface Printer {
    suspend fun isAvailable(): Boolean
    suspend fun print(receipt: ReceiptData, logo: Bitmap): Result<Unit>
}
