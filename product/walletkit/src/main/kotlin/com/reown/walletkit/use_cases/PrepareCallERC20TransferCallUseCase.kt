//package com.reown.walletkit.use_cases
//
//import com.reown.walletkit.client.Wallet
//import com.reown.walletkit.client.toWallet
//import kotlinx.coroutines.runBlocking
//import uniffi.uniffi_yttrium.ChainAbstractionClient
//
//class PrepareCallERC20TransferCallUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
//    operator fun invoke(contractAddress: String, to: String, amount: String): Wallet.Model.Call {
//        return runBlocking { chainAbstractionClient.prepareErc20TransferCall(contractAddress, to, amount).toWallet() }
//    }
//}