@file:JvmSynthetic

package com.walletconnect.sample.pos.model

enum class SymbolPosition { LEFT, RIGHT }

enum class Currency(
    val code: String,
    val displayName: String,
    val symbol: String,
    val unit: String,
    val symbolPosition: SymbolPosition
) {
    USD(
        code = "USD",
        displayName = "US Dollar",
        symbol = "$",
        unit = "iso4217/USD",
        symbolPosition = SymbolPosition.LEFT
    ),
    EUR(
        code = "EUR",
        displayName = "Euro",
        symbol = "€",
        unit = "iso4217/EUR",
        symbolPosition = SymbolPosition.RIGHT
    );

    companion object {
        fun fromCode(code: String): Currency =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: USD
    }
}

fun formatAmountWithSymbol(amount: String, currency: Currency): String =
    when (currency.symbolPosition) {
        SymbolPosition.LEFT -> "${currency.symbol}$amount"
        SymbolPosition.RIGHT -> "$amount${currency.symbol}"
    }
