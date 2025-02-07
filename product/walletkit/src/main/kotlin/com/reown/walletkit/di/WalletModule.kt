package com.reown.walletkit.di

import android.os.Build
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.model.ProjectId
import com.reown.walletkit.BuildConfig
import com.reown.walletkit.use_cases.PrepareChainAbstractionUseCase
import com.reown.walletkit.use_cases.EstimateGasUseCase
import com.reown.walletkit.use_cases.ExecuteChainAbstractionUseCase
import com.reown.walletkit.use_cases.GetERC20TokenBalanceUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.PulseMetadata

@JvmSynthetic
internal fun walletKitModule() = module {
    single {
        val metadata = PulseMetadata(
            packageName = get(named(AndroidCommonDITags.PACKAGE_NAME)),
            sdkPlatform = "mobile",
            sdkVersion = "reown-kotlin-${BuildConfig.SDK_VERSION}",
            bundleId = null,
            url = null
        )
        val projectId = get<ProjectId>().value

        println("kobe: CA Client: $projectId, $metadata")

        try {
            ChainAbstractionClient(
                projectId = projectId,
                pulseMetadata = metadata
            )
        } catch (e: Exception) {
            println("kobe: CA Client: $e")
            throw e
        }
    }

    single { PrepareChainAbstractionUseCase(get()) }

    single { EstimateGasUseCase(get()) }

    single { GetERC20TokenBalanceUseCase(get()) }

    single { ExecuteChainAbstractionUseCase(get()) }
}