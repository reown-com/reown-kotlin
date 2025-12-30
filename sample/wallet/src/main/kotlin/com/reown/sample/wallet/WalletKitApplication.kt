package com.reown.sample.wallet

import android.app.Application
import android.os.Build
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.pandulapeter.beagle.Beagle
import com.pandulapeter.beagle.common.configuration.Behavior
import com.pandulapeter.beagle.log.BeagleLogger
import com.pandulapeter.beagle.logOkHttp.BeagleOkHttpLogger
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.relay.ConnectionType
import com.reown.foundation.util.Logger
import com.reown.notify.client.Notify
import com.reown.notify.client.NotifyClient
import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SmartAccountEnabler
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.client.Stacks
import com.reown.sample.wallet.domain.client.TONClient
//import com.reown.sample.wallet.domain.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.domain.mixPanel
import com.reown.sample.wallet.domain.notify.NotificationHandler
import com.reown.sample.wallet.domain.notify.NotifyDelegate
import com.reown.sample.wallet.ui.state.ConnectionState
import com.reown.sample.wallet.ui.state.connectionStateFlow
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.reown.sample.wallet.domain.client.SuiUtils
import com.walletconnect.pay.Pay
import com.walletconnect.pay.WalletConnectPay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named
import timber.log.Timber
import com.reown.sample.common.BuildConfig as CommonBuildConfig

class WalletKitApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val projectId = BuildConfig.PROJECT_ID

        EthAccountDelegate.application = this
        TONAccountDelegate.application = this
        SolanaAccountDelegate.application = this
        StacksAccountDelegate.application = this
        SuiAccountDelegate.application = this
        SuiUtils.init(projectId, this.packageName, applicationContext)

        Stacks.init(BuildConfig.PROJECT_ID, applicationContext.packageName)
        TONClient.init(this.packageName)

        SmartAccountEnabler.init(this)

        // Initialize WalletConnectPay
        WalletConnectPay.initialize(
            Pay.SdkConfig(
                baseUrl = "https://api.pay.walletconnect.com",
                apiKey = BuildConfig.MERCHANT_API_KEY,
                sdkName = "kotlin-walletconnect-pay",
                sdkVersion = "1.0.0",
                sdkPlatform = "android"
            )
        )

        val appMetaData = Core.Model.AppMetaData(
            name = "Kotlin Wallet",
            description = "Kotlin Wallet Implementation",
            url = "https://appkit-lab.reown.com",
            icons = listOf("https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png"),
            redirect = "kotlin-web3wallet://request",
            appLink = BuildConfig.WALLET_APP_LINK,
            linkMode = true
        )

        CoreClient.initialize(
            application = this,
            projectId = projectId,
            metaData = appMetaData,
            connectionType = ConnectionType.AUTOMATIC,
            onError = { error ->
                Firebase.crashlytics.recordException(error.throwable)
                println("Init error: ${error.throwable.stackTraceToString()}")
                scope.launch {
                    connectionStateFlow.emit(ConnectionState.Error(error.throwable.message ?: ""))
                }
            }
        )

        println("Account: ${EthAccountDelegate.address}")

        WalletKit.initialize(
            Wallet.Params.Init(core = CoreClient),
            onSuccess = { println("Web3Wallet initialized") },
            onError = { error ->
                Firebase.crashlytics.recordException(error.throwable)
                println(error.throwable.stackTraceToString())
            })

        FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
        NotifyClient.initialize(
            init = Notify.Params.Init(CoreClient)
        ) { error ->
            Firebase.crashlytics.recordException(error.throwable)
            println(error.throwable.stackTraceToString())
        }

        mixPanel = MixpanelAPI.getInstance(this, CommonBuildConfig.MIX_PANEL, true).apply {
            identify(CoreClient.Push.clientId)
            people.set("\$name", EthAccountDelegate.ethAccount)
        }

        initializeBeagle()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            WalletKit.registerDeviceToken(
                firebaseAccessToken = token, enableEncrypted = true,
                onSuccess = {
                    println("Successfully registered firebase token for Web3Wallet: $token")
                },
                onError = {
                    println("Error while registering firebase token for Web3Wallet: ${it.throwable}")
                }
            )
        }

        scope.launch {
            supervisorScope {
                wcKoinApp.koin.get<Timber.Forest>().plant(object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        if (t != null) {
                            mixPanel.track("error: $t, message: $message")
                        } else {
                            mixPanel.track(message)
                        }
                    }
                })

                handleNotifyMessages()
            }
        }
    }

    private fun initializeBeagle() {
        Timber.plant(
            object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    Beagle.log("${tag?.let { "[$it] " } ?: ""}$message", "Timber", t?.stackTraceToString(), timestamp = System.currentTimeMillis())
                }
            }
        )

        Beagle.initialize(
            application = this,
            behavior = Behavior(
                logBehavior = Behavior.LogBehavior(loggers = listOf(BeagleLogger)),
                networkLogBehavior = Behavior.NetworkLogBehavior(networkLoggers = listOf(BeagleOkHttpLogger))
            )
        )
    }


    private fun handleNotifyMessages() {
        val scope = CoroutineScope(Dispatchers.Default)

        val notifyEventsJob = NotifyDelegate.notifyEvents
            .filterIsInstance<Notify.Event.Notification>()
            .onEach { notification -> NotificationHandler.addNotification(notification.notification) }
            .launchIn(scope)


        val notificationDisplayingJob = NotificationHandler.startNotificationDisplayingJob(scope, this)


        notifyEventsJob.invokeOnCompletion { cause ->
            onScopeCancelled(cause, "notifyEventsJob")
        }

        notificationDisplayingJob.invokeOnCompletion { cause ->
            onScopeCancelled(cause, "notificationDisplayingJob")
        }
    }

    private fun onScopeCancelled(error: Throwable?, job: String) {
        wcKoinApp.koin.get<Logger>(named(AndroidCommonDITags.LOGGER)).error("onScopeCancelled($job): $error")
    }
}