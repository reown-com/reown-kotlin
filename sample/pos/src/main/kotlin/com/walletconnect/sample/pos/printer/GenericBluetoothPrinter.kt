package com.walletconnect.sample.pos.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GenericBluetoothPrinter(private val context: Context) : Printer {

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = manager?.adapter
        adapter?.isEnabled == true && firstPairedConnection() != null
    }

    override suspend fun print(receipt: ReceiptData, logo: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = firstPairedConnection()
                ?: error("No paired Bluetooth printer found. Pair a thermal printer in Android settings first.")
            val printer = EscPosPrinter(connection, PRINTER_DPI, PRINTER_WIDTH_MM, PRINTER_CHARS_PER_LINE)
            try {
                val logoMarkup = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, BitmapDrawable(context.resources, logo)) + "</img>\n"
                printer.printFormattedTextAndCut(logoMarkup + ReceiptFormatter.toGenericMarkup(receipt))
            } finally {
                printer.disconnectPrinter()
            }
            Unit
        }
    }

    private fun firstPairedConnection(): BluetoothConnection? = try {
        BluetoothPrintersConnections.selectFirstPaired()
    } catch (e: SecurityException) {
        null
    }

    companion object {
        private const val PRINTER_DPI = 203
        private const val PRINTER_WIDTH_MM = 48f
        private const val PRINTER_CHARS_PER_LINE = 32
    }
}
