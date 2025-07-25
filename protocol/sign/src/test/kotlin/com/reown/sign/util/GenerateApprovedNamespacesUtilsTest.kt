package com.reown.sign.util

import com.reown.android.internal.common.model.Namespace
import com.reown.sign.client.Sign
import com.reown.sign.client.utils.generateApprovedNamespaces
import com.reown.sign.client.utils.normalizeNamespaces
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GenerateApprovedNamespacesUtilsTest {

    @Test
    fun testNamespacesNormalizationFromChainIndexing() {
        val namespaces = mapOf(
            "eip155:1" to Namespace.Proposal(methods = listOf("method_1", "method_2"), events = listOf("event_1", "event_2")),
            "eip155:2" to Namespace.Proposal(methods = listOf("method_11", "method_22"), events = listOf("event_11", "event_22")),
        )

        val normalizedNamespaces = mapOf(
            "eip155" to Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("method_1", "method_2", "method_11", "method_22"),
                events = listOf("event_1", "event_2", "event_11", "event_22")
            )
        )

        val result = normalizeNamespaces(namespaces)
        assertEquals(normalizedNamespaces, result)
    }

    @Test
    fun testNamespacesNormalizationMixedApproach() {
        val namespaces = mapOf(
            "eip155:1" to Namespace.Proposal(methods = listOf("method_1", "method_2"), events = listOf("event_1", "event_2")),
            "eip155" to Namespace.Proposal(chains = listOf("eip155:2"), methods = listOf("method_11", "method_22"), events = listOf("event_11", "event_22")),
        )

        val normalizedNamespaces = mapOf(
            "eip155" to Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("method_1", "method_2", "method_11", "method_22"),
                events = listOf("event_1", "event_2", "event_11", "event_22")
            )
        )

        val result = normalizeNamespaces(namespaces)
        assertEquals(normalizedNamespaces, result)
    }

    @Test
    fun testNamespacesNormalizationWithNormalizedMap() {
        val namespaces = mapOf(
            "eip155" to Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("method_1", "method_2", "method_11", "method_22"),
                events = listOf("event_1", "event_2", "event_11", "event_22")
            ),
        )

        val normalizedNamespaces = mapOf(
            "eip155" to Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("method_1", "method_2", "method_11", "method_22"),
                events = listOf("event_1", "event_2", "event_11", "event_22")
            )
        )

        val result = normalizeNamespaces(namespaces)
        assertEquals(normalizedNamespaces, result)
    }

    /* All test cases and configs can be found: https://docs.google.com/spreadsheets/d/1uc7lLWvx7tjgq_iQYylHVLNcs4F5z7jsnq_2f7ouGM8/edit#gid=0 */

    @Test
    fun `supported namespaces contain extra chain with the same event as required namespaces`() {
        val required = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("wallet_switchEthereumChain", "wallet_addEthereumChain"),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect")
            )
        )

        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:5", "eip155:10", "eip155:56"),
                methods = listOf(
                    "wallet_switchEthereumChain",
                    "wallet_addEthereumChain",
                ),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect", "message")
            )
        )

        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf(
                    "eip155:100",
                    "eip155:250",
                    "eip155:1",
                ),
                accounts = listOf(
                    "eip155:100:0x63821D66425d3Baf7B72601785E03D14bDF48919",
                    "eip155:250:0x63821D66425d3Baf7B72601785E03D14bDF48919",
                    "eip155:1:0x63821D66425d3Baf7B72601785E03D14bDF48919",
                ),
                methods = listOf(
                    "wallet_switchEthereumChain",
                    "wallet_addEthereumChain"
                ),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect"),
            ),
            "solana" to Sign.Model.Namespace.Session(
                chains = listOf("solana:101"),
                accounts = listOf("solana:101:3PQbBtoeG8iMuYa5wdmNELXJyRo4L3SwJ3Lmi1TTc81C"),
                events = listOf("connect"),
                methods = listOf("solana_rawSendTransaction", "signAndSendTransaction", "signTransaction", "signMessage")
            )
        )

        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("wallet_switchEthereumChain", "wallet_addEthereumChain"),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect"),
                accounts = listOf("eip155:1:0x63821D66425d3Baf7B72601785E03D14bDF48919")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `supported namespaces contain extra chain with the same method as required namespaces`() {
        val required = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("wallet_switchEthereumChain", "wallet_addEthereumChain", "sign"),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect")
            )
        )

        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:5", "eip155:10", "eip155:56"),
                methods = listOf(
                    "wallet_switchEthereumChain",
                    "wallet_addEthereumChain",
                ),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect", "message")
            )
        )

        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf(
                    "eip155:100",
                    "eip155:250",
                    "eip155:1",
                ),
                accounts = listOf(
                    "eip155:100:0x63821D66425d3Baf7B72601785E03D14bDF48919",
                    "eip155:250:0x63821D66425d3Baf7B72601785E03D14bDF48919",
                    "eip155:1:0x63821D66425d3Baf7B72601785E03D14bDF48919",
                ),
                methods = listOf(
                    "wallet_switchEthereumChain",
                    "wallet_addEthereumChain",
                    "sign"
                ),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect"),
            ),
            "solana" to Sign.Model.Namespace.Session(
                chains = listOf("solana:101"),
                accounts = listOf("solana:101:3PQbBtoeG8iMuYa5wdmNELXJyRo4L3SwJ3Lmi1TTc81C"),
                events = listOf("connect_test"),
                methods = listOf("solana_rawSendTransaction", "signAndSendTransaction", "signTransaction", "signMessage", "sign")
            )
        )

        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("wallet_switchEthereumChain", "wallet_addEthereumChain", "sign"),
                events = listOf("connect", "chainChanged", "accountsChanged", "disconnect"),
                accounts = listOf("eip155:1:0x63821D66425d3Baf7B72601785E03D14bDF48919")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `test if optional namespaces are satisfied when required namespaces are empty`() {
        val required = emptyMap<String, Sign.Model.Namespace.Proposal>()
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sendTransaction"),
                events = listOf("chainChanged", "accountChanged")
            )
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `test if session namespaces are valid when required and optional namespaces are empty`() {
        val required = emptyMap<String, Sign.Model.Namespace.Proposal>()
        val optional = emptyMap<String, Sign.Model.Namespace.Proposal>()
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `test if session namespaces doesn't have empty chains when they are not supoorted`() {
        val required = emptyMap<String, Sign.Model.Namespace.Proposal>()
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
            ),
            "solana" to Sign.Model.Namespace.Proposal(
                chains = listOf("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1", "solana:8E9rvCKLFQia2Y35HXjjpWzj8weVo44K"),
                methods = listOf("solana_signMessage", " solana_signTransaction"),
                events = listOf("chainChanged"),
            )
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            ),
            "solana" to Sign.Model.Namespace.Session(
                chains = listOf("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"),
                methods = listOf("solana_signMessage", " solana_signTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp:5r9aH2Jm9K6N1QP247TByNg34jMsFvcM5fGBYpw4w5nm")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `test if methods in optional namespaces are satisfied if events are empty`() {
        val required = mapOf("eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:1"), methods = listOf(), events = listOf()))
        val optional = mapOf("eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:1"), methods = listOf("eth_sendTransaction"), events = listOf("")))
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sendTransaction"),
                events = listOf(),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 1 - optional method`() {
        val required = mapOf("eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:1"), methods = listOf("personal_sign"), events = listOf("chainChanged")))
        val optional = mapOf("eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:1"), methods = listOf("eth_sendTransaction"), events = listOf("")))
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:137", "eip155:3"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf(
                    "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:137:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:3:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
                )
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 2 - optional chain`() {
        val required = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sendTransaction", "personal_sign"),
                events = listOf("chainChanged")
            )
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:2"),
                methods = listOf("eth_sendTransaction", "personal_sign"),
                events = listOf("chainChanged")
            )
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:3"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092", "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092", "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 3 - inline chain`() {
        val required = mapOf("eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")))
        val optional = mapOf("eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")))
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:3"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092", "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092", "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 4 - multiple inline chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:3"),
                methods = listOf("eth_sendTransaction", "personal_sign"),
                events = listOf("chainChanged")
            )
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:3"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf(
                    "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:3:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
                )
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:3"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf(
                    "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:3:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
                )
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 5 - multiple inline chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:3"), methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:4" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val accounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:3:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:4:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:3", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:3", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 6 - unsupported optional chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:3"), methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:4" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val accounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 7 - partially supported optional chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(chains = listOf("eip155:3"), methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:4" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val accounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:4:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 8 - partially supported optional methods`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction", "eth_signTypedData"),
                events = listOf("chainChanged")
            ),
        )
        val accounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged"),
                accounts = accounts
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 9 - partially supported optional events`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction", "eth_signTypedData"),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val accounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = accounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = accounts
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 10 - extra supported chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction", "eth_signTypedData"),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val accounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:4:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = accounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = listOf(
                    "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
                )
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `generate approved namespaces - config 11 - multiple required namespaces`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "cosmos" to Sign.Model.Namespace.Proposal(chains = listOf("cosmos:cosmoshub-4"), methods = listOf("cosmos_method"), events = listOf("cosmos_event"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf(
            "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
            "eip155:4:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
        )
        val cosmosAccounts = listOf("cosmos:cosmoshub-4:cosmos1hsk6jryyqjfhp5dhc55tc9jtckygx0eph6dd02")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2", "eip155:4"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = eipAccounts
            ),
            "cosmos" to Sign.Model.Namespace.Session(
                chains = listOf("cosmos:cosmoshub-4"),
                methods = listOf("cosmos_method"),
                events = listOf("cosmos_event"),
                accounts = cosmosAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = listOf(
                    "eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092",
                    "eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092"
                )
            ),
            "cosmos" to Sign.Model.Namespace.Session(
                chains = listOf("cosmos:cosmoshub-4"),
                methods = listOf("cosmos_method"),
                events = listOf("cosmos_event"),
                accounts = cosmosAccounts
            )
        )

        assertEquals(expected, approved)
    }

    @Test
    fun `should throw error - config 1 - required chains are not supported`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "cosmos" to Sign.Model.Namespace.Proposal(chains = listOf("cosmos:cosmoshub-4"), methods = listOf("cosmos_method"), events = listOf("cosmos_event"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:5:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:5"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("All required namespaces must be approved", "${exception.message}")
    }

    @Test
    fun `should throw error - config 2 - partially supported required chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "cosmos" to Sign.Model.Namespace.Proposal(chains = listOf("cosmos:cosmoshub-4"), methods = listOf("cosmos_method"), events = listOf("cosmos_event"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092", "eip155:5:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:5"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("All required namespaces must be approved", "${exception.message}")
    }

    @Test
    fun `should throw error - config 3 - not supported required methods`() {
        val required = mapOf("eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")))
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign"),
                events = listOf("chainChanged", "accountChanged"),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("All required namespaces must be approved: not all methods are approved", "${exception.message}")
    }

    @Test
    fun `should throw error - config 4 - not supported required methods`() {
        val required = mapOf("eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")))
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign"),
                events = listOf(),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("All required namespaces must be approved: not all methods are approved", "${exception.message}")
    }

    @Test
    fun `should throw error - config 5 - no accounts for required chains`() {
        val required = mapOf("eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")))
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("Accounts must be defined in matching namespace", "${exception.message}")
    }

    @Test
    fun `should throw error - config 6 - partial accounts for required chains`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("Accounts must be defined in matching namespace", "${exception.message}")
    }

    @Test
    fun `should throw error - config 7 - caip-10 is not supported`() {
        val required = mapOf(
            "eip155:1" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged")),
            "eip155:2" to Sign.Model.Namespace.Proposal(methods = listOf("eth_sendTransaction", "personal_sign"), events = listOf("chainChanged"))
        )
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData",
                ),
                events = listOf("chainChanged", "accountChanged")
            ),
        )
        val eipAccounts = listOf("eip155:2:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092", "0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = eipAccounts
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())
        val exception = assertThrows(Exception::class.java) { generateApprovedNamespaces(proposal, supported) }
        assertEquals("Accounts must be CAIP-10 compliant", "${exception.message}")
    }

    @Test
    fun `test optional namespaces with unsupported chains are excluded from result`() {
        val required = emptyMap<String, Sign.Model.Namespace.Proposal>()
        val optional = mapOf(
            "eip155" to Sign.Model.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:2"),
                methods = listOf(
                    "personal_sign",
                    "eth_sendTransaction",
                    "eth_signTransaction",
                    "eth_signTypedData"
                ),
                events = listOf("chainChanged", "accountsChanged")
            ),
            "bip122" to Sign.Model.Namespace.Proposal(
                chains = listOf("bip122:000000000019d6689c085ae165831e93"),
                methods = listOf("bip122_signTransaction"),
                events = listOf("chainChanged")
            )
        )
        val supported = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            ),
            "bip122" to Sign.Model.Namespace.Session(
                chains = listOf("bip122:000000000019d6689c085ae165831e92"),
                methods = listOf("bip122_signTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("bip122:000000000019d6689c085ae165831e92:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )
        val proposal = Sign.Model.SessionProposal("", "", "", "", listOf(), "", requiredNamespaces = required, optionalNamespaces = optional, mapOf(), "", "", "", mapOf())

        val approved = generateApprovedNamespaces(proposal, supported)
        val expected = mapOf(
            "eip155" to Sign.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092")
            )
        )

        assertEquals(expected, approved)
    }
}