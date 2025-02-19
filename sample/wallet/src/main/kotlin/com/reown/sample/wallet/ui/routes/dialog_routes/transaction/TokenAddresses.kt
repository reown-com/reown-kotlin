package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

enum class Chain(val id: String) {
    BASE("eip155:8453"),
    ARBITRUM("eip155:42161"),
    OPTIMISM("eip155:10")
}

enum class StableCoin {
    USDC,
    USDT
}

object TokenAddresses {
    private val ADDRESSES = mapOf(
        Chain.BASE to mapOf(
            StableCoin.USDC to "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
            StableCoin.USDT to "0x50c5725949A6F0c72E6C4a641F24049A917DB0Cb"
        ),
        Chain.ARBITRUM to mapOf(
            StableCoin.USDC to "0xaf88d065e77c8cC2239327C5EDb3A432268e5831",
            StableCoin.USDT to "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9"
        ),
        Chain.OPTIMISM to mapOf(
            StableCoin.USDC to "0x7F5c764cBc14f9669B88837ca1490cCa17c31607",
            StableCoin.USDT to "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58"
        )
    )

    private fun getTokenAddress(chain: Chain, token: StableCoin): String {
        return ADDRESSES[chain]?.get(token)
            ?: throw IllegalArgumentException("No address found for $token on $chain")
    }

    fun StableCoin.getAddressOn(chain: Chain): String {
        return getTokenAddress(chain, this)
    }
}
