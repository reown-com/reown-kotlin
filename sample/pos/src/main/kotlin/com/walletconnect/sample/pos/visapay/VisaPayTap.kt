@file:JvmSynthetic

package com.walletconnect.sample.pos.visapay

import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Mocked VisaPay tap-to-pay contract between this POS and the Kotlin sample wallet.
 *
 * The only real part is the NFC hand-off: the POS serves this link as an NDEF URI record
 * via HCE, and the wallet reads it when tapped. Everything after the tap is mocked on both sides.
 */
internal object VisaPayTapLink {
    private const val SCHEME = "visapay"
    private const val HOST = "pay"
    const val MERCHANT_NAME = "Sample Coffee Bar"

    fun build(reference: String, amountMajor: String, currencyCode: String, displayAmount: String): String =
        Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendQueryParameter("ref", reference)
            .appendQueryParameter("amount", amountMajor)
            .appendQueryParameter("currency", currencyCode)
            .appendQueryParameter("display", displayAmount)
            .appendQueryParameter("merchant", MERCHANT_NAME)
            .build()
            .toString()
}

/**
 * Mocked stages after the tap. Durations add up to 5 seconds and mirror the wallet's
 * payment sheet so both devices finish at roughly the same time.
 */
internal enum class VisaPayTapStage(val title: String, val durationMs: Long) {
    WAITING_FOR_TAP("Waiting for tap", 0L),
    READING_WALLET("Reading wallet", 1_000L),
    AUTHORIZING("Authorizing with Visa", 1_500L),
    COLLECTING_FUNDS("Collecting USDC via Permit2", 1_500L),
    SETTLING("Settling payment", 1_000L),
    APPROVED("Approved", 0L);

    val isProcessing: Boolean get() = this != WAITING_FOR_TAP && this != APPROVED

    companion object {
        val processingStages: List<VisaPayTapStage> = entries.filter { it.isProcessing }
        val processingDurationMs: Int = processingStages.sumOf { it.durationMs }.toInt()
    }
}

/** Short terminal-style sounds: a beep when the wallet taps, a chime when the payment is approved. */
internal object VisaPayTapSounds {
    private const val TAP_DURATION_MS = 180
    private const val APPROVED_DURATION_MS = 500

    fun playTap() = play(ToneGenerator.TONE_PROP_BEEP, TAP_DURATION_MS)

    fun playApproved() = play(ToneGenerator.TONE_PROP_ACK, APPROVED_DURATION_MS)

    private fun play(tone: Int, durationMs: Int) {
        val generator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME) }.getOrNull() ?: return
        generator.startTone(tone, durationMs)
        Handler(Looper.getMainLooper()).postDelayed({ generator.release() }, durationMs + 200L)
    }
}
