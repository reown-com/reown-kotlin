package com.walletconnect.sample.pos.printer

internal data class ReceiptRow(val label: String, val value: String, val bold: Boolean)

internal object ReceiptFormatter {

    private const val DIVIDER = "--------------------------------"
    private const val DEFAULT_FOOTER = "Thank you for your payment!"
    private const val LABEL_WIDTH = 10

    fun rows(receipt: ReceiptData): List<ReceiptRow> = buildList {
        add(ReceiptRow("ID", receipt.txId, bold = true))
        add(ReceiptRow("DATE", receipt.date, bold = true))
        add(ReceiptRow("METHOD", "WalletConnect Pay", bold = true))
        if (receipt.displayFiat.isNotBlank()) {
            add(ReceiptRow("AMOUNT", receipt.displayFiat, bold = true))
        }
        if (!receipt.tokenSymbol.isNullOrBlank() && !receipt.tokenAmountFormatted.isNullOrBlank()) {
            add(ReceiptRow("PAID WITH", "${receipt.tokenSymbol} ${receipt.tokenAmountFormatted}", bold = true))
        }
        if (!receipt.network.isNullOrBlank()) {
            add(ReceiptRow("NETWORK", receipt.network, bold = true))
        }
    }

    fun footer(receipt: ReceiptData): String = receipt.footerOverride ?: DEFAULT_FOOTER

    fun divider(): String = DIVIDER

    /**
     * Builds the DantSu ESCPOS markup string for the receipt body (everything *after* the logo).
     * The logo is printed separately because DantSu image markup requires bitmap conversion that
     * the driver handles outside this pure formatter.
     */
    fun toGenericMarkup(receipt: ReceiptData): String {
        val sb = StringBuilder()
        sb.append("[L]\n")
        sb.append("[C]$DIVIDER\n")
        sb.append("[L]\n")
        rows(receipt).forEach { row ->
            val label = row.label.padEnd(LABEL_WIDTH)
            val value = if (row.bold) "<b>${row.value}</b>" else row.value
            sb.append("[L]$label$value\n")
        }
        sb.append("[L]\n")
        sb.append("[C]$DIVIDER\n")
        sb.append("[L]\n")
        sb.append("[C]${footer(receipt)}\n")
        sb.append("[L]\n")
        return sb.toString()
    }
}
