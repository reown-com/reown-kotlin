@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.reown.sample.wallet.R

private data class ChainInfo(
    val name: String,
    @DrawableRes val icon: Int?,
    val color: Color,
    val label: String,
)

private val chainRegistry: Map<String, ChainInfo> = mapOf(
    // EIP155 chains
    "eip155:1" to ChainInfo("Ethereum", com.reown.sample.common.R.drawable.ic_ethereum, Color(0xFF627EEA), "E"),
    "eip155:5" to ChainInfo("Ethereum Goerli", com.reown.sample.common.R.drawable.ic_ethereum, Color(0xFF627EEA), "E"),
    "eip155:11155111" to ChainInfo("Ethereum Sepolia", com.reown.sample.common.R.drawable.ic_ethereum, Color(0xFF627EEA), "E"),
    "eip155:42" to ChainInfo("Ethereum Kovan", com.reown.sample.common.R.drawable.ic_ethereum, Color(0xFF627EEA), "E"),
    "eip155:137" to ChainInfo("Polygon", com.reown.sample.common.R.drawable.ic_polygon, Color(0xFF8247E5), "P"),
    "eip155:80001" to ChainInfo("Polygon Mumbai", com.reown.sample.common.R.drawable.ic_polygon, Color(0xFF8247E5), "P"),
    "eip155:10" to ChainInfo("Optimism", com.reown.sample.common.R.drawable.ic_optimism, Color(0xFFFF0420), "O"),
    "eip155:69" to ChainInfo("Optimism Kovan", com.reown.sample.common.R.drawable.ic_optimism, Color(0xFFFF0420), "O"),
    "eip155:11155420" to ChainInfo("Optimism Sepolia", com.reown.sample.common.R.drawable.ic_optimism, Color(0xFFFF0420), "O"),
    "eip155:42161" to ChainInfo("Arbitrum", com.reown.sample.common.R.drawable.ic_arbitrum, Color(0xFF28A0F0), "A"),
    "eip155:421611" to ChainInfo("Arbitrum Rinkeby", com.reown.sample.common.R.drawable.ic_arbitrum, Color(0xFF28A0F0), "A"),
    "eip155:421614" to ChainInfo("Arbitrum Sepolia", com.reown.sample.common.R.drawable.ic_arbitrum, Color(0xFF28A0F0), "A"),
    "eip155:56" to ChainInfo("BNB Smart Chain", com.reown.sample.common.R.drawable.bnb, Color(0xFFF0B90B), "B"),
    "eip155:8453" to ChainInfo("Base", R.drawable.ic_base, Color(0xFF0052FF), "B"),
    "eip155:43114" to ChainInfo("Avalanche", R.drawable.ic_avalanche, Color(0xFFE84142), "Av"),
    "eip155:43113" to ChainInfo("Avalanche Fuji", R.drawable.ic_avalanche, Color(0xFFE84142), "Av"),
    "eip155:42220" to ChainInfo("Celo", com.reown.sample.common.R.drawable.ic_celo, Color(0xFFFCFF52), "C"),
    "eip155:44787" to ChainInfo("Celo Alfajores", com.reown.sample.common.R.drawable.ic_celo, Color(0xFFFCFF52), "C"),
    "eip155:250" to ChainInfo("Fantom", R.drawable.ic_fantom, Color(0xFF1969FF), "F"),
    "eip155:100" to ChainInfo("Gnosis", R.drawable.ic_gnosis, Color(0xFF04795B), "G"),
    "eip155:9001" to ChainInfo("Evmos", R.drawable.ic_evmos, Color(0xFFED4E33), "Ev"),
    "eip155:324" to ChainInfo("zkSync Era", R.drawable.ic_zksync, Color(0xFF8C8DFC), "zk"),
    "eip155:314" to ChainInfo("Filecoin", R.drawable.ic_filecoin, Color(0xFF0090FF), "Fi"),
    "eip155:4689" to ChainInfo("IoTeX", R.drawable.ic_iotex, Color(0xFF44FFB2), "Io"),
    "eip155:1088" to ChainInfo("Metis", R.drawable.ic_metis, Color(0xFF00DACC), "M"),
    "eip155:1284" to ChainInfo("Moonbeam", R.drawable.ic_moonbeam, Color(0xFF53CBC9), "Mb"),
    "eip155:1285" to ChainInfo("Moonriver", R.drawable.ic_moonriver, Color(0xFFF2B705), "Mr"),
    "eip155:7777777" to ChainInfo("Zora", R.drawable.ic_zora, Color(0xFF2B5DF0), "Z"),
    "eip155:1313161554" to ChainInfo("Aurora", R.drawable.ic_aurora, Color(0xFF70D44B), "Au"),
    // Solana
    "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp" to ChainInfo("Solana", com.reown.sample.common.R.drawable.ic_solana, Color(0xFF9945FF), "S"),
    // Cosmos
    "cosmos:cosmoshub-4" to ChainInfo("Cosmos", com.reown.sample.common.R.drawable.ic_cosmos, Color(0xFFB2B2B2), "Co"),
    // SUI
    "sui:mainnet" to ChainInfo("SUI", R.drawable.ic_sui, Color(0xFF6FBCF0), "SU"),
    "sui:testnet" to ChainInfo("SUI Testnet", R.drawable.ic_sui, Color(0xFF6FBCF0), "SU"),
    "sui:devnet" to ChainInfo("SUI Devnet", R.drawable.ic_sui, Color(0xFF6FBCF0), "SU"),
    // TON
    "ton:-239" to ChainInfo("TON", R.drawable.ic_ton, Color(0xFF0098EA), "T"),
    "ton:-3" to ChainInfo("TON Testnet", R.drawable.ic_ton, Color(0xFF0098EA), "T"),
    // TRON
    "tron:0x2b6653dc" to ChainInfo("Tron", R.drawable.ic_tron, Color(0xFFFF0013), "Tr"),
    "tron:0xcd8690dc" to ChainInfo("Tron Testnet", R.drawable.ic_tron, Color(0xFFFF0013), "Tr"),
)

fun chainInfo(chainId: String): Pair<Color, String> {
    val info = chainRegistry[chainId]
    return if (info != null) {
        info.color to info.label
    } else {
        Color(0xFF666666) to chainId.takeLast(2)
    }
}

fun getChainName(chainId: String): String {
    return chainRegistry[chainId]?.name ?: chainId.take(24)
}

@DrawableRes
fun getChainIcon(chainId: String): Int? {
    return chainRegistry[chainId]?.icon
}
