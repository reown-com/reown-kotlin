package com.reown.walletkit.di

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.model.ProjectId
import com.reown.walletkit.BuildConfig
import com.reown.walletkit.use_cases.EstimateGasUseCase
import com.reown.walletkit.use_cases.ExecuteChainAbstractionUseCase
import com.reown.walletkit.use_cases.GetERC20TokenBalanceUseCase
import com.reown.walletkit.use_cases.PrepareChainAbstractionUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import uniffi.uniffi_yttrium.ChainAbstractionClient
import uniffi.yttrium.PulseMetadata

@JvmSynthetic
internal fun walletKitModule() = module {
    single {
        ChainAbstractionClient(
            projectId = get<ProjectId>().value,
            pulseMetadata = PulseMetadata(
                packageName = null,
                sdkPlatform = "mobile",
                sdkVersion = "reown-kotlin-${BuildConfig.SDK_VERSION}",
                bundleId = get(named(AndroidCommonDITags.PACKAGE_NAME)),
                url = null
            )
        )
    }

    single { PrepareChainAbstractionUseCase(get()) }

    single { EstimateGasUseCase(get()) }

    single { GetERC20TokenBalanceUseCase(get()) }

    single { ExecuteChainAbstractionUseCase(get()) }
}