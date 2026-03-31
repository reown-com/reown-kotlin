package com.walletconnect.sample.pos

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.walletconnect.pos.PosClient
import com.walletconnect.sample.pos.credentials.MerchantCredentialsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executors

class POSApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        var initError: String? = null
            private set

        private val _initCompleted = MutableStateFlow(false)
        val initCompleted = _initCompleted.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (if available)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize the POS SDK on a background thread to avoid blocking the main thread.
        // OkHttp, Retrofit, and Moshi class loading is expensive on weak POS hardware.
        val credentialsManager = MerchantCredentialsManager(this)
        val deviceId = "sample_pos_device_${Build.MODEL}_${Build.SERIAL}"
        Executors.newSingleThreadExecutor().execute {
            try {
                PosClient.init(
                    apiKey = credentialsManager.getApiKey(),
                    merchantId = credentialsManager.getMerchantId(),
                    deviceId = deviceId,
                )
                PosClient.setDelegate(PosSampleDelegate)
                Timber.d("POSClient initialized successfully")
            } catch (e: IllegalStateException) {
                initError = e.message ?: "Unknown initialization error"
                Timber.e(e, "POSClient initialization failed")
            } finally {
                _initCompleted.value = true
            }
        }
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
