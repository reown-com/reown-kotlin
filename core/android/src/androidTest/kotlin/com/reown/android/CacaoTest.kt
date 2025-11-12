package com.reown.android

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.signing.cacao.Cacao
import com.reown.android.internal.common.signing.cacao.CacaoType
import com.reown.android.internal.common.signing.cacao.CacaoVerifier
import com.reown.android.internal.common.signing.cacao.toCAIP222Message
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

    @Test
//    @Ignore("Test failing in pipeline")
    fun verifyEIP1271Success() {
        val iss = "did:pkh:eip155:1:0x6DF3d14554742D67068BB7294C80107a3c655A56"
        val payload = Cacao.Payload(
            iss = iss,
            domain = "etherscan.io",
            aud = "https://etherscan.io/verifiedSignatures#",
            version = "1",
            nonce = "DTYxeNr95Ne7Sape5",
            iat = "2024-02-05T13:09:08.427Z",
            nbf = null,
            exp = null,
            statement = "Sign message to verify ownership of the address 0x6DF3d14554742D67068BB7294C80107a3c655A56 on etherscan.io",
            requestId = null,
            resources = null
        )

        val signatureString =
            "0xb518b65724f224f8b12dedeeb06f8b278eb7d3b42524959bed5d0dfa49801bd776c7ee05de396eadc38ee693c917a04d93b20981d68c4a950cbc42ea7f4264bc1c"
        val signature: Cacao.Signature = Cacao.Signature(SignatureType.EIP1271.header, signatureString, payload.toCAIP222Message())
        val cacao = Cacao(CacaoType.EIP4361.toHeader(), payload, signature)
        val result: Boolean = cacaoVerifier.verify(cacao)
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