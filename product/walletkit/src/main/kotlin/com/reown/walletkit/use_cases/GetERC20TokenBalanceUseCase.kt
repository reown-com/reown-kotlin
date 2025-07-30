//package com.reown.walletkit.use_cases
//
//import kotlinx.coroutines.runBlocking
//import uniffi.uniffi_yttrium.ChainAbstractionClient
//
//class GetERC20TokenBalanceUseCase(private val chainAbstractionClient: ChainAbstractionClient) {
//    operator fun invoke(chainId: String, tokenAddress: String, ownerAddress: String): String {
//        return runBlocking { chainAbstractionClient.erc20TokenBalance(chainId, tokenAddress, ownerAddress) }
//    }
//}