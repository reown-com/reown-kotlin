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

    // For Android instrumented tests, we need to pass secrets via instrumentation args.
    // Environment variables don't work on Android.
    // Pass via gradle: -Pandroid.testInstrumentationRunnerArguments.KEY=value
    // Or the build.gradle.kts will automatically pass env vars as instrumentation args.
    private fun getInstrumentationArg(key: String): String? {
        val bundle = androidx.test.platform.app.InstrumentationRegistry.getArguments()
        return bundle.getString(key)?.takeIf { it.isNotBlank() }
    }

    private fun getPrivateKey(): String? {
        // Try instrumentation arguments first (works on Android)
        getInstrumentationArg("TEST_WALLET_PRIVATE_KEY")?.let {
            println("Got private key from instrumentation arguments")
            return it
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

    private fun getProjectId(): String {
        // Try instrumentation arguments first (preferred for CI)
        getInstrumentationArg("WC_CLOUD_PROJECT_ID")?.let {
            println("Got project ID from instrumentation arguments")
            return it
        }

        // Fallback to BuildConfig (set at build time)
        if (BuildConfig.PROJECT_ID.isNotBlank()) {
            println("Got project ID from BuildConfig")
            return BuildConfig.PROJECT_ID
        }

        throw IllegalStateException(
            "WC_CLOUD_PROJECT_ID not set. Ensure it's available as an environment variable during build or test."
        )
    }

    init {
        initialize()
    }

    private fun initialize() {
        val errors = mutableListOf<String>()

        // Initialize WalletConnectPay SDK
        try {
            if (!WalletConnectPay.isInitialized) {
                val projectId = getProjectId()
                println("=== WalletConnectPay SDK Initialization ===")
                println("Project ID: ${projectId.take(8)}... (length: ${projectId.length})")
                println("Package name: ${app.packageName}")
                println("BuildConfig.PROJECT_ID: '${BuildConfig.PROJECT_ID}' (length: ${BuildConfig.PROJECT_ID.length})")
                println("From instrumentation args: ${getInstrumentationArg("WC_CLOUD_PROJECT_ID")?.take(8) ?: "null"}")
                WalletConnectPay.initialize(
                    Pay.SdkConfig(
                        appId = projectId,
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
