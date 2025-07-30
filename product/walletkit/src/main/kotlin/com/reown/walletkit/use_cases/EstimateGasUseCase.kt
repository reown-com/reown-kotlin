//package com.reown.walletkit.use_cases
//
//import com.reown.walletkit.client.Wallet
//import com.reown.walletkit.client.toWallet
//import kotlinx.coroutines.runBlocking
//import uniffi.uniffi_yttrium.ChainAbstractionClient
//
//class EstimateGasUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
//    operator fun invoke(chainId: String): Wallet.Model.EstimatedFees {
//        return runBlocking { chainAbstractionClient.estimateFees(chainId).toWallet() }
//    }
//}