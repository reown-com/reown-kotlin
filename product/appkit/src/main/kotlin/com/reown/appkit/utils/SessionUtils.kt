package com.reown.appkit.utils

import com.reown.android.internal.utils.CoreValidator
import com.reown.sign.client.Sign
import com.walletconnect.util.Empty
import com.reown.appkit.client.Modal
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.Account
import com.reown.appkit.client.toModal
import com.reown.appkit.domain.model.Session

internal fun String.toVisibleAddress() = "${take(4)}...${takeLast(4)}"

internal fun List<Modal.Model.Chain>.getSelectedChain(chainId: String?) = find { it.id == chainId } ?: first()

internal fun Modal.Model.Session.getAddress(selectedChain: Modal.Model.Chain) = getAccounts().find { it.startsWith(selectedChain.id) }?.split(":")?.last() ?: String.Empty

internal fun Modal.Model.Session.getChains() = namespaces.values.toList()
    .flatMap { it.chains ?: listOf() }
    .filter { CoreValidator.isChainIdCAIP2Compliant(it) }
    .mapNotNull { it.toChain() }
    .ifEmpty { getDefaultChain() }

internal fun Modal.Model.UpdatedSession.getChains() = namespaces.values.toList()
    .flatMap { it.chains ?: listOf() }
    .mapNotNull { it.toChain() }

internal fun Modal.Model.UpdatedSession.getAddress(selectedChain: Modal.Model.Chain) = namespaces.values.toList().flatMap { it.accounts }.find { it.startsWith(selectedChain.id) }?.split(":")?.last() ?: String.Empty
internal fun Modal.Model.UpdatedSession.toSession(selectedChain: Modal.Model.Chain): Session.WalletConnect {
    val chain = getChains().firstOrNull() ?: selectedChain
    return Session.WalletConnect(getAddress(chain), chain.id, topic)
}

internal fun String.toChain() = AppKit.chains.find { it.id == this }

private fun Modal.Model.Session.getAccounts() = namespaces.values.toList().flatMap { it.accounts }

private fun Modal.Model.Session.getDefaultChain() = getAccounts()
    .accountsToChainId()
    .filter { CoreValidator.isChainIdCAIP2Compliant(it) }
    .mapNotNull { it.toChain() }

private fun List<String>.accountsToChainId() = map {
    val (chainNamespace, chainReference, _) = it.split(":")
    "$chainNamespace:$chainReference"
}

internal fun Modal.Model.ApprovedSession.WalletConnectSession.getAddress(chain: Modal.Model.Chain) = namespaces.values.toList()
    .flatMap { it.accounts }
    .find { it.startsWith(chain.id) }
    ?.split(":")
    ?.last() ?: String.Empty

internal fun Session.getChains() = when(this) {
    is Session.Coinbase -> AppKit.chains.filter { it.id == this.chain }
    is Session.WalletConnect -> AppKit.getActiveSessionByTopic(topic)?.getChains() ?: AppKit.chains
}

internal fun Session.toAccount() = Account(address, getChain(chain))

internal fun Sign.Model.Session.toAccount(session: Session.WalletConnect) = toModal().let {
    val chain = session.chain.toChain() ?: return@let null
    val address = it.getAddress(chain)
    Account(address, chain)
}

internal fun getChain(chainId: String) = AppKit.chains.find { it.id == chainId } ?: AppKit.chains.first()

internal fun Session.toConnectorType() = when(this) {
    is Session.Coinbase -> Modal.ConnectorType.WALLET_CONNECT
    is Session.WalletConnect -> Modal.ConnectorType.COINBASE
}

internal fun Modal.Model.ApprovedSession.toSession(chain: Modal.Model.Chain) = when (val approvedSession = this) {
    is Modal.Model.ApprovedSession.WalletConnectSession -> Session.WalletConnect(chain = chain.id, topic = approvedSession.topic, address = approvedSession.getAddress(chain))
    is Modal.Model.ApprovedSession.CoinbaseSession -> Session.Coinbase(chain = "eip155:${approvedSession.networkId}", approvedSession.address)
}
