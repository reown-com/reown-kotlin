package com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal

import com.reown.sample.wallet.domain.ACCOUNTS_1_EIP155_ADDRESS
import com.reown.sample.wallet.ui.common.peer.PeerContextUI
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.walletkit.client.Wallet

data class SessionProposalUI(
    val peerUI: PeerUI,
    val namespaces: Map<String, Wallet.Model.Namespace.Proposal>,
    val optionalNamespaces: Map<String, Wallet.Model.Namespace.Proposal> = mapOf(),
    val peerContext: PeerContextUI,
    val redirect: String,
    val pubKey: String,
)

data class WalletMetaData(
    val peerUI: PeerUI,
    val namespaces: Map<String, Wallet.Model.Namespace.Session>,
)

//val smartAccountWalletMetadata =
//    WalletMetaData(
//        peerUI = PeerUI(
//            peerIcon = "https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png",
//            peerName = "Kotlin.Wallet",
//            peerUri = "kotlin.wallet.app",
//            peerDescription = "Kotlin Wallet Description"
//        ),
//        namespaces = mapOf(
//            "eip155" to Wallet.Model.Namespace.Session(
//                chains = listOf("eip155:11155111"),
//                methods = listOf(
//                    "eth_sendTransaction",
//                    "personal_sign",
//                    "eth_accounts",
//                    "eth_requestAccounts",
//                    "eth_call",
//                    "eth_getBalance",
//                    "eth_sendRawTransaction",
//                    "eth_sign",
//                    "eth_signTransaction",
//                    "eth_signTypedData",
//                    "eth_signTypedData_v4",
//                    "wallet_switchEthereumChain",
//                    "wallet_addEthereumChain",
//                    "wallet_sendCalls",
//                    "wallet_getCallsStatus"
//                ),
//                events = listOf("chainChanged", "accountsChanged", "connect", "disconnect"),
//                accounts = listOf(
//                    "eip155:11155111:${
//                        try {
//                            WalletKit.getSmartAccount(Wallet.Params.GetSmartAccountAddress(Wallet.Params.Account(EthAccountDelegate.sepoliaAddress)))
//                        } catch (e: Exception) {
//                            println("Getting SA account error: ${e.message}")
//                            recordError(e)
//                            ""
//                        }
//                    }"
//                )
//            )
//        )
//    )

val walletMetaData = WalletMetaData(
    peerUI = PeerUI(
        peerIcon = "https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png",
        peerName = "Kotlin.Wallet",
        peerUri = "kotlin.wallet.app",
        peerDescription = ""
    ),
    namespaces = mapOf(
        "eip155" to Wallet.Model.Namespace.Session(
            chains = listOf("eip155:1", "eip155:137", "eip155:56", "eip155:42161", "eip155:8453", "eip155:10"),
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
                "eth_signTypedData",
                "eth_signTypedData_v4",
                "wallet_switchEthereumChain",
                "wallet_addEthereumChain",
                "wallet_sendCalls",
                "wallet_getCallsStatus",
                "wallet_getAssets"
            ),
            events = listOf("chainChanged", "accountsChanged", "connect", "disconnect"),
            accounts = listOf(
                "eip155:1:$ACCOUNTS_1_EIP155_ADDRESS",
                "eip155:137:$ACCOUNTS_1_EIP155_ADDRESS",
                "eip155:56:$ACCOUNTS_1_EIP155_ADDRESS",
                "eip155:42161:$ACCOUNTS_1_EIP155_ADDRESS",
                "eip155:8453:$ACCOUNTS_1_EIP155_ADDRESS",
                "eip155:10:$ACCOUNTS_1_EIP155_ADDRESS",
            )
        ),
        "cosmos" to Wallet.Model.Namespace.Session(
            chains = listOf("cosmos:cosmoshub-4", "cosmos:cosmoshub-1"),
            methods = listOf("cosmos_signDirect", "cosmos_signAmino"),
            events = listOf(),
            accounts = listOf("cosmos:cosmoshub-4:cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc", "cosmos:cosmoshub-1:cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc")
        ),
        "solana" to Wallet.Model.Namespace.Session(
            chains = listOf("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp", "solana:4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z", "solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1", "solana:8E9rvCKLFQia2Y35HXjjpWzj8weVo44K"),
            methods = listOf("solana_signMessage", "solana_signTransaction", "solana_signAndSendTransaction", "solana_signAllTransactions"),
            events = listOf("accountsChanged", "chainChanged"),
            accounts = listOf(
                "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp:DoMfA4MGqmAhstknCtcFFen1pr8oSha8yK2KBPzjr7g5",
                "solana:4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z:DoMfA4MGqmAhstknCtcFFen1pr8oSha8yK2KBPzjr7g5",
                "solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1:DoMfA4MGqmAhstknCtcFFen1pr8oSha8yK2KBPzjr7g5",
                "solana:8E9rvCKLFQia2Y35HXjjpWzj8weVo44K:DoMfA4MGqmAhstknCtcFFen1pr8oSha8yK2KBPzjr7g5"
            )
        )
    )
)