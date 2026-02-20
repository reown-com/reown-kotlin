@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import timber.log.Timber

/**
 * Manages NFC NDEF emulation state for the POS payment flow.
 *
 * When a payment is created and a QR code is displayed, call [updateNdefMessage]
 * with the payment gateway URI. This enables the POS device to also serve
 * the payment link via NFC tap. Call [clearNdefMessage] when the payment
 * completes, is cancelled, or errors out.
 */
internal object NfcManager {

    /**
     * Sets the NDEF message on the HCE service to contain a URI record
     * with the given payment URL.
     *
     * @param uri The payment gateway URL to encode in the NDEF record
     */
    fun updateNdefMessage(uri: String) {
        try {
            val ndefRecord = NdefRecord.createUri(uri)
            val ndefMessage = NdefMessage(arrayOf(ndefRecord))
            NdefHostApduService.currentNdefMessage = ndefMessage.toByteArray()
            Timber.d("NFC: NDEF message updated with URI: %s", uri)
        } catch (e: Exception) {
            Timber.e(e, "NFC: Failed to create NDEF message")
        }
    }

    /**
     * Clears the NDEF message so the HCE service no longer serves a payment URI.
     */
    fun clearNdefMessage() {
        NdefHostApduService.currentNdefMessage = null
        Timber.d("NFC: NDEF message cleared")
    }

    /**
     * Checks if the device has NFC hardware.
     */
    fun isNfcAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    /**
     * Checks if NFC is currently enabled in device settings.
     */
    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return adapter.isEnabled
    }

    /**
     * Checks if the device supports Host-based Card Emulation.
     */
    fun isHceSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
    }
}
