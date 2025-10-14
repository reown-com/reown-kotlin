package com.reown.sample.dapp

import android.app.Application
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.reown.sample.common.tag
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import timber.log.Timber
import com.reown.sample.common.BuildConfig as CommonBuildConfig

class DappSampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val metaData = Modal.Model.MetaData(
            name = "Kotlin Dapp",
            description = "Kotlin Dapp Implementation",
            url = "https://appkit-lab.reown.com",
            icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
            redirect = "kotlin-dapp-wc://request",
            appLink = BuildConfig.DAPP_APP_LINK,
            linkMode = true
        )

//        CoreClient.initialize(
//            application = this,
//            projectId = CommonBuildConfig.PROJECT_ID,
//            metaData = appMetaData,
//        ) {
//            Firebase.crashlytics.recordException(it.throwable)
//        }

        AppKit.initialize(
            Modal.Params.Init(
                application = this,
                projectId = CommonBuildConfig.PROJECT_ID,
                metaData = metaData,
            ),
            onError = { error ->
                println("kobe: Error: $error")
                Timber.e(tag(this), error.throwable.stackTraceToString())
            },
            onSuccess = {
                println("kobe: AppKit init success")
            }
        )

        AppKit.setChains(AppKitChainsPresets.ethChains.values.toList())

//        val authParams = Modal.Model.AuthPayloadParams(
//            chains = AppKitChainsPresets.ethChains.values.toList().map { it.id },
//            domain = "sample.kotlin.modal",
//            uri = "https://web3inbox.com/all-apps",
//            nonce = randomBytes(12).bytesToHex(),
//            statement = "I accept the Terms of Service: https://yourDappDomain.com/",
//            methods = EthUtils.ethMethods
//        )
//        AppKit.setAuthRequestParams(authParams)

        FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
    }
}