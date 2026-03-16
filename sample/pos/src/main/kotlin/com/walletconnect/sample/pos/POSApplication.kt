package com.walletconnect.sample.pos

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.walletconnect.pos.PosClient
import timber.log.Timber

class POSApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        var initError: String? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (if available)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize the lightweight POS SDK
        val deviceId = "sample_pos_device_${Build.MODEL}_${Build.SERIAL}"

        try {
            PosClient.init(
                apiKey = BuildConfig.MERCHANT_API_KEY,
                merchantId = BuildConfig.MERCHANT_ID,
                deviceId = deviceId,
            )
            PosClient.setDelegate(PosSampleDelegate)
            Timber.d("POSClient initialized successfully")
        } catch (e: IllegalStateException) {
            initError = e.message
            Timber.e(e, "POSClient initialization failed")
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
