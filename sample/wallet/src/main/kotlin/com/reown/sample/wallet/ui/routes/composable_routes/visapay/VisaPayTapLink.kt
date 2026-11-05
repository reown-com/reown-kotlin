package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Parses the VisaPay tap-to-pay link the Kotlin sample POS serves over NFC (HCE), e.g.
 * `visapay://pay?ref=ORDER-1&amount=12.50&currency=USD&display=$12.50&merchant=Sample%20Coffee%20Bar`.
 *
 * The NFC read is real; the payment that follows is mocked.
 */
object VisaPayTapLink {
    private const val SCHEME = "visapay"
    private const val HOST = "pay"

    fun isTapLink(url: String): Boolean =
        runCatching { Uri.parse(url) }.getOrNull()?.let { it.scheme == SCHEME && it.host == HOST } ?: false

    fun parse(url: String): VisaPayPaymentRequest? {
        if (!isTapLink(url)) return null
        val uri = Uri.parse(url)
        val amount = uri.getQueryParameter("amount")?.toDoubleOrNull() ?: return null
        return VisaPayPaymentRequest(
            merchant = uri.getQueryParameter("merchant") ?: "Point of sale",
            category = "In-store",
            amount = amount,
            fiatLabel = uri.getQueryParameter("display"),
            reference = uri.getQueryParameter("ref"),
            isContactless = true,
        )
    }
}

/** Terminal-style sounds on the wallet: a beep when the tap is read, a chime when the payment is approved, a low tone when it's declined. */
object VisaPaySounds {
    private const val TAP_DURATION_MS = 180
    private const val APPROVED_DURATION_MS = 500

    fun playTap() = play(ToneGenerator.TONE_PROP_BEEP, TAP_DURATION_MS)

    fun playApproved() = play(ToneGenerator.TONE_PROP_ACK, APPROVED_DURATION_MS)

    fun playDeclined() = play(ToneGenerator.TONE_PROP_NACK, APPROVED_DURATION_MS)

    private fun play(tone: Int, durationMs: Int) {
        val generator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME) }.getOrNull() ?: return
        generator.startTone(tone, durationMs)
        Handler(Looper.getMainLooper()).postDelayed({ generator.release() }, durationMs + 200L)
    }
}
