package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

enum class Chain(val id: String) {
    ETHEREUM("eip155:1"),
    BASE("eip155:8453"),
    ARBITRUM("eip155:42161"),
    OPTIMISM("eip155:10"),
    SOLANA("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"),
    STACKS_MAINNET("stacks:1"),
    STACKS_TESTNET("stacks:2147483648")
}

interface Token

enum class StableCoin(val decimals: Int): Token {
    USDC(6),
    USDT(6),
    USDS(18),
}

enum class Coin(val decimals: Int): Token {
    ETH(18)
}

object TokenAddresses {
    private val ADDRESSES = mapOf(
        Chain.ETHEREUM to mapOf(
            StableCoin.USDT to "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            StableCoin.USDC to "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
        ),
        Chain.BASE to mapOf(
            StableCoin.USDC to "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
            StableCoin.USDT to "0x50c5725949A6F0c72E6C4a641F24049A917DB0Cb",
            StableCoin.USDS to "0x820c137fa70c8691f0e44dc420a5e53c168921dc"
        ),
        Chain.ARBITRUM to mapOf(
            StableCoin.USDC to "0xaf88d065e77c8cC2239327C5EDb3A432268e5831",
            StableCoin.USDT to "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9"
        ),
        Chain.OPTIMISM to mapOf(
            StableCoin.USDC to "0x0b2c639c533813f4aa9d7837caf62653d097ff85",
            StableCoin.USDT to "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58"
        ),
        Chain.SOLANA to mapOf(
            StableCoin.USDC to "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
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
