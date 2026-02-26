@file:JvmSynthetic

package com.walletconnect.sample.pos.nfc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import com.usdk.apiservice.aidl.DeviceServiceData
import com.usdk.apiservice.aidl.ModuleName
import com.usdk.apiservice.aidl.UDeviceService
import com.usdk.apiservice.aidl.constants.RFDeviceName
import com.usdk.apiservice.aidl.rfreader.UNFCTag
import com.usdk.apiservice.aidl.rfreader.URFReader
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter
import com.usdk.apiservice.limited.DeviceServiceLimited
import timber.log.Timber

/**
 * Singleton managing the AIDL connection to the Ingenico USDK `com.usdk.apiservice`.
 * Provides access to NFC hardware modules (RF reader and NFC tag emulation).
 */
internal object UsdkServiceHelper {

    private const val USDK_SERVICE_ACTION = "com.usdk.apiservice"
    private const val USDK_SERVICE_PACKAGE = "com.usdk.apiservice"
    private const val MAX_RETRIES = 3
    private const val RETRY_INTERVAL_MS = 3000L

    @Volatile
    private var deviceService: UDeviceService? = null

    @Volatile
    var isServiceBound: Boolean = false
        private set

    @Volatile
    var isRegistered: Boolean = false
        private set

    private var retryCount = 0
    private lateinit var appContext: Context

    /** Binder token for USDK registration — must be kept alive to prevent GC deregistration. */
    private val binderToken = Binder()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val svc = UDeviceService.Stub.asInterface(service)
            deviceService = svc
            isServiceBound = true
            retryCount = 0
            Timber.d("USDK: Service connected")

            enableDebugLog(svc)

            DeviceServiceLimited.bind(
                appContext,
                svc,
                object : DeviceServiceLimited.ServiceBindListener {
                    override fun onSuccess() {
                        Timber.d("USDK: DeviceServiceLimited bind success")
                        register(svc)
                    }

                    override fun onFail() {
                        Timber.e("USDK: DeviceServiceLimited bind failed — registering anyway")
                        register(svc)
                    }
                }
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            deviceService = null
            isServiceBound = false
            Timber.w("USDK: Service disconnected")
        }
    }

    /**
     * Binds to the USDK service with retry logic (up to [MAX_RETRIES] attempts, [RETRY_INTERVAL_MS] apart).
     */
    fun bind(context: Context) {
        if (isServiceBound) {
            Timber.d("USDK: Service already bound")
            return
        }
        appContext = context.applicationContext
        retryCount = 0
        attemptBind(context)
    }

    private fun attemptBind(context: Context) {
        val intent = Intent(USDK_SERVICE_ACTION).apply {
            setPackage(USDK_SERVICE_PACKAGE)
        }
        try {
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                retryCount++
                if (retryCount <= MAX_RETRIES) {
                    Timber.w("USDK: Bind failed, retrying (%d/%d) in %dms", retryCount, MAX_RETRIES, RETRY_INTERVAL_MS)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!isServiceBound) attemptBind(context)
                    }, RETRY_INTERVAL_MS)
                } else {
                    Timber.e("USDK: Failed to bind after %d retries — USDK service not available", MAX_RETRIES)
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "USDK: SecurityException binding to service")
        }
    }

    /**
     * Unbinds from the USDK service.
     */
    fun unbind(context: Context) {
        if (!isServiceBound) return
        if (isRegistered) {
            try {
                deviceService?.unregister(null)
                isRegistered = false
                Timber.d("USDK: Unregistered from device service")
            } catch (e: Exception) {
                Timber.w(e, "USDK: Error unregistering from device service")
            }
        }
        try {
            DeviceServiceLimited.unbind(context)
        } catch (e: Exception) {
            Timber.w(e, "USDK: Error unbinding DeviceServiceLimited")
        }
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            Timber.w(e, "USDK: Error unbinding service")
        }
        deviceService = null
        isServiceBound = false
        Timber.d("USDK: Service unbound")
    }

    private fun enableDebugLog(svc: UDeviceService) {
        try {
            val logOption = Bundle().apply {
                putBoolean(DeviceServiceData.COMMON_LOG, true)
                putBoolean(DeviceServiceData.MASTERCONTROL_LOG, true)
            }
            svc.debugLog(logOption)
            Timber.d("USDK: Debug logging enabled")
        } catch (e: Exception) {
            Timber.w(e, "USDK: Failed to enable debug logging")
        }
    }

    private fun register(svc: UDeviceService) {
        try {
            val param = Bundle().apply {
                putBoolean(DeviceServiceData.USE_EPAY_MODULE, true)
            }
            svc.register(param, binderToken)
            isRegistered = true
            Timber.d("USDK: Registered with device service")
        } catch (e: Exception) {
            isRegistered = false
            Timber.e(e, "USDK: Failed to register — %s", e.message)
        }
    }

    /**
     * Returns the USDK RF reader for the inner NFC antenna, or null if the service is not bound.
     */
    fun getRFReader(): URFReader? {
        val service = deviceService ?: run {
            Timber.w("USDK: Service not bound — cannot get RF reader")
            return null
        }
        return try {
            val bundle = Bundle().apply { putString("rfDeviceName", RFDeviceName.INNER) }
            URFReader.Stub.asInterface(service.getRFReader(bundle))
        } catch (e: Exception) {
            Timber.e(e, "USDK: Failed to get RF reader")
            null
        }
    }

    /**
     * Returns the USDK vector printer, or null if not available.
     */
    fun getVectorPrinter(): UVectorPrinter? {
        val service = deviceService ?: run {
            Timber.w("USDK: Service not bound — cannot get vector printer")
            return null
        }
        return try {
            UVectorPrinter.Stub.asInterface(service.vectorPrinter)
        } catch (e: Exception) {
            Timber.e(e, "USDK: Failed to get vector printer")
            null
        }
    }

    /**
     * Returns the USDK NFC tag emulation module, or null if not available.
     */
    fun getNFCTag(): UNFCTag? {
        val service = deviceService ?: run {
            Timber.w("USDK: Service not bound — cannot get NFC tag module")
            return null
        }
        return try {
            UNFCTag.Stub.asInterface(
                service.getModule(ModuleName.NFC_TAG_EMULATION, null)
            )
        } catch (e: Exception) {
            Timber.e(e, "USDK: Failed to get NFC tag emulation module")
            null
        }
    }
}
