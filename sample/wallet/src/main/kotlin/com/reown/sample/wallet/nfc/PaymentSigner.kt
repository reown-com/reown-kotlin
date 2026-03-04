@file:JvmSynthetic

package com.reown.sample.wallet.nfc

import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.signer.EthSigner
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import com.reown.walletkit.client.Wallet
import org.json.JSONArray
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder

/**
 * Shared signing logic for payment RPC actions.
 * Used by PaymentViewModel for the payment flow.
 */
internal object PaymentSigner {

    fun signWalletRpcAction(action: Wallet.Model.WalletRpcAction): String {
        return when (action.method) {
            "eth_signTypedData_v4" -> signTypedDataV4(action.params)
            "personal_sign" -> EthSigner.personalSign(action.params)
            else -> throw UnsupportedOperationException("Unsupported signing method: ${action.method}")
        }
    }

    fun signTypedDataV4(params: String): String {
        val paramsArray = JSONArray(params)
        val requestedAddress = paramsArray.getString(0)
        val typedData = paramsArray.getString(1)

        if (!requestedAddress.equals(EthAccountDelegate.address, ignoreCase = true)) {
            throw IllegalArgumentException("Requested address does not match wallet address")
        }

        val encoder = StructuredDataEncoder(typedData)
        val hash = encoder.hashStructuredData()

        val keyPair = ECKeyPair.create(EthAccountDelegate.privateKey.hexToBytes())
        val signatureData = Sign.signMessage(hash, keyPair, false)

        val rHex = signatureData.r.bytesToHex()
        val sHex = signatureData.s.bytesToHex()
        val v = signatureData.v[0].toInt() and 0xff
        val vHex = v.toString(16).padStart(2, '0')

        return "0x$rHex$sHex$vHex".lowercase()
    }
}
