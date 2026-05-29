package com.walletconnect.sample.pos.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.model.PosVariant

internal class PrinterManager(private val context: Context) {

    private val printer: Printer = GenericBluetoothPrinter(context)

    suspend fun print(receipt: ReceiptData, variant: PosVariant): Result<Unit> =
        printer.print(receipt, logoBitmap(variant))

    // A single element of the co-branded lockup, sized in dp (matches BrandLogoRow proportions).
    private data class LogoElement(val drawableRes: Int, val widthDp: Int, val heightDp: Int)

    /**
     * Builds the receipt header logo as a single monochrome bitmap, mirroring the on-screen
     * [com.walletconnect.sample.pos.components.BrandLogoRow]: WCPay logo, and when the selected
     * variant carries a partner logo, a "+" separator followed by the partner logo.
     *
     * Every drawable is tinted black before drawing — the partner logos are authored white for the
     * dark-theme UI, so without tinting they would render white-on-white and print blank.
     */
    private fun logoBitmap(variant: PosVariant): Bitmap {
        val elements = buildList {
            add(LogoElement(R.drawable.ic_wcpay_logo, WCPAY_WIDTH_DP, WCPAY_HEIGHT_DP))
            variant.partnerLogoRes?.let { partnerRes ->
                add(LogoElement(R.drawable.ic_plus_header, PLUS_SIZE_DP, PLUS_SIZE_DP))
                add(LogoElement(partnerRes, variant.partnerLogoWidthDp, variant.partnerLogoHeightDp))
            }
        }

        // Lay out horizontally in dp, then scale uniformly so the whole lockup fits the print head.
        val totalWidthDp = elements.sumOf { it.widthDp } + GAP_DP * (elements.size - 1)
        val totalHeightDp = elements.maxOf { it.heightDp }
        val scale = LOGO_TARGET_WIDTH.toFloat() / totalWidthDp

        val canvasWidth = LOGO_TARGET_WIDTH
        val canvasHeight = (totalHeightDp * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var xDp = 0
        for (element in elements) {
            val drawable = loadBlackTinted(element.drawableRes)
            val left = (xDp * scale).toInt()
            val width = (element.widthDp * scale).toInt().coerceAtLeast(1)
            val height = (element.heightDp * scale).toInt().coerceAtLeast(1)
            val top = ((canvasHeight - height) / 2).coerceAtLeast(0) // vertically center against tallest
            drawable.setBounds(left, top, left + width, top + height)
            drawable.draw(canvas)
            xDp += element.widthDp + GAP_DP
        }

        return thresholdToMonochrome(bitmap)
    }

    private fun loadBlackTinted(drawableRes: Int): Drawable {
        val drawable = AppCompatResources.getDrawable(context, drawableRes)
            ?.mutate()
            ?: error("Receipt logo drawable missing: $drawableRes")
        DrawableCompat.setTint(drawable, Color.BLACK)
        return drawable
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

        // Lockup proportions in dp — mirror BrandLogoRow (WCPay 60x18, plus 20x20, spacing2 = 8dp gap)
        private const val WCPAY_WIDTH_DP = 60
        private const val WCPAY_HEIGHT_DP = 18
        private const val PLUS_SIZE_DP = 20
        private const val GAP_DP = 8
    }
}
