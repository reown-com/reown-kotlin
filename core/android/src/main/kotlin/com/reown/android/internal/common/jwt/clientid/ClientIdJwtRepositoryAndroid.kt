@file:JvmSynthetic

package com.reown.android.internal.common.jwt.clientid

import com.reown.android.internal.common.exception.CannotFindKeyPairException
import com.reown.android.internal.common.storage.key_chain.KeyStore
import com.reown.foundation.common.model.PrivateKey
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.crypto.data.repository.BaseClientIdJwtRepository
import com.reown.foundation.crypto.data.repository.BaseClientIdJwtRepository.Companion.CLIENT_ID_KEYPAIR_TAG
import com.reown.util.bytesToHex
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom

//TODO: Remove
internal class ClientIdJwtRepositoryAndroid(private val keyChain: KeyStore) : BaseClientIdJwtRepository() {

    override fun setKeyPair(key: String, privateKey: PrivateKey, publicKey: PublicKey) {
        keyChain.setKeys(CLIENT_ID_KEYPAIR_TAG, privateKey, publicKey)
    }

    override fun getKeyPair(): Pair<String, String> {
        return if (doesKeyPairExist()) {
            val (privateKey, publicKey) = keyChain.getKeys(CLIENT_ID_KEYPAIR_TAG)
                ?: throw CannotFindKeyPairException("No key pair for given tag: $CLIENT_ID_KEYPAIR_TAG")

            publicKey to privateKey
        } else {
            generateAndStoreClientIdKeyPair()
        }
    }

    private fun doesKeyPairExist(): Boolean {
        return keyChain.checkKeys(CLIENT_ID_KEYPAIR_TAG)
    }
}

internal class GetKeyPair(private val keyChain: KeyStore) {

    fun setKeyPair(key: String, privateKey: PrivateKey, publicKey: PublicKey) {
        keyChain.setKeys(KEYPAIR_TAG, privateKey, publicKey)
    }

    fun getKeyPair(): Pair<String, String> {
        return if (doesKeyPairExist()) {
            val (privateKey, publicKey) = keyChain.getKeys(KEYPAIR_TAG)
                ?: throw CannotFindKeyPairException("No key pair for given tag: $KEYPAIR_TAG")

            publicKey to privateKey
        } else {
            generateAndStoreKeyPair()
        }
    }

    private fun generateAndStoreKeyPair(): Pair<String, String> {
        val secureRandom = SecureRandom()
        val keyPair: AsymmetricCipherKeyPair = Ed25519KeyPairGenerator().run {
            this.init(Ed25519KeyGenerationParameters(secureRandom))
            this.generateKeyPair()
        }
        val publicKeyParameters = keyPair.public as Ed25519PublicKeyParameters
        val privateKeyParameters = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = PublicKey(publicKeyParameters.encoded.bytesToHex())
        val privateKey = PrivateKey(privateKeyParameters.encoded.bytesToHex())

        setKeyPair(KEYPAIR_TAG, privateKey, publicKey)

        return publicKey.keyAsHex to privateKey.keyAsHex
    }

    private fun doesKeyPairExist(): Boolean {
        return keyChain.checkKeys(KEYPAIR_TAG)
    }

    companion object {
        const val KEYPAIR_TAG = "key_keypair_rust"
    }
}