package com.walletconnect.sample.modal

import android.app.Application
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.walletconnect.sample.common.tag
import com.walletconnect.util.bytesToHex
import com.walletconnect.util.randomBytes
import com.walletconnect.web3.modal.client.Modal
import com.walletconnect.web3.modal.client.AppKit
import com.walletconnect.web3.modal.presets.AppKitChainsPresets
import com.walletconnect.web3.modal.utils.EthUtils
import timber.log.Timber
import com.walletconnect.sample.common.BuildConfig as CommonBuildConfig

class AppKitApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val appMetaData = Core.Model.AppMetaData(
            name = "reown Modals",
            description = "Kotlin AppKit Lab Sample",
            url = "https://web3modal-laboratory-git-chore-kotlin-assetlinks-walletconnect1.vercel.app",
            icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
            redirect = "kotlin-modal-wc://request",
            linkMode = true,
            appLink = BuildConfig.LAB_APP_LINK
        )

        CoreClient.initialize(
            projectId = CommonBuildConfig.PROJECT_ID,
            connectionType = ConnectionType.AUTOMATIC,
            application = this,
            metaData = appMetaData,
        ) {
            Timber.e(it.throwable)
        }

        AppKit.initialize(Modal.Params.Init(core = CoreClient)) { error ->
            Timber.e(tag(this), error.throwable.stackTraceToString())
            Firebase.crashlytics.recordException(error.throwable)
        }

        AppKit.setChains(AppKitChainsPresets.ethChains.values.toList())

        val authParams = Modal.Model.AuthPayloadParams(
            chains = AppKitChainsPresets.ethChains.values.toList().map { it.id },
            domain = "sample.kotlin.modal",
            uri = "https://web3inbox.com/all-apps",
            nonce = randomBytes(12).bytesToHex(),
            statement = "I accept the Terms of Service: https://yourDappDomain.com/",
            methods = EthUtils.ethMethods
        )
        AppKit.setAuthRequestParams(authParams)

        FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
    }
}
