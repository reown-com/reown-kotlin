package com.reown.sample.pos

import android.app.Application
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.pos.client.POS
import com.reown.pos.client.POSClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

class POSApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val projectId = BuildConfig.PROJECT_ID

        println("kobe: project id: $projectId")

        val metaData = POS.Model.MetaData(
            merchantName = "Sample POS App",
            description = "Sample Point of Sale application for testing POSClient",
            url = "https://appkit-lab.reown.com",
            icons = listOf("https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png")
        )

        val initParams = POS.Params.Init(
            projectId = projectId,
            deviceId = "sample_pos_device_${System.currentTimeMillis()}",
            metaData = metaData,
            application = this
        )

        // Initialize POSClient
        POSClient.initialize(
            init = initParams,
            onSuccess = {
                Timber.d("kobe: POSClient initialized successfully")
            },
            onError = { error ->
                Firebase.crashlytics.recordException(error.throwable)
                Timber.e("kobe: POSClient initialization failed: ${error.throwable}")
            }
        )

        POSClient.setChains(listOf("eip155:1"))
    }
}
