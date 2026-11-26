package com.reown.android.internal.common.signing

import com.reown.android.internal.common.signing.eip1271.EIP1271Verifier
import com.reown.android.internal.common.signing.signature.Signature
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Runs without the network: the RPC is stubbed so these tests cover the eth_call we build and how we read
// the answer. The live on-chain check is CacaoTest.verifyEIP1271Success (instrumented).
class EIP1271VerifierTest {
    private val address = "0x2faf83c542b68f1b4cdc0e770e8cb9f567b08f71"
    private val projectId = "test-project-id"
    private val message = """localhost wants you to sign in with your Ethereum account:
0x2faf83c542b68f1b4cdc0e770e8cb9f567b08f71

URI: http://localhost:3000/
Version: 1
Chain ID: 1
Nonce: 1665443015700
Issued At: 2022-10-10T23:03:35.700Z
Expiration Time: 2022-10-11T23:03:35.700Z"""
    private val signatureHex =
        "c1505719b2504095116db01baaf276361efd3a73c28cf8cc28dabefa945b8d536011289ac0a3b048600c1e692ff173ca944246cf7ceb319ac2262d27b395c82b1c"
    private val signature = Signature.fromString(signatureHex)

    // EIP-191 hash of `message`, computed independently of the SDK.
    private val messageHash = "3aaa8393796c7388e4e062861d8238503de7584c977676fe9d8d551c30e11f84"
    private val expectedCallData = "0x1626ba7e" + messageHash +
            "0000000000000000000000000000000000000000000000000000000000000040" +
            "0000000000000000000000000000000000000000000000000000000000000041" +
            signatureHex

    private val magicValueResult = "0x1626ba7e00000000000000000000000000000000000000000000000000000000"

    private class StubRpc(private val answer: (id: Long) -> String) : Interceptor {
        var request: Request? = null
        var body: JSONObject? = null

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val buffer = Buffer().also { request.body!!.writeTo(it) }
            val body = JSONObject(buffer.readUtf8())
            this.request = request
            this.body = body
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(answer(body.getLong("id")).toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private fun clientWith(stub: StubRpc) = OkHttpClient.Builder().addInterceptor(stub).build()

    private fun result(id: Long, result: String) = """{"jsonrpc":"2.0","id":$id,"result":"$result"}"""

    @Test
    fun `should send isValidSignature eth_call to the contract`() {
        val stub = StubRpc { id -> result(id, magicValueResult) }

        EIP1271Verifier.verify(signature, message, address, projectId, clientWith(stub))

        val request = stub.request!!
        assertEquals("rpc.walletconnect.com", request.url.host)
        assertEquals("eip155:1", request.url.queryParameter("chainId"))
        assertEquals(projectId, request.url.queryParameter("projectId"))

        val body = stub.body!!
        assertEquals("eth_call", body.getString("method"))
        val params = body.getJSONArray("params")
        val call = params.getJSONObject(0)
        assertEquals(address, call.getString("to"))
        assertEquals(expectedCallData, call.getString("data"))
        assertEquals("latest", params.getString(1))
    }

    @Test
    fun `should return true when the contract answers with the magic value`() {
        val stub = StubRpc { id -> result(id, magicValueResult) }

        assertTrue(EIP1271Verifier.verify(signature, message, address, projectId, clientWith(stub)))
    }

    @Test
    fun `should return false when the contract answers with another value`() {
        val stub = StubRpc { id -> result(id, "0xffffffff00000000000000000000000000000000000000000000000000000000") }

        assertFalse(EIP1271Verifier.verify(signature, message, address, projectId, clientWith(stub)))
    }

    @Test
    fun `should return false when the contract reverts`() {
        val stub = StubRpc { id ->
            """{"jsonrpc":"2.0","id":$id,"error":{"code":3,"message":"execution reverted: TM: Invalid signer"}}"""
        }

        assertFalse(EIP1271Verifier.verify(signature, message, address, projectId, clientWith(stub)))
    }

    @Test
    fun `should hash a hex message the same way as its plaintext`() {
        val stub = StubRpc { id -> result(id, magicValueResult) }
        val hexMessage = "0x" + message.toByteArray().joinToString("") { "%02x".format(it) }

        assertTrue(EIP1271Verifier.verifyHex(signature, hexMessage, address, projectId, clientWith(stub)))
        assertEquals(expectedCallData, stub.body!!.getJSONArray("params").getJSONObject(0).getString("data"))
    }
}
