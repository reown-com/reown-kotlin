package com.reown.sample.pos

import android.app.Application
import com.reown.pos.client.POS
import com.reown.pos.client.POSClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class POSApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            withContext(Dispatchers.IO) {
                val projectId = BuildConfig.PROJECT_ID

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
                    application = this@POSApplication,
                )

                // Initialize POSClient
                POSClient.initialize(
                    initParams = initParams,
                    onSuccess = {
                        Timber.d("POSClient initialized successfully")
                    },
                    onError = { error ->
                        Timber.e("POSClient initialization failed: ${error.throwable}")
                    }
                )

                POSClient.setDelegate(PosSampleDelegate)
            }
        }
    }
}
