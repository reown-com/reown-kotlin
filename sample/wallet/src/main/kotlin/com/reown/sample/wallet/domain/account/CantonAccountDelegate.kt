@file:JvmSynthetic

package com.reown.sample.wallet.domain.account

object CantonAccountDelegate {
    const val PARTY_ID = "operator::1220abcdef1234567890abcdef1234567890abcdef1234567890abcdef12345678"
    const val PARTY_ID_URL_ENCODED = "operator%3A%3A1220abcdef1234567890abcdef1234567890abcdef1234567890abcdef12345678"
    const val PUBLIC_KEY_BASE64 = "q83vEjRWeJCrze8SNFZbkKvN7xI0VluQq83vEjRWeJg="
    const val NAMESPACE = "1220abcdef1234567890abcdef1234567890abcdef1234567890abcdef12345678"

    const val mainnet = "canton:mainnet"
    const val devnet = "canton:devnet"

    val caip10MainnetAddress: String = "$mainnet:$PARTY_ID_URL_ENCODED"
    val caip10DevnetAddress: String = "$devnet:$PARTY_ID_URL_ENCODED"
}
