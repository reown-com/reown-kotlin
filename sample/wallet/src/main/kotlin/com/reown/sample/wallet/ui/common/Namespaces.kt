package com.reown.sample.wallet.ui.common

import com.reown.walletkit.client.Wallet

fun getAllMethodsByChainId(namespace: Wallet.Model.Namespace.Proposal, chainId: String): List<String> {
    return namespace.methods.takeIf { namespace.chains != null && namespace.chains!!.contains(chainId) } ?: emptyList()
}

fun getAllEventsByChainId(namespace: Wallet.Model.Namespace.Proposal, chainId: String): List<String> {
    return namespace.events.takeIf { namespace.chains != null && namespace.chains!!.contains(chainId) } ?: emptyList()
}

fun getAllMethodsByChainId(namespace: Wallet.Model.Namespace.Session, accountId: String): List<String> {
    return namespace.methods.takeIf { namespace.accounts.contains(accountId) } ?: emptyList()
}

fun getAllEventsByChainId(namespace: Wallet.Model.Namespace.Session, accountId: String): List<String> {
    return namespace.events.takeIf { namespace.accounts.contains(accountId) } ?: emptyList()
}