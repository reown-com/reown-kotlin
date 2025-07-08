package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SymmetricKey
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.utils.fiveMinutesInSeconds
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidNamespaceException
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.proposal.ProposalStorageRepository
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ProposeUseCaseTest {
    private lateinit var jsonRpcInteractor: RelayJsonRpcInteractorInterface
    private lateinit var crypto: KeyManagementRepository
    private lateinit var proposalStorageRepository: ProposalStorageRepository
    private lateinit var logger: Logger
    private lateinit var proposeSessionUseCase: ProposeSessionUseCase
    private lateinit var selfAppMetaData: AppMetaData

    @Before
    fun setUp() {
        jsonRpcInteractor = mockk(relaxed = true)
        crypto = mockk(relaxed = true)
        proposalStorageRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        selfAppMetaData = mockk(relaxed = true)

        proposeSessionUseCase = ProposeSessionUseCase(
            jsonRpcInteractor = jsonRpcInteractor,
            crypto = crypto,
            proposalStorageRepository = proposalStorageRepository,
            selfAppMetaData = selfAppMetaData,
            logger = logger
        )
    }

    @Test
    fun `proposeSession should succeed when all inputs are valid`() = runTest {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            requiredNamespaces,
            null,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        verify { logger.log("Sending proposal on topic: test_topic") }
        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `proposeSession should fail when validation fails`() = runTest {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:sasdsa:2222", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")

        proposeSessionUseCase.proposeSession(
            requiredNamespaces,
            null,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = {
                assert(it is InvalidNamespaceException)
            }
        )

        coVerify { jsonRpcInteractor wasNot Called }
    }

    @Test
    fun `proposeSession should call onFailure when subscription fails`() = runTest {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any(), captureLambda()) } answers {
            lambda<(Throwable) -> Unit>().invoke(Exception("Subscription Error"))
        }

        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)

        proposeSessionUseCase.proposeSession(
            requiredNamespaces,
            null,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = onFailure
        )

        coVerify { onFailure.invoke(any()) }
    }

    @Test
    fun `proposeSession should call onFailure when publishing request fails`() = runTest {
        val requiredNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        every { logger.log(any<String>()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any(), any(), captureLambda()) } answers {
            lambda<(Throwable) -> Unit>().invoke(Exception("Publishing Error"))
        }
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)

        proposeSessionUseCase.proposeSession(
            requiredNamespaces,
            null,
            null,
            null,
            pairing,
            onSuccess = {},
            onFailure = onFailure
        )
        coVerify { onFailure.invoke(any()) }
    }

    // Test cases for merging required and optional namespaces (adapted from JS tests)

    @Test
    fun `should merge required and optional namespaces case 1`() = runTest {
        // Case 1: Required with chain index, optional with same chain index
        val required = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged")
            )
        )
        val optional = mapOf(
            "eip155:1" to EngineDO.Namespace.Proposal(
                chains = emptyList(),
                methods = listOf("eth_sendTransaction"),
                events = listOf("accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            required,
            optional,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `should merge required and optional namespaces case 2`() = runTest {
        // Case 2: Required with namespace key, optional with same namespace key
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged")
            )
        )
        val optional = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sendTransaction"),
                events = listOf("accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            required,
            optional,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `should merge required and optional namespaces case 3`() = runTest {
        // Case 3: Required and optional with different namespaces
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged")
            )
        )
        val optional = mapOf(
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf("solana:1"),
                methods = listOf("solana_signTransaction"),
                events = listOf("accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            required,
            optional,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `should merge required and optional namespaces case 4`() = runTest {
        // Case 4: Multiple required namespaces with overlapping optional namespaces
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:2"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged")
            ),
            "solana" to EngineDO.Namespace.Proposal(
                chains = listOf("solana:1"),
                methods = listOf("solana_signTransaction"),
                events = listOf("accountsChanged")
            )
        )
        val optional = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("eth_signTypedData"),
                events = listOf("accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            required,
            optional,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `should handle null requiredNamespaces correctly`() = runTest {
        val optional = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sendTransaction"),
                events = listOf("accountsChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            null,
            optional,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `should handle null optionalNamespaces correctly`() = runTest {
        val required = mapOf(
            "eip155" to EngineDO.Namespace.Proposal(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction"),
                events = listOf("chainChanged")
            )
        )
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            required,
            null,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `should handle both null requiredNamespaces and optionalNamespaces`() = runTest {
        val pairing = Pairing(Topic("test_topic"), RelayProtocolOptions(), SymmetricKey("ss"), Expiry(fiveMinutesInSeconds), "methods")
        val publicKey = mockk<PublicKey>(relaxed = true)

        coEvery { crypto.generateAndStoreX25519KeyPair() } returns publicKey
        coEvery { jsonRpcInteractor.subscribe(pairing.topic, any()) } just Runs
        coEvery { proposalStorageRepository.insertProposal(any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs

        proposeSessionUseCase.proposeSession(
            null,
            null,
            null,
            null,
            pairing,
            onSuccess = { },
            onFailure = { }
        )

        coVerify {
            jsonRpcInteractor.publishJsonRpcRequest(
                eq(pairing.topic),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }
}