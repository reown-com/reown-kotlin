package com.reown.sample.dapp

import android.app.Application
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sample.common.tag
import com.reown.util.bytesToHex
import com.reown.util.randomBytes
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import com.reown.appkit.utils.EthUtils
import timber.log.Timber
import com.reown.sample.common.BuildConfig as CommonBuildConfig

class DappSampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val appMetaData = Core.Model.AppMetaData(
            name = "Kotlin Dapp",
            description = "Kotlin Dapp Implementation",
            url = "https://dev.lab.web3modal.com",
            icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
            redirect = "kotlin-dapp-wc://request",
            appLink = BuildConfig.DAPP_APP_LINK,
            linkMode = true
        )

        CoreClient.initialize(
            application = this,
            projectId = CommonBuildConfig.PROJECT_ID,
            metaData = appMetaData,
        ) {
            Firebase.crashlytics.recordException(it.throwable)
        }

        AppKit.initialize(Modal.Params.Init(core = CoreClient)) { error ->
            Timber.e(tag(this), error.throwable.stackTraceToString())
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