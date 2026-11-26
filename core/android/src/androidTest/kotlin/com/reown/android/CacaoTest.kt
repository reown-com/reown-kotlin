package com.reown.android

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.signing.cacao.Cacao
import com.reown.android.internal.common.signing.cacao.CacaoType
import com.reown.android.internal.common.signing.cacao.CacaoVerifier
import com.reown.android.internal.common.signing.cacao.toCAIP222Message
import com.reown.android.internal.common.signing.signature.Signature
import com.reown.android.internal.common.signing.signature.verify
import com.reown.android.utils.cacao.CacaoSignerInterface
import com.reown.android.utils.cacao.sign
import com.reown.util.hexToBytes
import org.junit.Assert
import org.junit.Test

internal class CacaoTest {
    private val cacaoVerifier = CacaoVerifier(ProjectId(BuildConfig.PROJECT_ID))
    private val cacaoSigner = object : CacaoSignerInterface<Cacao.Signature> {}
    private val chainName = "Ethereum"
    private val payload = Cacao.Payload(
        iss = "did:pkh:eip155:1:0x93fdeCDCb4A9c3A8ce406f8b21a95fFa1cB6C010",
        domain = "appkit-lab.reown.com",
        aud = "https://appkit-lab.reown.com",
        version = "1",
        nonce = "f2bb9aa60453c71cdb181d144dc81872bfd0ff743724268ce916aa5c5780a598",
        iat = "2025-09-12T13:30:29.565Z",
        nbf = null,
        exp = null,
        statement = "Please sign with your account I further authorize the stated URI to perform the following actions on my behalf: (1) 'request': 'eth_sign', 'eth_signTypedData', 'eth_signTypedData_v4', 'personal_sign' for 'eip155'.",
        requestId = null,
        resources = listOf("urn:recap:eyJhdHQiOnsiZWlwMTU1Ijp7InJlcXVlc3QvZXRoX3NpZ24iOlt7ImNoYWlucyI6WyJlaXAxNTU6MSIsImVpcDE1NToxMzciXX1dLCJyZXF1ZXN0L2V0aF9zaWduVHlwZWREYXRhIjpbeyJjaGFpbnMiOlsiZWlwMTU1OjEiLCJlaXAxNTU6MTM3Il19XSwicmVxdWVzdC9ldGhfc2lnblR5cGVkRGF0YV92NCI6W3siY2hhaW5zIjpbImVpcDE1NToxIiwiZWlwMTU1OjEzNyJdfV0sInJlcXVlc3QvcGVyc29uYWxfc2lnbiI6W3siY2hhaW5zIjpbImVpcDE1NToxIiwiZWlwMTU1OjEzNyJdfV19fX0")
    )

    private val privateKey = "27d47c1b45ed34f6f531719dc9d2e75d89e41fce50d8f6ac0ead57860ef35270".hexToBytes()

    @Test
    fun signAndVerifyWithEIP191Test() {
        val message = payload.toCAIP222Message(chainName)
        val signature: Cacao.Signature = cacaoSigner.sign(message, privateKey, SignatureType.EIP191)
        val cacao = Cacao(CacaoType.CAIP222.toHeader(), payload, signature)
        val result: Boolean = cacaoVerifier.verify(cacao)
        Assert.assertTrue("0x962fc9b8fbe359668dc92a3aceaf3483e17a606adc13f51c778e33a2341f932952d8e226c40564a4281b2c265bc52ebd4021c10d13fd7ad2781c45d7ec975f5d1c" == signature.s)
        Assert.assertTrue(result)
    }

    // Live eth_call against mainnet, with the same smart account fixture as the Swift and JS SDKs.
    // The exact signed message is used rather than one rebuilt from a Cacao.Payload: with a null statement
    // toCAIP222Message() puts an extra blank line before "URI:", so the rebuilt message would not match the signature.
    @Test
    fun verifyEIP1271Success() {
        val address = "0x2faf83c542b68f1b4cdc0e770e8cb9f567b08f71"
        val message = """localhost wants you to sign in with your Ethereum account:
0x2faf83c542b68f1b4cdc0e770e8cb9f567b08f71

URI: http://localhost:3000/
Version: 1
Chain ID: 1
Nonce: 1665443015700
Issued At: 2022-10-10T23:03:35.700Z
Expiration Time: 2022-10-11T23:03:35.700Z"""
        val signature = Signature.fromString(
            "0xc1505719b2504095116db01baaf276361efd3a73c28cf8cc28dabefa945b8d536011289ac0a3b048600c1e692ff173ca944246cf7ceb319ac2262d27b395c82b1c"
        )

        val result: Boolean = signature.verify(message, address, SignatureType.EIP1271.header, ProjectId(BuildConfig.PROJECT_ID))
        Assert.assertTrue(result)
    }

    @Test
    fun verifyEIP1271Failure() {
        val iss = "did:pkh:eip155:1:0x2faf83c542b68f1b4cdc0e770e8cb9f567b08f71"
        val payload = Cacao.Payload(
            iss = iss,
            domain = "localhost",
            aud = "http://localhost:3000/",
            version = "1",
            nonce = "1665443015700",
            iat = "2022-10-10T23:03:35.700Z",
            nbf = null,
            exp = "2022-10-11T23:03:35.700Z",
            statement = null,
            requestId = null,
            resources = null
        )

        val signatureString =
            "0xdeaddeaddead4095116db01baaf276361efd3a73c28cf8cc28dabefa945b8d536011289ac0a3b048600c1e692ff173ca944246cf7ceb319ac2262d27b395c82b1c"
        val signature: Cacao.Signature = Cacao.Signature(SignatureType.EIP1271.header, signatureString, payload.toCAIP222Message())
        val cacao = Cacao(CacaoType.EIP4361.toHeader(), payload, signature)
        val result: Boolean = cacaoVerifier.verify(cacao)
        Assert.assertFalse(result)
    }
}