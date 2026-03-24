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

            val centerFmt = Bundle().apply {
                putInt(VectorPrinterData.ALIGNMENT, Alignment.CENTER)
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.NORMAL)
            }

            // Header — Reown Pay logo text
            val headerFmt = Bundle().apply {
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.LARGE)
                putInt(VectorPrinterData.ALIGNMENT, Alignment.CENTER)
                putBoolean(VectorPrinterData.BOLD, true)
            }
            printer.addText(headerFmt, "WCPay\n")

            // Dotted separator
            printer.addText(centerFmt, "· · · · · · · · · · · · · · · · · · · · · · · ·\n\n")

            // Label style (small, left-aligned)
            val labelFmt = Bundle().apply {
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)
                putInt(VectorPrinterData.ALIGNMENT, Alignment.NORMAL)
            }
            // Value style (normal, bold, left-aligned)
            val valueFmt = Bundle().apply {
                putInt(VectorPrinterData.TEXT_SIZE, TextSize.NORMAL)
                putInt(VectorPrinterData.ALIGNMENT, Alignment.NORMAL)
                putBoolean(VectorPrinterData.BOLD, true)
            }

            if (paymentInfo != null) {
                // TXN ID
                printer.addText(labelFmt, "TXN ID\n")
                printer.addText(valueFmt, "${paymentInfo.txHash}\n\n")

                // DATE
                val dateStr = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
                printer.addText(labelFmt, "DATE\n")
                printer.addText(valueFmt, "$dateStr\n\n")

                // METHOD
                printer.addText(labelFmt, "METHOD\n")
                printer.addText(valueFmt, "WalletConnect Pay\n\n")

                // AMOUNT (fiat)
                if (fiatAmount != null) {
                    printer.addText(labelFmt, "AMOUNT\n")
                    printer.addText(valueFmt, "$fiatAmount\n\n")
                }

                // PAID WITH (crypto)
                val cryptoAmount = paymentInfo.formatAmount()
                if (cryptoAmount.isNotBlank()) {
                    printer.addText(labelFmt, "PAID WITH\n")
                    printer.addText(valueFmt, "$cryptoAmount\n\n")
                }

                // NETWORK
                if (paymentInfo.networkName != null) {
                    printer.addText(labelFmt, "NETWORK\n")
                    printer.addText(valueFmt, "${paymentInfo.networkName}\n\n")
                }
            } else {
                // Fallback when no payment info
                if (fiatAmount != null) {
                    printer.addText(labelFmt, "AMOUNT\n")
                    printer.addText(valueFmt, "$fiatAmount\n\n")
                }
                val dateStr = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
                printer.addText(labelFmt, "DATE\n")
                printer.addText(valueFmt, "$dateStr\n\n")
            }

            // Footer
            printer.addText(centerFmt, "\nThank you for your payment!\n")

            // Feed paper before cut
            printer.feedPix(80)

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
