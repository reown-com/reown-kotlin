package com.reown.sign.engine.use_case.utils

import com.reown.sign.engine.model.EngineDO
import org.junit.Assert.*
import org.junit.Test

class NamespaceMergerTest {

    // Test data constants
    companion object {
        private const val ETH_CHAIN = "eip155:1"
        private const val POLY_CHAIN = "eip155:137"
        private const val COSMOS_CHAIN = "cosmos:cosmoshub-4"
        private const val SOLANA_CHAIN = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
    }

    // MARK: - Basic merging tests

    @Test
    fun `test merge empty required into empty optional`() {
        val requiredNamespaces: Map<String, EngineDO.Namespace.Proposal> = emptyMap()
        val optionalNamespaces: Map<String, EngineDO.Namespace.Proposal> = emptyMap()

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertTrue("Result should be null when both inputs are empty", result == null)
    }

    @Test
    fun `test merge required into empty optional`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign", "personal_sign"),
                events = listOf("chainChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, null)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)
        assertNotNull("Should contain eip155 namespace", result!!["eip155"])

        val eip155Namespace = result["eip155"]!!
        assertEquals("Chains should match", listOf(ETH_CHAIN), eip155Namespace.chains)
        assertEquals("Methods should match", listOf("eth_sign", "personal_sign"), eip155Namespace.methods)
        assertEquals("Events should match", listOf("chainChanged"), eip155Namespace.events)
    }

    @Test
    fun `test merge empty required into existing optional`() {
        val requiredNamespaces: Map<String, EngineDO.Namespace.Proposal> = emptyMap()
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)
        assertNotNull("Should contain eip155 namespace", result!!["eip155"])

        val eip155Namespace = result["eip155"]!!
        assertEquals("Chains should match", listOf(ETH_CHAIN), eip155Namespace.chains)
        assertEquals("Methods should match", listOf("eth_sign"), eip155Namespace.methods)
        assertEquals("Events should match", listOf("chainChanged"), eip155Namespace.events)
    }

    // MARK: - Merging different namespaces

    @Test
    fun `test merge different namespaces`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )
        val optionalNamespaces = mapOf(
            "cosmos" to EngineDO.Namespace.Proposal(
                chains = listOf(COSMOS_CHAIN),
                methods = listOf("cosmos_signDirect"),
                events = listOf("someEvent")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have two namespaces", 2, result?.size)
        assertNotNull("Should contain eip155 namespace", result!!["eip155"])
        assertNotNull("Should contain cosmos namespace", result["cosmos"])

        val eip155Namespace = result["eip155"]!!
        assertEquals("eip155 chains should match", listOf(ETH_CHAIN), eip155Namespace.chains)
        assertEquals("eip155 methods should match", listOf("eth_sign"), eip155Namespace.methods)
        assertEquals("eip155 events should match", listOf("chainChanged"), eip155Namespace.events)

        val cosmosNamespace = result["cosmos"]!!
        assertEquals("cosmos chains should match", listOf(COSMOS_CHAIN), cosmosNamespace.chains)
        assertEquals("cosmos methods should match", listOf("cosmos_signDirect"), cosmosNamespace.methods)
        assertEquals("cosmos events should match", listOf("someEvent"), cosmosNamespace.events)
    }

    // MARK: - Merging same namespace with different content

    @Test
    fun `test merge same namespace with different chains`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(POLY_CHAIN),
                methods = listOf("personal_sign"),
                events = listOf("accountsChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)
        assertNotNull("Should contain eip155 namespace", result!!["eip155"])

        val eip155Namespace = result["eip155"]!!
        assertEquals("Chains should be merged", listOf(ETH_CHAIN, POLY_CHAIN), eip155Namespace.chains)
        assertEquals("Methods should be merged", listOf("eth_sign", "personal_sign"), eip155Namespace.methods)
        assertEquals("Events should be merged", listOf("chainChanged", "accountsChanged"), eip155Namespace.events)
    }

    @Test
    fun `test merge both with empty chains`() {
        val requiredNamespaces = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )
        val optionalNamespaces = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                methods = listOf("personal_sign"),
                events = listOf("accountsChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)
        assertNotNull("Should contain eip155:1 namespace", result!!["eip155:1"])

        val eip155Namespace = result["eip155:1"]!!
        assertTrue("Chains should be empty", eip155Namespace.chains!!.isEmpty())
        assertEquals("Methods should be merged", listOf("eth_sign", "personal_sign"), eip155Namespace.methods)
        assertEquals("Events should be merged", listOf("chainChanged", "accountsChanged"), eip155Namespace.events)
    }

    // MARK: - Complex merging scenarios

    @Test
    fun `test merge multiple namespaces`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            ),
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf(SOLANA_CHAIN),
                methods = listOf("solana_signMessage"),
                events = emptyList()
            )
        )
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(POLY_CHAIN),
                methods = listOf("personal_sign"),
                events = listOf("accountsChanged")
            ),
            "cosmos" to EngineDO.Namespace.Proposal(
                chains = listOf(COSMOS_CHAIN),
                methods = listOf("cosmos_signDirect"),
                events = listOf("someEvent")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have three namespaces", 3, result?.size)

        // Check eip155 (merged)
        val eip155Namespace = result!!["eip155"]!!
        assertEquals("eip155 chains should be merged", listOf(ETH_CHAIN, POLY_CHAIN), eip155Namespace.chains)
        assertEquals("eip155 methods should be merged", listOf("eth_sign", "personal_sign"), eip155Namespace.methods)
        assertEquals("eip155 events should be merged", listOf("chainChanged", "accountsChanged"), eip155Namespace.events)

        // Check solana (from required only)
        val solanaNamespace = result["solana"]!!
        assertEquals("solana chains should match", listOf(SOLANA_CHAIN), solanaNamespace.chains)
        assertEquals("solana methods should match", listOf("solana_signMessage"), solanaNamespace.methods)
        assertTrue("solana events should match", solanaNamespace.events.isEmpty())

        // Check cosmos (from optional only)
        val cosmosNamespace = result["cosmos"]!!
        assertEquals("cosmos chains should match", listOf(COSMOS_CHAIN), cosmosNamespace.chains)
        assertEquals("cosmos methods should match", listOf("cosmos_signDirect"), cosmosNamespace.methods)
        assertEquals("cosmos events should match", listOf("someEvent"), cosmosNamespace.events)
    }

    // MARK: - Edge cases

    @Test
    fun `test merge with duplicate methods and events`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign", "personal_sign"),
                events = listOf("chainChanged", "accountsChanged")
            )
        )
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(POLY_CHAIN),
                methods = listOf("personal_sign", "eth_signTypedData"), // personal_sign is duplicate
                events = listOf("accountsChanged", "someEvent") // accountsChanged is duplicate
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)
        assertNotNull("Should contain eip155 namespace", result!!["eip155"])

        val eip155Namespace = result["eip155"]!!
        assertEquals("Chains should be merged", listOf(ETH_CHAIN, POLY_CHAIN), eip155Namespace.chains)
        assertEquals("Methods should be merged without duplicates", listOf("eth_sign", "personal_sign", "eth_signTypedData"), eip155Namespace.methods)
        assertEquals("Events should be merged without duplicates", listOf("chainChanged", "accountsChanged", "someEvent"), eip155Namespace.events)
    }

    // MARK: - Additional edge cases

    @Test
    fun `test merge with null required namespaces`() {
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )

        val result = NamespaceMerger.merge(null, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Result should be the same as optional namespaces", optionalNamespaces, result)
    }

    @Test
    fun `test merge with null optional namespaces`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, null)

        assertNotNull(result)
        assertEquals("Result should be the same as required namespaces", requiredNamespaces, result)
    }

    @Test
    fun `test merge with both null`() {
        val result = NamespaceMerger.merge(null, null)

        assertNull("Result should be null when both inputs are null", result)
    }

    @Test
    fun `test merge with empty chains in both`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                methods = listOf("eth_sign"),
                events = listOf("chainChanged")
            )
        )
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                methods = listOf("personal_sign"),
                events = listOf("accountsChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)

        val eip155Namespace = result?.get("eip155")!!
        assertTrue("Chains should be empty", eip155Namespace.chains!!.isEmpty())
        assertEquals("Methods should be merged", listOf("eth_sign", "personal_sign"), eip155Namespace.methods)
        assertEquals("Events should be merged", listOf("chainChanged", "accountsChanged"), eip155Namespace.events)
    }

    @Test
    fun `test merge with empty methods and events`() {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(ETH_CHAIN),
                methods = emptyList(),
                events = emptyList()
            )
        )
        val optionalNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf(POLY_CHAIN),
                methods = listOf("personal_sign"),
                events = listOf("accountsChanged")
            )
        )

        val result = NamespaceMerger.merge(requiredNamespaces, optionalNamespaces)

        assertNotNull(result)
        assertEquals("Should have one namespace", 1, result?.size)

        val eip155Namespace = result!!["eip155"]!!
        assertEquals("Chains should be merged", listOf(ETH_CHAIN, POLY_CHAIN), eip155Namespace.chains)
        assertEquals("Methods should be merged", listOf("personal_sign"), eip155Namespace.methods)
        assertEquals("Events should be merged", listOf("accountsChanged"), eip155Namespace.events)
    }

    // MARK: - JavaScript test cases

    @Test
    fun `should merge required and optional namespaces case 1`() {
        val required = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                events = listOf("chainChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            )
        )
        val optional = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                events = listOf("accountsChanged"),
                methods = listOf("eth_sendTransaction")
            )
        )
        val expected = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                events = listOf("chainChanged", "accountsChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            )
        )

        val result = NamespaceMerger.merge(required, optional)

        assertNotNull(result)
        assertEquals("Should have expected number of namespaces", expected.size, result?.size)

        val resultEip155 = result!!["eip155"]
        val expectedEip155 = expected["eip155"]

        assertEquals("Chains should match", expectedEip155?.chains, resultEip155?.chains)
        assertEquals("Events should match", expectedEip155?.events, resultEip155?.events )
        assertEquals("Methods should match", expectedEip155?.methods, resultEip155?.methods)
    }

    @Test
    fun `should merge required and optional namespaces case 2`() {
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                events = listOf("chainChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            )
        )
        val optional = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                events = listOf("accountsChanged"),
                methods = listOf("eth_sendTransaction")
            )
        )
        val expected = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                events = listOf("chainChanged", "accountsChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            )
        )

        val result = NamespaceMerger.merge(required, optional)

        assertNotNull(result)
        assertEquals("Should have expected number of namespaces", expected.size, result?.size)

        val resultEip155 = result!!["eip155"]
        val expectedEip155 = expected["eip155"]
        assertNotNull("Should contain eip155 namespace", resultEip155)
        assertNotNull("Expected should contain eip155 namespace", expectedEip155)

        assertEquals("Chains should match", expectedEip155!!.chains, resultEip155!!.chains)
        assertEquals("Events should match", expectedEip155.events, resultEip155.events)
        assertEquals("Methods should match", expectedEip155.methods, resultEip155.methods)
    }

    @Test
    fun `should merge required and optional namespaces case 3`() {
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                events = listOf("chainChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            )
        )
        val optional = mapOf(
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf("solana:1"),
                events = listOf("accountsChanged"),
                methods = listOf("solana_signTransaction")
            )
        )
        val expected = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                events = listOf("chainChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            ),
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf("solana:1"),
                events = listOf("accountsChanged"),
                methods = listOf("solana_signTransaction")
            )
        )

        val result = NamespaceMerger.merge(required, optional)

        assertNotNull(result)
        assertEquals("Should have expected number of namespaces", expected.size, result?.size)

        // Check eip155 namespace
        val resultEip155 = result?.get("eip155")
        val expectedEip155 = expected["eip155"]
        assertNotNull("Should contain eip155 namespace", resultEip155)
        assertNotNull("Expected should contain eip155 namespace", expectedEip155)

        assertEquals("eip155 chains should match", expectedEip155!!.chains, resultEip155!!.chains)
        assertEquals("eip155 events should match", expectedEip155.events, resultEip155.events)
        assertEquals("eip155 methods should match", expectedEip155.methods, resultEip155.methods)

        // Check solana namespace
        val resultSolana = result["solana"]
        val expectedSolana = expected["solana"]
        assertNotNull("Should contain solana namespace", resultSolana)
        assertNotNull("Expected should contain solana namespace", expectedSolana)

        assertEquals("solana chains should match", expectedSolana!!.chains, resultSolana!!.chains)
        assertEquals("solana events should match", expectedSolana.events, resultSolana.events)
        assertEquals("solana methods should match", expectedSolana.methods, resultSolana.methods)
    }

    @Test
    fun `should merge required and optional namespaces case 4`() {
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:2"),
                events = listOf("chainChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction")
            ),
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf("solana:1"),
                events = listOf("accountsChanged"),
                methods = listOf("solana_signTransaction")
            )
        )
        val optional = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                events = listOf("accountsChanged"),
                methods = listOf("eth_signTypedData")
            )
        )
        val expected = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:2", "eip155:1"),
                events = listOf("chainChanged", "accountsChanged"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTypedData")
            ),
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf("solana:1"),
                events = listOf("accountsChanged"),
                methods = listOf("solana_signTransaction")
            )
        )

        val result = NamespaceMerger.merge(required, optional)

        assertNotNull(result)
        assertEquals("Should have expected number of namespaces", expected.size, result?.size)

        // Check eip155 namespace
        val resultEip155 = result!!["eip155"]
        val expectedEip155 = expected["eip155"]
        assertNotNull("Should contain eip155 namespace", resultEip155)
        assertNotNull("Expected should contain eip155 namespace", expectedEip155)

        assertEquals("eip155 chains should match", expectedEip155!!.chains, resultEip155!!.chains)
        assertEquals("eip155 events should match", expectedEip155.events, resultEip155.events)
        assertEquals("eip155 methods should match", expectedEip155.methods, resultEip155.methods)

        // Check solana namespace
        val resultSolana = result["solana"]
        val expectedSolana = expected["solana"]
        assertNotNull("Should contain solana namespace", resultSolana)
        assertNotNull("Expected should contain solana namespace", expectedSolana)

        assertEquals("solana chains should match", expectedSolana!!.chains, resultSolana!!.chains)
        assertEquals("solana events should match", expectedSolana.events, resultSolana.events)
        assertEquals("solana methods should match", expectedSolana.methods, resultSolana.methods)
    }
} 