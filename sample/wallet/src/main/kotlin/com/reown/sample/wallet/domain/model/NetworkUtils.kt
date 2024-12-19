package com.reown.sample.wallet.domain.model

import com.reown.sample.wallet.R

object NetworkUtils {
    fun getNameByChainId(chainId: String): String {
        return when (chainId) {
            "eip155:10" -> "Optimism"
            "eip155:8453" -> "Base"
            "eip155:42161" -> "Arbitrum"
            else -> chainId
        }
    }

    fun getIconByChainId(chainId: String): Int {
        return when (chainId) {
            "eip155:10" -> R.drawable.ic_optimism
            "eip155:8453" -> R.drawable.base_network_logo
            "eip155:42161" -> R.drawable.ic_arbitrum
            else -> com.reown.sample.common.R.drawable.ic_walletconnect_circle_blue
        }
    }
}