package com.reown.sample.wallet.ui.routes.dialog_routes.session_proposal

import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.CantonAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.ui.common.peer.PeerContextUI
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.routes.dialog_routes.transaction.Chain
import com.reown.walletkit.client.Wallet

data class SessionProposalUI(
    val peerUI: PeerUI,
    val namespaces: Map<String, Wallet.Model.Namespace.Proposal>,
    val optionalNamespaces: Map<String, Wallet.Model.Namespace.Proposal> = mapOf(),
    val peerContext: PeerContextUI,
    val redirect: String,
    val messagesToSign: List<String> = emptyList(),
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

fun walletMetaData(): WalletMetaData {
    val eip155Address = EthAccountDelegate.address
    return WalletMetaData(
        peerUI = PeerUI(
            peerIcon = "https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png",
            peerName = "Kotlin.Wallet",
            peerUri = "kotlin.wallet.app",
            peerDescription = ""
        ),
        namespaces = mapOf(
            "eip155" to Wallet.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:137", "eip155:56", "eip155:42161", "eip155:8453", "eip155:10", "eip155:11155111", "eip155:42220", "eip155:143"),
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
                    "eip155:1:$eip155Address",
                    "eip155:137:$eip155Address",
                    "eip155:56:$eip155Address",
                    "eip155:42161:$eip155Address",
                    "eip155:8453:$eip155Address",
                    "eip155:10:$eip155Address",
                    "eip155:11155111:$eip155Address",
                    "eip155:42220:$eip155Address",
                    "eip155:143:$eip155Address",
                )
            ),
            "cosmos" to Wallet.Model.Namespace.Session(
                chains = listOf("cosmos:cosmoshub-4", "cosmos:cosmoshub-1"),
                methods = listOf("cosmos_signDirect", "cosmos_signAmino"),
                events = listOf(),
                accounts = listOf(
                    "cosmos:cosmoshub-4:cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc",
                    "cosmos:cosmoshub-1:cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc"
                )
            ),
            "ton" to Wallet.Model.Namespace.Session(
                chains = listOf(TONAccountDelegate.mainnet),
                methods = listOf("ton_sendMessage", "ton_signData"),
                events = listOf(),
                accounts = listOf(TONAccountDelegate.caip10MainnetAddress)
            ),
            "stacks" to Wallet.Model.Namespace.Session(
                chains = listOf(Chain.STACKS_MAINNET.id, Chain.STACKS_TESTNET.id),
                methods = listOf("stx_transferStx", "stx_signMessage"),
                events = listOf("stx_accountsChanged", "stx_chainChanged"),
                accounts = listOf(StacksAccountDelegate.mainnetAddress, StacksAccountDelegate.testnetAddress)
            ),
            "solana" to Wallet.Model.Namespace.Session(
                chains = listOf(Chain.SOLANA.id),
                methods = listOf("solana_signMessage", "solana_signTransaction", "solana_signAndSendTransaction", "solana_signAllTransactions"),
                events = listOf("accountsChanged", "chainChanged"),
                accounts = listOf(SolanaAccountDelegate.keys.third)
            ),
            "sui" to Wallet.Model.Namespace.Session(
                chains = listOf(Chain.SUI.id, Chain.SUI_TESTNET.id),
                methods = listOf("sui_signAndExecuteTransaction", "sui_signTransaction", "sui_signPersonalMessage"),
                events = listOf("accountsChanged", "chainChanged"),
                accounts = listOf(SuiAccountDelegate.mainnetAddress, SuiAccountDelegate.testnetAddress)
            ),
            "tron" to Wallet.Model.Namespace.Session(
                chains = listOf(TronAccountDelegate.mainnet),
                methods = listOf("tron_signMessage", "tron_signTransaction"),
                events = listOf(),
                accounts = listOf(TronAccountDelegate.caip10MainnetAddress)
            ),
            "canton" to Wallet.Model.Namespace.Session(
                chains = listOf(CantonAccountDelegate.mainnet, CantonAccountDelegate.devnet),
                methods = listOf(
                    "canton_prepareSignExecute",
                    "canton_listAccounts",
                    "canton_getPrimaryAccount",
                    "canton_getActiveNetwork",
                    "canton_status",
                    "canton_ledgerApi",
                    "canton_signMessage"
                ),
                events = listOf("accountsChanged", "statusChanged", "chainChanged"),
                accounts = listOf(
                    CantonAccountDelegate.caip10MainnetAddress,
                    CantonAccountDelegate.caip10DevnetAddress
                )
            )
        )
    )
}
