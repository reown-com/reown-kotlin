package com.reown.sign.engine.model.tvf

object TVF {
    val evm = listOf("eth_sendTransaction", "eth_sendRawTransaction")
    val solana = listOf("solana_signAndSendTransaction", "solana_signTransaction", "solana_signAllTransactions")
    val wallet = listOf("wallet_sendCalls")
    val all = evm + solana + wallet
}