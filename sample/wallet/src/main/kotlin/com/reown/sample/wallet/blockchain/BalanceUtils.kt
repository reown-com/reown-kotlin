package com.reown.sample.wallet.blockchain

import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.log10

private const val TRUST_WALLET_LOGO_BASE =
    "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains"

private fun nativeToken(
    name: String,
    symbol: String,
    chainId: String,
    decimals: String,
    logo: String,
) = TokenBalance(
    name = name,
    symbol = symbol,
    chainId = chainId,
    address = null,
    value = 0.0,
    price = 0.0,
    quantity = TokenQuantity(decimals = decimals, numeric = "0"),
    iconUrl = "$TRUST_WALLET_LOGO_BASE/$logo/info/logo.png"
)

/**
 * Mainnet native tokens always shown (with a zero-balance default) so the user
 * can always read/copy the account address even when the balance API omits the
 * native row. The chainId matches the chainId used to fetch that chain so a real
 * API balance overrides this zero default.
 */
internal val mainnetNativeTokens: List<TokenBalance> = listOf(
    nativeToken("Ethereum", "ETH", "eip155:1", "18", "ethereum"),
    nativeToken("Solana", "SOL", Chain.SOLANA.id, "9", "solana"),
    nativeToken("Sui", "SUI", Chain.SUI.id, "9", "sui"),
    nativeToken("TON", "TON", TONAccountDelegate.mainnet, "9", "ton"),
    nativeToken("TRON", "TRX", TronAccountDelegate.mainnet, "6", "tron"),
)

private val nativeChainIds: Set<String> = mainnetNativeTokens.map { it.chainId }.toSet()

/**
 * Formats a token quantity for display: two decimals normally, but for small
 * amounts (< 0.01) it keeps adding leading zeros until the first non-zero decimal
 * digit (e.g. 0.00034 -> "0.0003") so the value isn't shown as "0.00".
 */
internal fun formatTokenAmount(numeric: String): String {
    val num = numeric.toDoubleOrNull() ?: return numeric
    if (num <= 0.0) return "0"

    val symbols = DecimalFormatSymbols(Locale.US)
    val pattern = if (num >= 0.01) {
        "#,##0.00"
    } else {
        // Position of the first significant decimal digit.
        val decimals = ceil(-log10(num)).toInt().coerceAtLeast(2)
        "0." + "0".repeat(decimals)
    }
    return DecimalFormat(pattern, symbols).apply { roundingMode = RoundingMode.HALF_UP }.format(num)
}

/** Symbols of known spam/airdrop tokens to hide, matched case-insensitively. */
private val spamSymbols: Set<String> = setOf(
    "MANTRA POS", "AMPAR", "ACHIVX", "BASED", "GT", "MY"
)

/** Domain-like segment ("name.tld") that legitimate token symbols never contain. */
private val domainRegex = Regex("[a-z0-9][a-z0-9-]*\\.[a-z]{2,}", RegexOption.IGNORE_CASE)

/**
 * Detects scam/airdrop tokens that abuse the symbol field to advertise a website
 * (e.g. "USD0 [www.usual.finance]", "ecAVAX - https://invest...", "www.2base.cfd")
 * or that match a known spam symbol blocklist.
 */
internal fun isSpam(token: TokenBalance): Boolean {
    val symbol = token.symbol
    val lower = symbol.lowercase()
    if (lower.contains("http://") || lower.contains("https://") ||
        lower.contains("www.") || lower.contains("://")
    ) return true
    if (domainRegex.containsMatchIn(symbol)) return true
    if (symbol.contains("[") || symbol.contains("]")) return true
    if (symbol.trim().uppercase() in spamSymbols) return true
    return false
}

/**
 * Filters out dust (zero-value, zero-quantity non-native tokens) and ensures a
 * zero-balance native row exists for each [availableNativeChainIds] chain so its
 * address stays visible. Returns the list sorted by USD value descending.
 */
internal fun processBalances(
    apiBalances: List<TokenBalance>,
    availableNativeChainIds: Set<String>,
): List<TokenBalance> {
    val filtered = apiBalances.filter { b ->
        b.value > 0 ||
            (b.address == null && b.chainId in nativeChainIds) ||
            (b.quantity.numeric.toDoubleOrNull() ?: 0.0) > 0
    }

    val result = filtered.toMutableList()
    for (native in mainnetNativeTokens) {
        if (native.chainId !in availableNativeChainIds) continue
        val hasNative = result.any { it.chainId == native.chainId && it.address == null }
        if (!hasNative) result.add(native)
    }

    return result.sortedByDescending { it.value }
}
