package com.reown.android.test.utils

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reown.android.BuildConfig
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.CoreProtocol
import com.reown.android.di.overrideModule
import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.model.type.JsonRpcInteractorInterface
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.keyserver.domain.IdentitiesInteractor
import com.reown.android.relay.ConnectionType
import com.reown.android.relay.RelayClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import timber.log.Timber

internal object TestClient {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    fun KoinApplication.Companion.createNewWCKoinApp(): KoinApplication = KoinApplication.init().apply { createEagerInstances() }

    object Primary {

        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Primary",
            description = "Primary client for automation tests",
            url = "kotlin.e2e.primary",
            icons = listOf(),
            redirect = null
        )

        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()

        private val coreProtocol = CoreClient.apply {
            Timber.d("Primary CP start: ")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL, onError = ::globalOnError)
            Relay.connect(::globalOnError)
            _isInitialized.tryEmit(true)
            Timber.d("Primary CP finish: ")
        }


        internal val Relay get() = coreProtocol.Relay
        internal val jsonRpcInteractor: RelayJsonRpcInteractorInterface by lazy { wcKoinApp.koin.get() }
        internal val keyManagementRepository: KeyManagementRepository by lazy { wcKoinApp.koin.get() }
        internal val identitiesInteractor: IdentitiesInteractor by lazy { wcKoinApp.koin.get() }
        internal val keyserverUrl: String by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.KEYSERVER_URL)) }
    }


    object Secondary {

        private val metadata = Core.Model.AppMetaData(
            name = "Kotlin E2E Secondary",
            description = "Secondary client for automation tests",
            url = "kotlin.e2e.secondary",
            icons = listOf(),
            redirect = null
        )

        private val secondaryKoinApp = KoinApplication.createNewWCKoinApp()

        private var _isInitialized = MutableStateFlow(false)
        internal var isInitialized = _isInitialized.asStateFlow()

        private val coreProtocol = CoreProtocol(secondaryKoinApp).apply {
            Timber.d("Secondary CP start: ")
            initialize(app, BuildConfig.PROJECT_ID, metadata, ConnectionType.MANUAL) { Timber.e(it.throwable) }

            // Override of previous Relay necessary for reinitialization of `eventsFlow`
            Relay = RelayClient(secondaryKoinApp)

            // Override of storage instances and depending objects
            secondaryKoinApp.modules(overrideModule(Relay, Pairing, PairingController, "test_secondary", app.packageName))

            // Necessary reinit of Relay, Pairing and PairingController
            Relay.initialize(ConnectionType.MANUAL) { Timber.e(it) }
            Pairing.initialize()
            PairingController.initialize()

            Relay.connect(::globalOnError)
            _isInitialized.tryEmit(true)
            Timber.d("Secondary CP finish: ")
        }

        internal val Relay get() = coreProtocol.Relay
        internal val jsonRpcInteractor: RelayJsonRpcInteractorInterface by lazy { secondaryKoinApp.koin.get() }
        internal val keyManagementRepository: KeyManagementRepository by lazy { secondaryKoinApp.koin.get() }

    }
}
