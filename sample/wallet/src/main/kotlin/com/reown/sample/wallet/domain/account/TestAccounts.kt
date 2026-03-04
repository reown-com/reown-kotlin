package com.reown.sample.wallet.domain.account

import com.reown.sample.common.Chains
import io.ipfs.multibase.Base16

val ACCOUNTS_1_EIP155_ADDRESS: String
    get() = EthAccountDelegate.address
const val ACCOUNTS_2_EIP155_ADDRESS = "0x15bca56b6e2728aec2532df9d436bd1600e86688"

private val accounts: List<Pair<Chains, String>>
    get() = listOf(
        Chains.ETHEREUM_MAIN to EthAccountDelegate.address,
        Chains.POLYGON_MATIC to EthAccountDelegate.address,
        Chains.ETHEREUM_KOVAN to EthAccountDelegate.address,
        Chains.POLYGON_MUMBAI to EthAccountDelegate.address,
        Chains.COSMOS to "cosmos1w605a5ejjlhp04eahjqxhjhmg8mj6nqhp8v6xc",
        Chains.BNB to EthAccountDelegate.address
    )

val PRIVATE_KEY_1: ByteArray
    get() = EthAccountDelegate.privateKey.hexToBytes()

const val ISS_DID_PREFIX = "did:pkh:"

val ISSUER: String
    get() = accounts.first().toIssuer()

fun Pair<Chains, String>.toIssuer(): String = "$ISS_DID_PREFIX${first.chainId}:$second"

fun ByteArray.bytesToHex(): String = Base16.encode(this)

fun String.hexToBytes(): ByteArray = Base16.decode(this.lowercase())
