package com.walletconnect.sample.pos.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.walletconnect.sample.pos.R

internal class PrinterManager(private val context: Context) {

    private val printer: Printer = GenericBluetoothPrinter(context)

    suspend fun print(receipt: ReceiptData): Result<Unit> = printer.print(receipt, logoBitmap())

    private fun logoBitmap(): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_wcpay_logo)
            ?: error("Receipt logo drawable missing: ic_wcpay_logo")
        val srcWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: LOGO_TARGET_WIDTH
        val srcHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: (LOGO_TARGET_WIDTH / 3)
        val targetHeight = (LOGO_TARGET_WIDTH * srcHeight / srcWidth).coerceAtLeast(1)

        // Render the vector at target printer resolution onto a white background — vectors stay crisp
        // at any size, unlike upscaling a small raster which produces blurry edges after thresholding.
        val bitmap = Bitmap.createBitmap(LOGO_TARGET_WIDTH, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawable.setBounds(0, 0, LOGO_TARGET_WIDTH, targetHeight)
        drawable.draw(canvas)

        return thresholdToMonochrome(bitmap)
    }

    private fun thresholdToMonochrome(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = if (luma < LUMA_THRESHOLD) Color.BLACK else Color.WHITE
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    companion object {
        // Target a width that fits comfortably inside the 384-dot, 48mm @ 203 DPI thermal head
        private const val LOGO_TARGET_WIDTH = 320
        private const val LUMA_THRESHOLD = 160
    }
}
