package com.reown.sample.wallet.ui.routes.dialog_routes.session_authenticate

import com.reown.sample.wallet.domain.ACCOUNTS_1_EIP155_ADDRESS
import com.reown.sample.wallet.ui.common.peer.PeerContextUI
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.walletkit.client.Wallet

data class SessionAuthenticateUI(
    val peerUI: PeerUI,
    val messages: MutableList<String>,
    val peerContextUI: PeerContextUI,
)

data class WalletMetaData(
    val peerUI: PeerUI,
    val namespaces: Map<String, Wallet.Model.Namespace.Session>,
)

val walletMetaData = WalletMetaData(
    peerUI = PeerUI(
        peerIcon = "https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png",
        peerName = "Kotlin.Wallet",
        peerUri = "kotlin.wallet.app",
        peerDescription = ""
    ),
    namespaces = mapOf(
        "eip155" to Wallet.Model.Namespace.Session(
            chains = listOf("eip155:1", "eip155:137", "eip155:56"),
            methods = listOf(
                "eth_sendTransaction",
                "personal_sign",
                "eth_accounts",
                "eth_requestAccounts",
                "eth_call",
                "eth_getBalance",
                "eth_sendRawTransaction",
                "eth_sign",
                "eth_signTransaction",
                "eth_signTypedData"
            ),
            events = listOf("chainChanged", "accountsChanged"),
            accounts = listOf("eip155:1:$ACCOUNTS_1_EIP155_ADDRESS", "eip155:137:$ACCOUNTS_1_EIP155_ADDRESS", "eip155:56:$ACCOUNTS_1_EIP155_ADDRESS")
        ),
//        "cosmos" to Wallet.Model.Namespace.Session(
//            chains = listOf("cosmos:cosmoshub-4", "cosmos:cosmoshub-1"),
//            methods = listOf("accountsChanged", "personalSign"),
//            events = listOf("chainChanged", "chainChanged"),
//            accounts = listOf("cosmos:cosmoshub-4:cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc", "cosmos:cosmoshub-1:cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc")
//        )
    )
)