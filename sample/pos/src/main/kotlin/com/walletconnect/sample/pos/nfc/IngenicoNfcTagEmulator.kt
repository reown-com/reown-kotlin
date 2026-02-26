@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import com.usdk.apiservice.aidl.data.BytesValue
import com.usdk.apiservice.aidl.rfreader.IoCtrlCmd
import com.usdk.apiservice.aidl.rfreader.OnNFCTagListener
import com.usdk.apiservice.aidl.rfreader.RFError
import timber.log.Timber

/**
 * Wraps the Ingenico USDK NFC tag emulation API.
 *
 * When enabled, the POS device acts as an NDEF tag — both Android and iOS
 * wallets can read the payment URI by tapping the terminal.
 *
 * Uses [URFReader.ioControl] with [IoCtrlCmd.SetCardEmuSwitch], [IoCtrlCmd.SetCardEmuTimeout],
 * and [IoCtrlCmd.SetCardEmuInfo] to control the hardware emulation, plus [UNFCTag.startEventListener]
 * for read/timeout callbacks.
 */
internal object IngenicoNfcTagEmulator {

    private const val DEFAULT_TIMEOUT_SECONDS = 30

    @Volatile
    private var currentNdefBytes: ByteArray? = null

    @Volatile
    private var currentTimeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS

    @Volatile
    private var onBeingReadCallback: (() -> Unit)? = null

    @Volatile
    private var active: Boolean = false

    /**
     * Whether the USDK NFC tag emulation module is available on this device.
     */
    val isAvailable: Boolean
        get() = UsdkServiceHelper.isServiceBound &&
            UsdkServiceHelper.getRFReader() != null &&
            UsdkServiceHelper.getNFCTag() != null

    private val nfcTagListener = object : OnNFCTagListener.Stub() {
        override fun onTimeout() {
            Timber.d("NFC Tag Emulation: Timeout — re-arming")
            rearm()
        }

        override fun onBeingRead() {
            Timber.d("NFC Tag Emulation: Being read by external device — re-arming")
            onBeingReadCallback?.invoke()
            rearm()
        }
    }

    /**
     * Enables NFC tag emulation with the given NDEF payload.
     *
     * @param ndefBytes The raw NDEF message bytes to advertise.
     * @param timeoutSeconds How long to keep emulating before auto-timeout (re-arms automatically).
     * @param onBeingRead Optional callback invoked when an external device reads the tag.
     */
    fun enable(ndefBytes: ByteArray, timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS, onBeingRead: (() -> Unit)? = null) {
        currentNdefBytes = ndefBytes
        currentTimeoutSeconds = timeoutSeconds
        onBeingReadCallback = onBeingRead
        active = true

        val rfReader = UsdkServiceHelper.getRFReader()
        val nfcTag = UsdkServiceHelper.getNFCTag()
        if (rfReader == null || nfcTag == null) {
            Timber.w("NFC Tag Emulation: Module not available — running on non-Ingenico device?")
            return
        }

        try {
            // 1. Start event listener for onBeingRead / onTimeout callbacks
            nfcTag.startEventListener(nfcTagListener)

            val out = BytesValue()

            // 2. Enable card emulation mode
            var ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuSwitch, byteArrayOf(1), out)
            if (ret != RFError.SUCCESS) {
                Timber.e("NFC Tag Emulation: SetCardEmuSwitch(1) failed — error %d", ret)
                return
            }

            // 3. Set timeout
            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuTimeout, byteArrayOf(timeoutSeconds.toByte()), out)
            if (ret != RFError.SUCCESS) {
                Timber.e("NFC Tag Emulation: SetCardEmuTimeout(%d) failed — error %d", timeoutSeconds, ret)
                return
            }

            // 4. Set NDEF data
            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuInfo, ndefBytes, out)
            if (ret != RFError.SUCCESS) {
                Timber.e("NFC Tag Emulation: SetCardEmuInfo failed — error %d", ret)
                return
            }

            Timber.d("NFC Tag Emulation: Enabled (timeout=%ds, payload=%d bytes, hex=%s)",
                timeoutSeconds, ndefBytes.size, ndefBytes.toHexString())
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to enable")
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    /**
     * Disables NFC tag emulation.
     */
    fun disable() {
        active = false
        currentNdefBytes = null
        onBeingReadCallback = null

        val rfReader = UsdkServiceHelper.getRFReader()
        val nfcTag = UsdkServiceHelper.getNFCTag()
        if (rfReader == null || nfcTag == null) {
            Timber.d("NFC Tag Emulation: Module not available — nothing to disable")
            return
        }

        try {
            val out = BytesValue()
            val ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuSwitch, byteArrayOf(0), out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: SetCardEmuSwitch(0) failed — error %d", ret)
            }
            nfcTag.stopEventListener()
            Timber.d("NFC Tag Emulation: Disabled")
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to disable")
        }
    }

    /**
     * Updates the NDEF payload while emulation is active.
     * If not currently active, stores the data for the next [enable] call.
     */
    fun updateNdefData(ndefBytes: ByteArray) {
        currentNdefBytes = ndefBytes
        if (!active) return

        val rfReader = UsdkServiceHelper.getRFReader() ?: run {
            Timber.w("NFC Tag Emulation: RF reader not available — cannot update")
            return
        }

        try {
            val out = BytesValue()
            val ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuInfo, ndefBytes, out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: SetCardEmuInfo update failed — error %d", ret)
                return
            }
            Timber.d("NFC Tag Emulation: Updated payload (%d bytes)", ndefBytes.size)
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to update NDEF data")
        }
    }

    /**
     * Re-arms emulation after a timeout. Uses the last known NDEF payload.
     */
    private fun rearm() {
        if (!active) return
        val bytes = currentNdefBytes ?: return

        val rfReader = UsdkServiceHelper.getRFReader() ?: return
        try {
            val out = BytesValue()

            var ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuSwitch, byteArrayOf(1), out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: Re-arm SetCardEmuSwitch failed — error %d", ret)
                return
            }

            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuTimeout, byteArrayOf(currentTimeoutSeconds.toByte()), out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: Re-arm SetCardEmuTimeout failed — error %d", ret)
                return
            }

            ret = rfReader.ioControl(IoCtrlCmd.SetCardEmuInfo, bytes, out)
            if (ret != RFError.SUCCESS) {
                Timber.w("NFC Tag Emulation: Re-arm SetCardEmuInfo failed — error %d", ret)
                return
            }

            Timber.d("NFC Tag Emulation: Re-armed (timeout=%ds)", currentTimeoutSeconds)
        } catch (e: Exception) {
            Timber.e(e, "NFC Tag Emulation: Failed to re-arm")
        }
    }
}
