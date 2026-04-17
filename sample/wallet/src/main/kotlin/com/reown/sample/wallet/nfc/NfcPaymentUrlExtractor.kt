@file:JvmSynthetic

package com.reown.sample.wallet.nfc

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import timber.log.Timber

/**
 * Extracts payment URLs from NFC intents.
 *
 * Shared between [NfcPaymentReader] (foreground dispatch) and
 * [com.reown.sample.wallet.ui.NfcPaymentActivity] (manifest dispatch)
 * to avoid duplicating extraction logic.
 */
internal object NfcPaymentUrlExtractor {

    fun extractFromNdefExtras(intent: Intent): String? {
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null
        for (msg in messages) {
            val ndefMessage = msg as? NdefMessage ?: continue
            for (record in ndefMessage.records) {
                val uri = record.toUri()?.toString() ?: continue
                if (isPaymentUrl(uri)) return unwrapPaymentUrl(uri)
            }
        }
        return null
    }

    /**
     * Reads NDEF data from the tag. This performs blocking RF I/O
     * and must NOT be called on the main thread.
     */
    fun extractFromTag(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: return null
            ndefMessage.records.firstNotNullOfOrNull { record ->
                record.toUri()?.toString()?.takeIf { isPaymentUrl(it) }?.let { unwrapPaymentUrl(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "NFC: Error reading NDEF from tag")
            null
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    fun isPaymentUrl(url: String): Boolean =
        url.contains("pay.walletconnect.com")

    fun unwrapPaymentUrl(url: String): String =
        try { Uri.parse(url).getQueryParameter("payUrl") ?: url } catch (_: Exception) { url }
}
