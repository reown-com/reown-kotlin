package com.reown.android.internal.common.signing.eip191

import com.reown.android.internal.common.signing.cacao.guaranteeNoHexPrefix
import com.reown.android.internal.common.signing.signature.Signature
import com.reown.android.internal.common.signing.signature.toSignatureData
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric.hexStringToByteArray
import java.security.SignatureException

internal object EIP191Verifier {
    fun verify(signature: Signature, originalMessage: ByteArray, address: String): Boolean {
        val addressFromSig = getAddressUsedToSignPrefixedMessage(signature.toSignatureData(), originalMessage)
        val userAddress = address.guaranteeNoHexPrefix()
        return addressFromSig.equals(userAddress, ignoreCase = true)
    }

    fun verify(signature: Signature, originalMessage: String, address: String): Boolean = verify(signature, originalMessage.toByteArray(), address)

    fun verifyNoPrefix(signature: Signature, originalMessage: ByteArray, address: String): Boolean =
        getAddressUsedToSignMessage(signature.toSignatureData(), originalMessage).equals(address.guaranteeNoHexPrefix(), ignoreCase = true)

    fun verifyNoPrefix(signature: Signature, originalMessage: String, address: String): Boolean = verifyNoPrefix(signature, originalMessage.toByteArray(), address)

    @Throws(SignatureException::class)
    private fun getAddressUsedToSignPrefixedMessage(signedHash: Sign.SignatureData, originalMessage: ByteArray): String {
        return Keys.getAddress(Sign.signedPrefixedMessageToKey(originalMessage, signedHash).toString(16))
    }

    @Throws(SignatureException::class)
    private fun getAddressUsedToSignMessage(signedHash: Sign.SignatureData, originalMessage: ByteArray): String {
        return Keys.getAddress(Sign.signedMessageToKey(originalMessage, signedHash).toString(16))
    }
}