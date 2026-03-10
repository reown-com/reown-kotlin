package com.reown.sample.wallet

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.domain.client.TONClient
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.web3j.crypto.Sign

@RunWith(AndroidJUnit4::class)
class ImportTestValuesInstrumentedTest {

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        app.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        EthAccountDelegate.application = app
        TronAccountDelegate.application = app
        TONAccountDelegate.application = app
        SolanaAccountDelegate.application = app
        StacksAccountDelegate.application = app
        SuiAccountDelegate.application = app

        TONClient.init(app.packageName)
    }

    @Test
    fun importsProvidedValuesAcrossSupportedChains() {
        EthAccountDelegate.importFromMnemonic(COMMON_MNEMONIC)
        assertTrue(EthAccountDelegate.address.isNotBlank())

        EthAccountDelegate.privateKey = TEST_HEX_PRIVATE_KEY
        assertEquals(TEST_HEX_PRIVATE_KEY, EthAccountDelegate.privateKey)

        val tonParts = TON_SECRET_PUBLIC.split(":", limit = 2)
        TONAccountDelegate.importKeypair(sk = tonParts[0], pk = tonParts[1])
        assertTrue(TONAccountDelegate.addressFriendly.isNotBlank())

        SolanaAccountDelegate.keyPair = SOLANA_KEYPAIR_BASE58
        assertTrue(SolanaAccountDelegate.keys.second.isNotBlank())

        SuiAccountDelegate.keypair = SUI_KEYPAIR
        assertTrue(SuiAccountDelegate.address.isNotBlank())

        val tronCompressedPublicKey = Sign.publicPointFromPrivate(BigInteger(TEST_HEX_PRIVATE_KEY, 16))
            .getEncoded(true)
            .toHex()
        TronAccountDelegate.publicKey = tronCompressedPublicKey
        TronAccountDelegate.secretKey = TEST_HEX_PRIVATE_KEY
        assertTrue(TronAccountDelegate.address.isNotBlank())

        StacksAccountDelegate.importedWallet = COMMON_MNEMONIC
        assertTrue(StacksAccountDelegate.mainnetAddress.startsWith("stacks:1:"))
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val SHARED_PREFS_NAME = "Wallet_Sample_Shared_Prefs"

        const val COMMON_MNEMONIC =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        const val TEST_HEX_PRIVATE_KEY =
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
        const val TON_SECRET_PUBLIC =
            "T+pQX93XzMGdpvV1/zyyM/zO9zV/gl0KiMaoK8Aroms=:31bbe51bc00c50f22357096ac7a9e6961cbff8b474296b4e936f82868d128001"
        const val SOLANA_KEYPAIR_BASE58 =
            "1GMkH3brNXiNNs1tiFZHu4yZSRrzJwxi5wB9bHFtMikjwpAW9DMZzU2Pqakc5it8X3N5vPmqdN7KF4CCUpmKhq"
        const val SUI_KEYPAIR =
            "suiprivkey1qq4a47vvrn0e96nntt2x0yx5d699szagl9rtq42p3ed45l75f7j0688pgwe"
    }
}
