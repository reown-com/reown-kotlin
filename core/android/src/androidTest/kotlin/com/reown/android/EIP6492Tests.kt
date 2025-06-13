package com.reown.android

import com.reown.android.internal.common.signing.eip6492.EIP6492Verifier
import org.junit.Assert.assertTrue
import org.junit.Test

class EIP6492Tests {

    @Test
    fun verifySignatureTest() {
        // Test data
        val safeChainId = "eip155:11155111"
        val chainId = "eip155:10"
        val projectId = "18f69a420fce7f005dc43f74e6404be7"
        val safeSignature = "0x6037120d3995a626caa8dac775220ef5c91ece120a8ffa599bf28dbd1c40f1e1398cb941dd9fc4e880b988f279c603cedf7fe0609aa2b9cad829861d9798803b1be2a4308f50367f32a84baa7965102c87ab22db69a70ba3d3717f951e07007fea5901c524425171f8ff9143b64fe3223155a26ee0aab54b03058ab99187bc5ad71c"
        val safeAddress = "0x9a1148b5D6a2D34CA46111379d0FD1352a0ade4a"
        val message = "sample.kotlin.modal wants you to sign in with your Ethereum account:\n" +
                "0x8fb8125215d6f988c5908dbcfa5428c85c302de1\n" +
                "\n" +
                "I accept the Terms of Service: https://yourDappDomain.com/\n" +
                "\n" +
                "URI: https://web3inbox.com/all-apps\n" +
                "Version: 1\n" +
                "Chain ID: 10\n" +
                "Nonce: 0a13a0d1657f3d79d165d5eb\n" +
                "Issued At: 2025-06-12T15:26:36+02:00\n"

        val messageHash = "0xb48c43838346726a55fe0023cd2fc14b26144b6a9d36284a436b62e934bf382d"

        val signature = "0x3e2d111c8c52a5ef0ba64fe4d85e32a5153032367ec44aaae0a4e2d1bfb9bebd"
        val address = "0x8fb8125215d6f988c5908dbcfa5428c85c302de1"
        // Initialize EIP6492Verifier
        EIP6492Verifier.init(chainId, projectId)

        // Verify the signature
        val isValid = EIP6492Verifier.verify6492(
            originalMessage = "test message",
            address = address,
            signature = signature
        )

        // Assert that the signature is valid
        assertTrue("EIP6492 signature verification should succeed", isValid)
    }
}