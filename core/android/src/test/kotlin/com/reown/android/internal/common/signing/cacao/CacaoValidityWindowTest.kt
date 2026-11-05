package com.reown.android.internal.common.signing.cacao

import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.utils.cacao.CacaoSignerInterface
import com.reown.android.utils.cacao.sign
import com.reown.util.hexToBytes
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CacaoValidityWindowTest {
    private val cacaoSigner = object : CacaoSignerInterface<Cacao.Signature> {}
    private val privateKey = "305c6cde3846927892cd32762f6120539f3ec74c9e3a16b9b798b1e85351ae2a".hexToBytes()
    private val now = 1_700_000_000L

    @Test
    fun `absent exp and nbf stay valid`() {
        assertTrue(payload().isWithinValidityWindow(now))
    }

    @Test
    fun `future exp stays valid`() {
        assertTrue(payload(exp = "2024-01-01T00:00:00Z").isWithinValidityWindow(now))
    }

    @Test
    fun `past exp is expired`() {
        assertFalse(payload(exp = "2023-01-01T00:00:00Z").isWithinValidityWindow(now))
    }

    @Test
    fun `exp equal to now is expired`() {
        assertFalse(payload(exp = "2023-11-14T22:13:20Z").isWithinValidityWindow(now))
    }

    @Test
    fun `past nbf stays valid`() {
        assertTrue(payload(nbf = "2023-01-01T00:00:00Z").isWithinValidityWindow(now))
    }

    @Test
    fun `future nbf is not yet valid`() {
        assertFalse(payload(nbf = "2024-01-01T00:00:00Z").isWithinValidityWindow(now))
    }

    @Test
    fun `unparseable exp fails closed`() {
        assertFalse(payload(exp = "not-a-timestamp").isWithinValidityWindow(now))
    }

    @Test
    fun `unparseable nbf fails closed`() {
        assertFalse(payload(nbf = "not-a-timestamp").isWithinValidityWindow(now))
    }

    @Test
    fun `verify rejects a signed cacao after exp`() {
        val cacao = signedCacao(exp = "2020-01-01T00:00:00Z")
        assertFalse(CacaoVerifier(ProjectId("")).verify(cacao))
    }

    @Test
    fun `verify rejects a signed cacao before nbf`() {
        val cacao = signedCacao(nbf = "2099-01-01T00:00:00Z")
        assertFalse(CacaoVerifier(ProjectId("")).verify(cacao))
    }

    @Test
    fun `verify rejects a signed cacao with unparseable exp`() {
        val cacao = signedCacao(exp = "not-a-timestamp")
        assertFalse(CacaoVerifier(ProjectId("")).verify(cacao))
    }

    @Test
    fun `verify still accepts a currently valid signed cacao`() {
        val cacao = signedCacao(exp = "2099-01-01T00:00:00Z", nbf = "2020-01-01T00:00:00Z")
        assertTrue(CacaoVerifier(ProjectId("")).verify(cacao))
    }

    private fun payload(exp: String? = null, nbf: String? = null): Cacao.Payload = Cacao.Payload(
        iss = "did:pkh:eip155:1:0x15bca56b6e2728aec2532df9d436bd1600e86688",
        domain = "service.invalid",
        aud = "https://service.invalid/login",
        version = "1",
        nonce = "32891756",
        iat = "2021-09-30T16:25:24Z",
        nbf = nbf,
        exp = exp,
        statement = "I accept the ServiceOrg Terms of Service: https://service.invalid/tos",
        requestId = null,
        resources = listOf(
            "ipfs://bafybeiemxf5abjwjbikoz4mc3a3dla6ual3jsgpdr4cjr3oz3evfyavhwq/",
            "https://example.com/my-web2-claim.json",
        ),
    )

    private fun signedCacao(exp: String? = null, nbf: String? = null): Cacao {
        val signedPayload = payload(exp = exp, nbf = nbf)
        val message = signedPayload.toCAIP222Message()
        val signature = cacaoSigner.sign(message, privateKey, SignatureType.EIP191)
        return Cacao(CacaoType.EIP4361.toHeader(), signedPayload, signature)
    }
}
