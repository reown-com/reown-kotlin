package com.reown.sample.wallet.nfc

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import timber.log.Timber
import java.io.ByteArrayOutputStream

/**
 * HCE service that receives payment URLs from a POS terminal via NFC.
 *
 * The POS terminal (e.g., Ingenico AXIUM DX8000) acts as an NFC reader
 * and sends the payment URL using a custom APDU protocol:
 *
 * 1. POS sends SELECT with Reown Pay AID (F052454F574E504159)
 * 2. POS sends one or more PUSH_URL APDUs containing the URL bytes
 * 3. This service reassembles the URL and launches the wallet Activity
 */
class PaymentHceService : HostApduService() {

    companion object {
        private val REOWN_PAY_AID = byteArrayOf(
            0xF0.toByte(), 0x52.toByte(), 0x45.toByte(), 0x4F.toByte(),
            0x57.toByte(), 0x4E.toByte(), 0x50.toByte(), 0x41.toByte(),
            0x59.toByte()
        ) // F052454F574E504159

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_WRONG_PARAMS = byteArrayOf(0x6B.toByte(), 0x00.toByte())
        private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())

        private const val INS_SELECT = 0xA4.toByte()
        private const val INS_PUSH_URL: Byte = 0x01

        /** Broadcast action sent when a payment URL is received via NFC. */
        const val ACTION_PAYMENT_URL_RECEIVED = "com.reown.wallet.NFC_PAYMENT_URL"
        const val EXTRA_PAYMENT_URL = "payment_url"
    }

    private var appSelected = false
    private val urlBuffer = ByteArrayOutputStream()

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Timber.d("NFC: <<< APDU (%d bytes): %s", commandApdu.size, commandApdu.toHexString())

        if (commandApdu.size < 4) return SW_WRONG_PARAMS

        val ins = commandApdu[1]

        val response = when (ins) {
            INS_SELECT -> handleSelect(commandApdu)
            INS_PUSH_URL -> handlePushUrl(commandApdu)
            else -> {
                Timber.w("NFC: Unsupported INS: 0x%02X", ins)
                SW_INS_NOT_SUPPORTED
            }
        }

        Timber.d("NFC: >>> Response: %s", response.toHexString())
        return response
    }

    private fun handleSelect(apdu: ByteArray): ByteArray {
        val p1 = apdu[2]
        if (p1 != 0x04.toByte()) return SW_NOT_FOUND

        val dataLen = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
        if (dataLen == 0 || apdu.size < 5 + dataLen) return SW_NOT_FOUND

        val aidData = apdu.copyOfRange(5, 5 + dataLen)
        if (!aidData.contentEquals(REOWN_PAY_AID)) {
            Timber.w("NFC: Unknown AID: %s", aidData.toHexString())
            return SW_NOT_FOUND
        }

        Timber.d("NFC: Reown Pay AID selected")
        appSelected = true
        urlBuffer.reset()
        return SW_OK
    }

    private fun handlePushUrl(apdu: ByteArray): ByteArray {
        if (!appSelected) {
            Timber.w("NFC: PUSH_URL before SELECT")
            return SW_NOT_FOUND
        }

        val p1 = apdu[2] // 0x01 = more chunks, 0x00 = last chunk
        val dataLen = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
        if (dataLen == 0 || apdu.size < 5 + dataLen) return SW_WRONG_PARAMS

        val data = apdu.copyOfRange(5, 5 + dataLen)
        urlBuffer.write(data)

        if (p1 == 0x00.toByte()) {
            // Last chunk — assemble and deliver the URL
            val paymentUrl = urlBuffer.toString(Charsets.UTF_8.name())
            urlBuffer.reset()
            appSelected = false
            Timber.d("NFC: Payment URL received: %s", paymentUrl)
            deliverPaymentUrl(paymentUrl)
        } else {
            Timber.d("NFC: URL chunk received (%d bytes, more coming)", dataLen)
        }

        return SW_OK
    }

    private fun deliverPaymentUrl(url: String) {
        // Launch wallet activity with the payment URL
        val intent = Intent(this, Class.forName("com.reown.sample.wallet.ui.WalletKitActivity")).apply {
            action = ACTION_PAYMENT_URL_RECEIVED
            putExtra(EXTRA_PAYMENT_URL, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "LINK_LOSS"
            DEACTIVATION_DESELECTED -> "DESELECTED"
            else -> "UNKNOWN($reason)"
        }
        Timber.d("NFC: Deactivated — %s", reasonStr)
        appSelected = false
        urlBuffer.reset()
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}
