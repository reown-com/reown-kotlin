@file:JvmSynthetic

package com.walletconnect.sample.pos.sound

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import timber.log.Timber

internal object TapSoundPlayer {

    fun playTapSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 300)
        } catch (e: Exception) {
            Timber.w(e, "Failed to play tap sound")
        }
    }
}
