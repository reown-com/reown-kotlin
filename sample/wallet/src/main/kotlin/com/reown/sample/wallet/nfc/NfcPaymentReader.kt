@file:JvmSynthetic

package com.reown.sample.wallet.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import timber.log.Timber

/**
 * Reads payment URLs from NFC tags using Android's reader mode API.
 *
 * When enabled, the phone actively polls for NFC tags and reads NDEF data.
 * This is the Android equivalent of iOS's CoreNFC NFCNDEFReaderSession.
 *
 * The POS terminal emulates an NDEF tag containing the payment URL.
 * This reader detects it and delivers the URL to the wallet.
 */
internal class NfcPaymentReader(
    private val activity: Activity,
    private val onPaymentUrl: (String) -> Unit,
) : NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    /**
     * Enables NFC reader mode. Call from Activity.onResume().
     * Puts the NFC controller into active polling mode so it can detect
     * the POS terminal's emulated NDEF tag.
     */
    fun enable() {
        nfcAdapter?.enableReaderMode(
            activity,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        Timber.d("NFC Reader: Enabled")
    }

    /**
     * Disables NFC reader mode. Call from Activity.onPause().
     */
    fun disable() {
        nfcAdapter?.disableReaderMode(activity)
        Timber.d("NFC Reader: Disabled")
    }

    override fun onTagDiscovered(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Timber.d("NFC Reader: Tag detected but no NDEF support")
            return
        }

        try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage
            if (ndefMessage == null) {
                Timber.d("NFC Reader: No NDEF message on tag")
                return
            }

            for (record in ndefMessage.records) {
                val uri = record.toUri()?.toString() ?: continue
                if (isPaymentUrl(uri)) {
                    Timber.d("NFC Reader: Payment URL found: %s", uri)
                    activity.runOnUiThread { onPaymentUrl(uri) }
                    return
                }
            }

            Timber.d("NFC Reader: Tag read but no payment URL found")
        } catch (e: Exception) {
            Timber.e(e, "NFC Reader: Error reading tag")
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) { }
        }
    }

    private fun isPaymentUrl(url: String): Boolean =
        url.contains("pay.walletconnect.com")
}
