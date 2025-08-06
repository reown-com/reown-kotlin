package com.reown.android

import android.app.Application
import android.content.SharedPreferences
import com.reown.android.di.coreStorageModule
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.di.KEY_CLIENT_ID
import com.reown.android.internal.common.di.appKitModule
import com.reown.android.internal.common.di.coreAndroidNetworkModule
import com.reown.android.internal.common.di.coreCommonModule
import com.reown.android.internal.common.di.coreCryptoModule
import com.reown.android.internal.common.di.coreJsonRpcModule
import com.reown.android.internal.common.di.corePairingModule
import com.reown.android.internal.common.di.explorerModule
import com.reown.android.internal.common.di.keyServerModule
import com.reown.android.internal.common.di.pulseModule
import com.reown.android.internal.common.di.pushModule
import com.reown.android.internal.common.explorer.ExplorerInterface
import com.reown.android.internal.common.explorer.ExplorerProtocol
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.model.Redirect
import com.reown.android.internal.common.model.TelemetryEnabled
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.pairing.client.PairingInterface
import com.reown.android.pairing.client.PairingProtocol
import com.reown.android.pairing.handler.PairingController
import com.reown.android.pairing.handler.PairingControllerInterface
import com.reown.android.push.PushInterface
import com.reown.android.push.client.PushClient
import com.reown.android.relay.ConnectionType
import com.reown.android.relay.NetworkClientTimeout
import com.reown.android.relay.RelayClient
import com.reown.android.relay.RelayConnectionInterface
import com.reown.android.utils.isValidRelayServerUrl
import com.reown.android.utils.plantTimber
import com.reown.android.utils.projectId
import com.reown.android.verify.client.VerifyClient
import com.reown.android.verify.client.VerifyInterface
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module
import uniffi.yttrium.Logger
import uniffi.yttrium.SignClient
import uniffi.yttrium.registerLogger

class AndroidLogger: Logger {
    override fun log(message: String) {
        println("jordan: Message from Rust: $message")
    }
}

class CoreProtocol(private val koinApp: KoinApplication = wcKoinApp) : CoreInterface {
    override val Pairing: PairingInterface = PairingProtocol(koinApp)
    override val PairingController: PairingControllerInterface = PairingController(koinApp)
    override var Relay = RelayClient(koinApp)

    @Deprecated(message = "Replaced with Push")
    override val Echo: PushInterface = PushClient
    override val Push: PushInterface = PushClient
    override val Verify: VerifyInterface = VerifyClient(koinApp)
    override val Explorer: ExplorerInterface = ExplorerProtocol(koinApp)

    lateinit var signClient: SignClient

    init {
        plantTimber()
    }

    override fun setDelegate(delegate: CoreInterface.Delegate) {
        Pairing.setDelegate(delegate)
    }

    companion object {
        val instance = CoreProtocol()
    }

    override fun initialize(
        metaData: Core.Model.AppMetaData,
        relayServerUrl: String,
        connectionType: ConnectionType,
        application: Application,
        relay: RelayConnectionInterface?,
        keyServerUrl: String?,
        networkClientTimeout: NetworkClientTimeout?,
        telemetryEnabled: Boolean,
        onError: (Core.Model.Error) -> Unit
    ) {
        try {
            require(relayServerUrl.isValidRelayServerUrl()) { "Check the schema and projectId parameter of the Server Url" }
            //TODO: Init Sign rust client

            setup(
                application = application,
                serverUrl = relayServerUrl,
                projectId = relayServerUrl.projectId(),
                telemetryEnabled = telemetryEnabled,
                connectionType = connectionType,
                networkClientTimeout = networkClientTimeout,
                relay = relay,
                onError = onError,
                metaData = metaData,
                keyServerUrl = keyServerUrl,
                signClient = signClient
            )
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    override fun initialize(
        application: Application,
        projectId: String,
        metaData: Core.Model.AppMetaData,
        connectionType: ConnectionType,
        relay: RelayConnectionInterface?,
        keyServerUrl: String?,
        networkClientTimeout: NetworkClientTimeout?,
        telemetryEnabled: Boolean,
        onError: (Core.Model.Error) -> Unit
    ) {
        try {
            require(projectId.isNotEmpty()) { "Project Id cannot be empty" }
            registerLogger(AndroidLogger())
            signClient = SignClient(projectId = projectId)
            setup(
                application = application,
                projectId = projectId,
                telemetryEnabled = telemetryEnabled,
                connectionType = connectionType,
                networkClientTimeout = networkClientTimeout,
                relay = relay,
                onError = onError,
                metaData = metaData,
                keyServerUrl = keyServerUrl,
                signClient = signClient
            )
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    private fun CoreProtocol.setup(
        application: Application,
        serverUrl: String? = null,
        projectId: String,
        telemetryEnabled: Boolean,
        connectionType: ConnectionType,
        networkClientTimeout: NetworkClientTimeout?,
        relay: RelayConnectionInterface?,
        onError: (Core.Model.Error) -> Unit,
        metaData: Core.Model.AppMetaData,
        keyServerUrl: String?,
        signClient: SignClient
    ) {
        val packageName: String = application.packageName
        val relayServerUrl = if (serverUrl.isNullOrEmpty()) "wss://relay.walletconnect.org?projectId=$projectId" else serverUrl

        with(koinApp) {
            androidContext(application)
            modules(
                module { single(named(AndroidCommonDITags.PACKAGE_NAME)) { packageName } },
                module { single { ProjectId(projectId) } },
                module { single(named(AndroidCommonDITags.SIGN_RUST_CLIENT)) { signClient } },
                module { single(named(AndroidCommonDITags.TELEMETRY_ENABLED)) { TelemetryEnabled(telemetryEnabled) } },
                coreAndroidNetworkModule(relayServerUrl, connectionType, BuildConfig.SDK_VERSION, networkClientTimeout, packageName),
                coreCommonModule(),
                coreCryptoModule(),
            )

            if (relay == null) {
                Relay.initialize(connectionType) { error -> onError(Core.Model.Error(error)) }
            }

            modules(
                coreStorageModule(packageName = packageName),
                module { single(named(AndroidCommonDITags.CLIENT_ID)) { requireNotNull(get<SharedPreferences>().getString(KEY_CLIENT_ID, null)) } },
                pushModule(),
                module { single { relay ?: Relay } },
                module {
                    single {
                        with(metaData) {
                            AppMetaData(
                                name = name,
                                description = description,
                                url = url,
                                icons = icons,
                                redirect = Redirect(native = redirect, universal = appLink, linkMode = linkMode)
                            )
                        }
                    }
                },
                module { single { Echo } },
                module { single { Push } },
                module { single { Verify } },
                coreJsonRpcModule(),
                corePairingModule(Pairing, PairingController),
                keyServerModule(keyServerUrl),
                explorerModule(),
                appKitModule(),
                pulseModule(packageName)
            )
        }

        Pairing.initialize()
        PairingController.initialize()
        Verify.initialize()
    }
}