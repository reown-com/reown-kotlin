package com.reown.android.internal.common.modal.domain.usecase

import android.content.Context
import com.reown.android.internal.common.modal.data.model.Wallet
import com.reown.android.utils.isWalletInstalled

interface GetSampleWalletsUseCaseInterface {
    suspend operator fun invoke(): List<Wallet>
}

internal class GetSampleWalletsUseCase(
    private val context: Context
) : GetSampleWalletsUseCaseInterface {
    override suspend fun invoke(): List<Wallet> {
        val samples = mapOf(
            "com.reown.sample.wallet.debug" to SampleWalletDebug,
            "com.reown.sample.wallet.internal" to SampleWalletInternal,
            "com.reown.sample.wallet" to SampleWalletRelease,
            "com.walletconnect.web3wallet.rnsample.internal" to RNSampleWalletInternal,
            "com.walletconnect.web3wallet.rnsample" to RNSampleWallet,
            "com.walletconnect.flutterwallet" to FLSampleWallet,
            "com.walletconnect.flutterwallet.internal" to FLSampleWalletInternal
        )
        samples.forEach { (walletPackage, wallet) ->
            wallet.apply {
                isWalletInstalled = context.packageManager.isWalletInstalled(walletPackage)
            }
        }
        return samples.map { it.value }.filter { it.isWalletInstalled }
    }
}


private val SampleWalletDebug = Wallet(
    id = "SampleWalletDebug",
    name = "Android Sample Debug",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "1",
    mobileLink = "kotlin-web3wallet://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/wallet_debug",
    true
)

private val SampleWalletInternal = Wallet(
    id = "SampleWalletInternal",
    name = "Android Sample Internal",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "2",
    mobileLink = "kotlin-web3wallet://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/wallet_internal",
    true
)

private val SampleWalletRelease = Wallet(
    id = "SampleWalletRelease",
    name = "Android Sample Release",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "3",
    mobileLink = "kotlin-web3wallet://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/wallet_release",
    true
)

private val RNSampleWalletInternal = Wallet(
    id = "RNSampleWallet",
    name = "RN Sample",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "4",
    mobileLink = "rn-web3wallet://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/rn_walletkit_internal",
    true
)

private val RNSampleWallet = Wallet(
    id = "RNSampleWalletInternal",
    name = "RN Sample Internal",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "5",
    mobileLink = "rn-web3wallet://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/rn_walletkit",
    true
)

private val FLSampleWallet = Wallet(
    id = "FLSampleWallet",
    name = "FL Sample",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "6",
    mobileLink = "wcflutterwallet://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/flutter_walletkit",
    true
)

private val FLSampleWalletInternal = Wallet(
    id = "FLSampleWalletInternal",
    name = "FL Sample Internal",
    homePage = "https://walletconnect.com",
    imageUrl = "https://raw.githubusercontent.com/WalletConnect/WalletConnectKotlinV2/develop/sample/wallet/src/main/res/drawable-xxxhdpi/wc_icon.png",
    order = "7",
    mobileLink = "wcflutterwallet-internal://",
    playStore = null,
    webAppLink = null,
    linkMode = "https://appkit-lab.reown.com/flutter_walletkit_internal",
    true
)