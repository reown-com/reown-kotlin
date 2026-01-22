package com.walletconnect.pos.api

import com.walletconnect.pos.Pos

internal fun mapErrorCodeToPaymentError(code: String, message: String): Pos.PaymentEvent.PaymentError {
    return when (code) {
        ErrorCodes.PAYMENT_NOT_FOUND -> Pos.PaymentEvent.PaymentError.PaymentNotFound(message)
        ErrorCodes.PAYMENT_EXPIRED -> Pos.PaymentEvent.PaymentError.PaymentExpired(message)
        ErrorCodes.INVALID_REQUEST -> Pos.PaymentEvent.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.PaymentEvent.PaymentError.Undefined(message)
    }
}

internal fun mapCreatePaymentError(code: String, message: String): Pos.PaymentEvent.PaymentError {
    return when (code) {
        ErrorCodes.INVALID_REQUEST -> Pos.PaymentEvent.PaymentError.InvalidPaymentRequest(message)
        else -> Pos.PaymentEvent.PaymentError.CreatePaymentFailed(message)
    }
}

internal fun mapStatusToPaymentEvent(
    status: String,
    paymentId: String,
    info: PaymentInfoDto? = null
): Pos.PaymentEvent {
    return when (status) {
        PaymentStatus.REQUIRES_ACTION -> Pos.PaymentEvent.PaymentRequested
        PaymentStatus.PROCESSING -> Pos.PaymentEvent.PaymentProcessing
        PaymentStatus.SUCCEEDED -> Pos.PaymentEvent.PaymentSuccess(paymentId, info?.toPaymentInfo())
        PaymentStatus.EXPIRED -> Pos.PaymentEvent.PaymentError.PaymentExpired("Payment has expired")
        PaymentStatus.FAILED -> Pos.PaymentEvent.PaymentError.PaymentFailed("Payment failed") //TODO: add error message?
        else -> Pos.PaymentEvent.PaymentError.Undefined("Unknown payment status: $status")
    }
}

internal fun PaymentInfoDto.toPaymentInfo(): Pos.PaymentInfo {
    val display = optionAmount.display
    return Pos.PaymentInfo(
        assetName = display.assetName,
        assetSymbol = display.assetSymbol,
        networkName = display.networkName,
        amount = optionAmount.value,
        decimals = display.decimals,
        txHash = txId,
        iconUrl = display.iconUrl,
        networkIconUrl = display.networkIconUrl
    )
}

// Transaction History Mapping Functions

internal fun PaymentRecord.toTransaction(): Pos.Transaction {
    return Pos.Transaction(
        paymentId = paymentId,
        referenceId = referenceId,
        status = mapToTransactionStatus(status),
        txHash = txHash,
        fiatAmount = fiatAmount,
        fiatCurrency = fiatCurrency,
        tokenAmount = tokenAmount,
        tokenSymbol = extractTokenSymbol(tokenCaip19),
        network = mapChainIdToNetworkName(chainId),
        chainId = chainId,
        walletName = walletName,
        createdAt = createdAt,
        confirmedAt = confirmedAt
    )
}

internal fun mapToTransactionStatus(status: String): Pos.TransactionStatus {
    return when (status.lowercase()) {
        PaymentStatus.REQUIRES_ACTION -> Pos.TransactionStatus.REQUIRES_ACTION
        PaymentStatus.PROCESSING -> Pos.TransactionStatus.PROCESSING
        PaymentStatus.SUCCEEDED -> Pos.TransactionStatus.SUCCEEDED
        PaymentStatus.EXPIRED -> Pos.TransactionStatus.EXPIRED
        PaymentStatus.FAILED -> Pos.TransactionStatus.FAILED
        else -> Pos.TransactionStatus.UNKNOWN
    }
}

internal fun extractTokenSymbol(caip19: String?): String? {
    if (caip19 == null) return null
    // CAIP-19 format examples:
    // eip155:1/erc20:0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48 (USDC)
    // eip155:1/slip44:60 (ETH)
    // Extract the type part (erc20, slip44, etc.)
    val assetPart = caip19.substringAfter("/", "")
    return when {
        assetPart.startsWith("erc20:") -> "ERC20"
        assetPart.startsWith("slip44:60") -> "ETH"
        assetPart.startsWith("slip44:") -> "Native"
        else -> assetPart.substringBefore(":").uppercase().ifEmpty { null }
    }
}

internal fun mapChainIdToNetworkName(chainId: String?): String? {
    if (chainId == null) return null
    // Handle both raw chain IDs and CAIP-2 format (eip155:1)
    val numericChainId = chainId.substringAfter("eip155:", chainId)
    return when (numericChainId) {
        "1" -> "Ethereum"
        "137" -> "Polygon"
        "10" -> "Optimism"
        "42161" -> "Arbitrum"
        "8453" -> "Base"
        "56" -> "BNB Chain"
        "43114" -> "Avalanche"
        "250" -> "Fantom"
        "100" -> "Gnosis"
        "324" -> "zkSync Era"
        "59144" -> "Linea"
        "534352" -> "Scroll"
        else -> "Chain $numericChainId"
    }
}

internal fun TransactionStatsDto?.toTransactionStats(): Pos.TransactionStats? {
    if (this == null) return null
    return Pos.TransactionStats(
        totalTransactions = totalTransactions,
        totalCustomers = totalCustomers,
        totalRevenue = totalRevenue?.let {
            Pos.TotalRevenue(
                amount = it.amount,
                currency = it.currency
            )
        }
    )
}