package com.walletconnect.pay.test.utils

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.walletconnect.pay.BuildConfig
import com.walletconnect.pay.Pay
import com.walletconnect.pay.WalletConnectPay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object TestClient {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    var initializationError: String? = null
        private set

    lateinit var posApi: PosTestApi
        private set

    lateinit var signer: TestWalletSigner
        private set

    // For Android instrumented tests, we need to pass the private key via instrumentation args
    // or use a hardcoded test key. Environment variables don't work on Android.
    // This should be set via: adb shell am instrument -e TEST_WALLET_PRIVATE_KEY "your_key" ...
    // Or passed via gradle: -Pandroid.testInstrumentationRunnerArguments.TEST_WALLET_PRIVATE_KEY=your_key
    private fun getPrivateKey(): String? {
        // Try instrumentation arguments first (works on Android)
        val bundle = androidx.test.platform.app.InstrumentationRegistry.getArguments()
        val fromArgs = bundle.getString("TEST_WALLET_PRIVATE_KEY")
        if (!fromArgs.isNullOrBlank()) {
            println("Got private key from instrumentation arguments")
            return fromArgs
        }

        // Try system property (might work in some cases)
        val fromProp = System.getProperty("TEST_WALLET_PRIVATE_KEY")
        if (!fromProp.isNullOrBlank()) {
            println("Got private key from system property")
            return fromProp
        }

        // Try environment variable (usually doesn't work on Android but try anyway)
        val fromEnv = System.getenv("TEST_WALLET_PRIVATE_KEY")
        if (!fromEnv.isNullOrBlank()) {
            println("Got private key from environment variable")
            return fromEnv
        }

        return null
    }

    init {
        initialize()
    }

    private fun initialize() {
        val errors = mutableListOf<String>()

        // Initialize WalletConnectPay SDK
        try {
            if (!WalletConnectPay.isInitialized) {
                WalletConnectPay.initialize(
                    Pay.SdkConfig(
                        apiKey = Common.API_KEY,
                        projectId = BuildConfig.PROJECT_ID,
                        packageName = app.packageName
                    )
                )
            }
            println("WalletConnectPay SDK initialized successfully")
        } catch (e: Exception) {
            val error = "Failed to initialize WalletConnectPay SDK: ${e.message}"
            println(error)
            e.printStackTrace()
            errors.add(error)
        }

        // Initialize POS API client
        try {
            posApi = createPosApi()
            println("POS API client initialized successfully")
        } catch (e: Exception) {
            val error = "Failed to initialize POS API client: ${e.message}"
            println(error)
            e.printStackTrace()
            errors.add(error)
        }

        // Initialize signer
        try {
            val privateKey = getPrivateKey()
            if (privateKey == null) {
                throw IllegalStateException(
                    "TEST_WALLET_PRIVATE_KEY not set. Pass it via gradle:\n" +
                    "./gradlew :product:pay:connectedAndroidTest " +
                    "-Pandroid.testInstrumentationRunnerArguments.TEST_WALLET_PRIVATE_KEY=your_private_key"
                )
            }

            signer = TestWalletSigner(privateKey)

            // Verify address matches expected
            if (!signer.address.equals(Common.TEST_ADDRESS, ignoreCase = true)) {
                throw IllegalStateException(
                    "Signer address ${signer.address} does not match expected ${Common.TEST_ADDRESS}"
                )
            }
            println("TestWalletSigner initialized with address: ${signer.address}")
        } catch (e: Exception) {
            val error = "Failed to initialize TestWalletSigner: ${e.message}"
            println(error)
            e.printStackTrace()
            errors.add(error)
        }

        if (errors.isEmpty()) {
            _isInitialized.tryEmit(true)
            println("All test clients initialized successfully")
        } else {
            initializationError = errors.joinToString("\n")
            println("Initialization failed with errors:\n$initializationError")
        }
    }

    fun shutdown() {
        if (WalletConnectPay.isInitialized) {
            WalletConnectPay.shutdown()
        }
        _isInitialized.tryEmit(false)
        println("WalletConnectPay SDK shutdown")
    }
}
