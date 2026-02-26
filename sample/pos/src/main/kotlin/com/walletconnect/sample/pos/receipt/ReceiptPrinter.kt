@file:JvmSynthetic

package com.walletconnect.sample.pos.receipt

import android.os.Bundle
import com.usdk.apiservice.aidl.vectorprinter.Alignment
import com.usdk.apiservice.aidl.vectorprinter.OnPrintListener
import com.usdk.apiservice.aidl.vectorprinter.TextSize
import com.usdk.apiservice.aidl.vectorprinter.VectorPrinterData
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.nfc.UsdkServiceHelper
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ReceiptPrinter {

    val isAvailable: Boolean
        get() = UsdkServiceHelper.isServiceBound &&
            UsdkServiceHelper.getVectorPrinter() != null

    fun printPaymentReceipt(
        paymentInfo: Pos.PaymentInfo?,
        fiatAmount: String?,
        onFinish: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val printer = UsdkServiceHelper.getVectorPrinter()
        if (printer == null) {
            Timber.w("Receipt: Printer not available")
            onError("Printer not available")
            return
        }

        try {
            // Init with auto-cut
            printer.init(Bundle().apply {
                putBoolean(VectorPrinterData.AUTO_CUT_PAPER, true)
            })

            // Header
            val headerFmt = Bundle().apply {
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.LARGE)
                putInt(VectorPrinterData.ALIGNMENT, Alignment.CENTER)
                putBoolean(VectorPrinterData.BOLD, true)
            }
            printer.addText(headerFmt, "WalletConnect\nPAY RECEIPT\n")

            // Separator
            val centerFmt = Bundle().apply {
                putInt(VectorPrinterData.ALIGNMENT, Alignment.CENTER)
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.NORMAL)
            }
            printer.addText(centerFmt, "──────────────────────\n")

            if (paymentInfo != null) {
                val normalFmt = Bundle().apply {
                    putInt(VectorPrinterData.TEXT_SIZE, TextSize.NORMAL)
                }
                val weights = intArrayOf(1, 1)
                val aligns = intArrayOf(Alignment.NORMAL, Alignment.OPPOSITE)

                // Crypto amount
                val cryptoAmount = paymentInfo.formatAmount()
                if (cryptoAmount.isNotBlank()) {
                    printer.addTextColumns(normalFmt, arrayOf("Amount:", cryptoAmount), weights, aligns)
                }

                // Fiat amount
                if (fiatAmount != null) {
                    printer.addTextColumns(normalFmt, arrayOf("Fiat:", fiatAmount), weights, aligns)
                }

                // Asset
                if (paymentInfo.assetName != null) {
                    printer.addTextColumns(normalFmt, arrayOf("Asset:", paymentInfo.assetName), weights, aligns)
                }

                // Network
                if (paymentInfo.networkName != null) {
                    printer.addTextColumns(normalFmt, arrayOf("Network:", paymentInfo.networkName), weights, aligns)
                }

                // Separator
                printer.addText(centerFmt, "──────────────────────\n")

                // Tx Hash
                val smallFmt = Bundle().apply {
                    putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)
                    putInt(VectorPrinterData.ALIGNMENT, Alignment.NORMAL)
                }
                printer.addText(normalFmt, "Tx Hash:\n")
                printer.addText(smallFmt, "${paymentInfo.txHash}\n")
            }

            // Separator + timestamp
            printer.addText(centerFmt, "──────────────────────\n")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val smallCenter = Bundle().apply {
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)
                putInt(VectorPrinterData.ALIGNMENT, Alignment.CENTER)
            }
            printer.addText(smallCenter, "$timestamp\n")

            // Feed paper before cut
            printer.feedPix(50)

            // Print
            printer.startPrint(object : OnPrintListener.Stub() {
                override fun onStart() {
                    Timber.d("Receipt: Printing started")
                }

                override fun onFinish() {
                    Timber.d("Receipt: Printing finished")
                    onFinish()
                }

                override fun onError(error: Int, errorMsg: String?) {
                    Timber.e("Receipt: Print error %d — %s", error, errorMsg)
                    onError(errorMsg ?: "Print error $error")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "Receipt: Failed to print")
            onError(e.message ?: "Print failed")
        }
    }
}
