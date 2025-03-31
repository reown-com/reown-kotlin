package com.reown.sign.test.utils

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.CoreProtocol
import com.reown.android.relay.ConnectionType
import com.reown.android.relay.RelayClient
import com.reown.sign.BuildConfig
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.reown.sign.client.SignProtocol
import com.reown.sign.di.overrideModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.KoinApplication
import timber.log.Timber

internal object TestClient {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    fun KoinApplication.Companion.createNewWCKoinApp(): KoinApplication = init().apply { createEagerInstances() }

    object Wallet {

        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Wallet",
            description = "Wallet for automation tests",
            url = "kotlin.e2e.wallet",
            icons = listOf(),
            redirect = null
        )

        private val coreProtocol = CoreClient.apply {
            Timber.d("kobe: Wallet CP start: ${BuildConfig.PROJECT_ID}")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL, onError = ::globalOnError)
            Relay.connect(::globalOnError)
        }

        private val initParams = Sign.Params.Init(coreProtocol)
        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()
        internal val signClient = SignClient.apply {
            initialize(initParams, onSuccess = { _isInitialized.tryEmit(true) }, onError = { Timber.e(it.throwable) })
            Timber.d("Wallet CP finish: ")
        }

        internal val Relay get() = coreProtocol.Relay
        internal val Pairing = coreProtocol.Pairing
    }

    object WalletLinkMode {

        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Wallet",
            description = "Wallet for automation tests",
            url = "kotlin.e2e.wallet",
            icons = listOf(),
            appLink = "https://web3modal-laboratory-git-chore-kotlin-assetlinks-walletconnect1.vercel.app/wallet",
            linkMode = true,
            redirect = null
        )

        private val coreProtocol = CoreClient.apply {
            Timber.d("Wallet CP start: ")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL, onError = ::globalOnError)
            Relay.connect(::globalOnError)
        }

        private val initParams = Sign.Params.Init(coreProtocol)
        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()
        internal val signClient = SignClient.apply {
            initialize(initParams, onSuccess = { _isInitialized.tryEmit(true) }, onError = { Timber.e(it.throwable) })
            Timber.d("Wallet CP finish: ")
        }

        internal val Relay get() = coreProtocol.Relay
        internal val Pairing = coreProtocol.Pairing
    }

    object Dapp {

        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Dapp",
            description = "Dapp for automation tests",
            url = "kotlin.e2e.dapp",
            icons = listOf(),
            redirect = null
        )

        private val dappKoinApp = KoinApplication.createNewWCKoinApp()

        private val coreProtocol = CoreProtocol(dappKoinApp).apply {
            Timber.d("Dapp CP start: ")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL) { Timber.e(it.throwable) }

            // Override of previous Relay necessary for reinitialization of `eventsFlow`
            Relay = RelayClient(dappKoinApp)

            // Override of storage instances and depending objects
            dappKoinApp.modules(
                overrideModule(
                    Relay,
                    Pairing,
                    PairingController,
                    "test_dapp",
                    "wss://relay.walletconnect.org?projectId=${BuildConfig.PROJECT_ID}",
                    ConnectionType.MANUAL,
                    app.packageName
                )
            )

            // Necessary reinit of Relay, Pairing and PairingController
            Relay.initialize(ConnectionType.MANUAL) { Timber.e(it) }
            Pairing.initialize()
            PairingController.initialize()

            Relay.connect(::globalOnError)
        }

        private val initParams = Sign.Params.Init(coreProtocol)
        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()
        internal val signClient = SignProtocol(dappKoinApp).apply {
            initialize(initParams, onSuccess = { _isInitialized.tryEmit(true) }, onError = { Timber.e(it.throwable) })
            Timber.d("Dapp CP finish: ")
        }

        internal val Relay get() = coreProtocol.Relay
        internal val Pairing = coreProtocol.Pairing
    }

    object DappLinkMode {

        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Dapp",
            description = "Dapp for automation tests",
            url = "kotlin.e2e.dapp",
            icons = listOf(),
            linkMode = true,
            appLink = "https://web3modal-laboratory-git-chore-kotlin-assetlinks-walletconnect1.vercel.app/dapp",
            redirect = null
        )

        private val dappKoinApp = KoinApplication.createNewWCKoinApp()

        private val coreProtocol = CoreProtocol(dappKoinApp).apply {
            Timber.d("Dapp CP start: ")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL) { Timber.e(it.throwable) }

            // Override of previous Relay necessary for reinitialization of `eventsFlow`
            Relay = RelayClient(dappKoinApp)

            // Override of storage instances and depending objects
            dappKoinApp.modules(
                overrideModule(
                    Relay,
                    Pairing,
                    PairingController,
                    "test_hybrid",
                    "wss://relay.walletconnect.org?projectId=${BuildConfig.PROJECT_ID}",
                    ConnectionType.MANUAL,
                    app.packageName
                )
            )

            // Necessary reinit of Relay, Pairing and PairingController
            Relay.initialize(ConnectionType.MANUAL) { Timber.e(it) }
            Pairing.initialize()
            PairingController.initialize()

            Relay.connect(::globalOnError)
        }

        private val initParams = Sign.Params.Init(coreProtocol)
        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()
        internal val signClientLinkMode = SignProtocol(dappKoinApp).apply {
            initialize(initParams, onSuccess = { _isInitialized.tryEmit(true) }, onError = { Timber.e(it.throwable) })
            Timber.d("Dapp CP finish: ")
        }

        internal val Relay get() = coreProtocol.Relay
        internal val Pairing = coreProtocol.Pairing
    }

    object Hybrid {
        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Hybrid App",
            description = "Hybrid App for automation tests",
            url = "kotlin.e2e.hybrid",
            icons = listOf(),
            redirect = null
        )

        private val hybridKoinApp = KoinApplication.createNewWCKoinApp()

        private val coreProtocol = CoreProtocol(hybridKoinApp).apply {
            Timber.d("Hybrid CP start: ")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL) { Timber.e(it.throwable) }

            // Override of previous Relay necessary for reinitialization of `eventsFlow`
            Relay = RelayClient(hybridKoinApp)

            // Override of storage instances and depending objects
            hybridKoinApp.modules(
                overrideModule(
                    Relay,
                    Pairing,
                    PairingController,
                    "test_hybrid",
                    "wss://relay.walletconnect.org?projectId=${BuildConfig.PROJECT_ID}",
                    ConnectionType.MANUAL,
                    app.packageName
                )
            )

            // Necessary reinit of Relay, Pairing and PairingController
            Relay.initialize(ConnectionType.MANUAL) { Timber.e(it) }
            Pairing.initialize()
            PairingController.initialize()

            Relay.connect(::globalOnError)
        }

        private val initParams = Sign.Params.Init(coreProtocol)
        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()

        internal val signClient = SignProtocol(hybridKoinApp)
            .apply {
                initialize(initParams, onSuccess = { _isInitialized.tryEmit(true) }, onError = { Timber.e(it.throwable) })
                initialize(initParams, onSuccess = { println("Second Init") }, onError = { Timber.e("Second Init Error: ${it.throwable}") })
                Timber.d("Hybrid CP finish: ")
            }

        internal val Relay get() = coreProtocol.Relay
        internal val Pairing = coreProtocol.Pairing
    }
}