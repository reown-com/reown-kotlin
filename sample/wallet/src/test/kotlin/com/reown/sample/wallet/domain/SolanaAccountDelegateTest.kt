package com.reown.sample.wallet.domain

import org.junit.Test

internal class SolanaAccountDelegateTest {

    @Test
    fun `decode private and pub key from keypair`() {
        with(SolanaAccountDelegate) {
            val result = decodeKeyPair("5pPSHYJyH89EuPFXfgGNeCJTaVwb3QxEkCCmcA3PpiGKPc9x8GYsQNN66WKmJpTWFk1Ci1MZUm337yE5rgDNdpWe")
            println("$result")
        }
    }
}