package com.walletconnect.sample.pos.components

import com.walletconnect.pos.Pos

enum class TransactionFilter(val label: String, val statuses: List<Pos.TransactionStatus>?) {
    ALL("All", null),
    FAILED("Failed", listOf(Pos.TransactionStatus.FAILED)),
    CANCELLED("Cancelled", listOf(Pos.TransactionStatus.CANCELLED)),
    EXPIRED("Expired", listOf(Pos.TransactionStatus.EXPIRED)),
    PENDING("Pending", listOf(Pos.TransactionStatus.REQUIRES_ACTION, Pos.TransactionStatus.PROCESSING)),
    COMPLETED("Completed", listOf(Pos.TransactionStatus.SUCCEEDED))
}
