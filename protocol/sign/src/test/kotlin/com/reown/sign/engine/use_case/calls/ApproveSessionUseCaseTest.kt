package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.reown.android.pulse.domain.InsertTelemetryEventUseCase
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidNamespaceException
import com.reown.sign.common.exceptions.SessionProposalExpiredException
import com.reown.sign.common.model.vo.proposal.ProposalVO
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.proposal.ProposalStorageRepository
import com.reown.sign.storage.sequence.SessionStorageRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ApproveSessionUseCaseTest {
    private lateinit var approveSessionUseCase: ApproveSessionUseCase
    private val jsonRpcInteractor = mockk<RelayJsonRpcInteractorInterface>(relaxed = true)
    private val crypto = mockk<KeyManagementRepository>(relaxed = true)
    private val sessionStorageRepository = mockk<SessionStorageRepository>(relaxed = true)
    private val proposalStorageRepository = mockk<ProposalStorageRepository>(relaxed = true)
    private val metadataStorageRepository = mockk<MetadataStorageRepositoryInterface>(relaxed = true)
    private val verifyContextStorageRepository = mockk<VerifyContextStorageRepository>(relaxed = true)
    private val insertEventUseCase = mockk<InsertTelemetryEventUseCase>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val selfAppMetaData = mockk<AppMetaData>(relaxed = true)

    @Before
    fun setUp() {
        approveSessionUseCase = ApproveSessionUseCase(
            jsonRpcInteractor,
            crypto,
            sessionStorageRepository,
            proposalStorageRepository,
            metadataStorageRepository,
            verifyContextStorageRepository,
            selfAppMetaData,
            insertEventUseCase,
            logger
        )
    }

    @Test
    fun `approve should call onSuccess when session is approved successfully`() = runTest {
        val proposerPublicKey = "proposerPublicKey"
        val selfPublicKey = "selfPublicKey"
        val sessionNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Session(
                chains = listOf("eip155:1", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged"),
                accounts = listOf("eip155:1:0x1234556")
            )
        )
        val proposal = mockk<ProposalVO>(relaxed = true) {
            every { expiry } returns null
        }
        val sessionTopic = Topic("sessionTopic")

        coEvery { proposalStorageRepository.getProposalByKey(any()) } returns proposal
        coEvery { crypto.generateAndStoreX25519KeyPair() } returns PublicKey(selfPublicKey)
        coEvery { crypto.generateTopicFromKeyAgreement(any(), any()) } returns sessionTopic
        coEvery { crypto.getSelfPublicFromKeyAgreement(any()) } returns PublicKey(selfPublicKey)
        coEvery { jsonRpcInteractor.subscribe(any(), any(), any()) } just Runs
        coEvery { jsonRpcInteractor.respondWithParams(any(), any(), any(), any(), any(), any()) } just Runs
        coEvery { jsonRpcInteractor.publishJsonRpcResponse(any(), any(), any(), captureLambda(), any(), any(), any()) } answers {
            lambda<() -> Unit>().invoke()
        }
        coEvery { sessionStorageRepository.insertSession(any(), any()) } just Runs
        coEvery { metadataStorageRepository.insertOrAbortMetadata(any(), any(), any()) } just Runs
        coEvery { metadataStorageRepository.insertOrAbortMetadata(any(), any(), any()) } just Runs
        every { logger.log(any<String>()) } just Runs
        val onSuccess = mockk<() -> Unit>(relaxed = true)

        approveSessionUseCase.approve(
            proposerPublicKey,
            sessionNamespaces,
            onSuccess = onSuccess,
            onFailure = {}
        )

        coVerify {
            jsonRpcInteractor.approveSession(
                any(),
                eq(sessionTopic),
                any(),
                any(),
                eq(listOf("eip155:1", "eip155:42161")),
                eq(listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData")),
                eq(listOf("chainChanged", "accountsChanged")),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `approve should call onFailure when proposal is expired`() = runTest {
        val proposerPublicKey = "proposerPublicKey"
        val sessionNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Session(
                chains = listOf("eip155:1", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged"),
                accounts = listOf("eip155:1:0x1234556")
            )
        )
        val proposal = mockk<ProposalVO>(relaxed = true) {
            every { expiry } returns Expiry(0L)
        }

        coEvery { proposalStorageRepository.getProposalByKey(proposerPublicKey) } returns proposal
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)

        approveSessionUseCase.approve(
            proposerPublicKey,
            sessionNamespaces,
            onSuccess = {},
            onFailure = onFailure
        )

        coVerify { onFailure.invoke(any<SessionProposalExpiredException>()) }
    }

    @Test
    fun `approve should call onFailure when namespace validation fails`() = runTest {
        val proposerPublicKey = "proposerPublicKey"
        val sessionNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Session(
                chains = listOf("eip155:1:213442343243223", "eip155:42161"),
                methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                events = listOf("chainChanged", "accountsChanged"),
                accounts = listOf("eip155:1:0x1234556")
            )
        )
        val proposal = mockk<ProposalVO>(relaxed = true) {
            every { expiry } returns null
        }

        coEvery { proposalStorageRepository.getProposalByKey(proposerPublicKey) } returns proposal
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)

        approveSessionUseCase.approve(
            proposerPublicKey,
            sessionNamespaces,
            onSuccess = {},
            onFailure = onFailure
        )

        coVerify { onFailure.invoke(any<InvalidNamespaceException>()) }
    }

    @Test
    fun `approve should call onFailure when session settle fails`() = runTest {
        val proposerPublicKey = "proposerPublicKey"
        val sessionNamespaces = mapOf<String, EngineDO.Namespace.Session>()
        val proposal = mockk<ProposalVO>(relaxed = true)
        val sessionTopic = mockk<Topic>()

        coEvery { proposalStorageRepository.getProposalByKey(proposerPublicKey) } returns proposal
        coEvery { crypto.generateAndStoreX25519KeyPair() } returns mockk()
        coEvery { crypto.generateTopicFromKeyAgreement(any(), any()) } returns sessionTopic

        coEvery { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any()) } answers {
            lastArg<(Throwable) -> Unit>().invoke(Exception("Session settle failure"))
        }

        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)

        approveSessionUseCase.approve(
            proposerPublicKey,
            sessionNamespaces,
            onSuccess = {},
            onFailure = onFailure
        )

        coVerify { onFailure.invoke(any<Exception>()) }
    }

    @Test
    fun `approve should extract chains from accounts when chains are absent`() = runTest {
        val proposerPublicKey = "proposer"
        val sessionNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Session(
                chains = null,
                methods = listOf("eth_sign"),
                events = listOf("accountsChanged"),
                accounts = listOf(
                    "eip155:1:0xabc"
                )
            )
        )
        val proposal = mockk<ProposalVO>(relaxed = true) { every { expiry } returns null }
        val sessionTopic = Topic("sessionTopic")

        coEvery { proposalStorageRepository.getProposalByKey(any()) } returns proposal
        coEvery { crypto.generateAndStoreX25519KeyPair() } returns PublicKey("self")
        coEvery { crypto.generateTopicFromKeyAgreement(any(), any()) } returns sessionTopic

        val chainsSlot = io.mockk.slot<List<String>?>()

        approveSessionUseCase.approve(proposerPublicKey, sessionNamespaces, onSuccess = {}, onFailure = {})

        coVerify {
            jsonRpcInteractor.approveSession(
                any(),
                eq(sessionTopic),
                any(),
                any(),
                captureNullable(chainsSlot),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        kotlin.test.assertEquals(listOf("eip155:1"), chainsSlot.captured)
    }

    @Test
    fun `approve should dedupe analytics arrays`() = runTest {
        val proposerPublicKey = "proposer"
        val sessionNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Session(
                chains = listOf("eip155:1", "eip155:1"),
                methods = listOf("eth_sign", "eth_sign"),
                events = listOf("accountsChanged", "accountsChanged"),
                accounts = listOf("eip155:1:0xabc", "eip155:1:0xabc")
            )
        )
        val proposal = mockk<ProposalVO>(relaxed = true) { every { expiry } returns null }
        val sessionTopic = Topic("sessionTopic")

        coEvery { proposalStorageRepository.getProposalByKey(any()) } returns proposal
        coEvery { crypto.generateAndStoreX25519KeyPair() } returns PublicKey("self")
        coEvery { crypto.generateTopicFromKeyAgreement(any(), any()) } returns sessionTopic

        approveSessionUseCase.approve(proposerPublicKey, sessionNamespaces, onSuccess = {}, onFailure = {})

        coVerify {
            jsonRpcInteractor.approveSession(
                any(),
                eq(sessionTopic),
                any(),
                any(),
                eq(listOf("eip155:1")),
                eq(listOf("eth_sign")),
                eq(listOf("accountsChanged")),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `approve should forward session and scoped properties`() = runTest {
        val proposerPublicKey = "proposer"
        val sessionNamespaces = mapOf(
            "eip155" to EngineDO.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("eth_sign"),
                events = listOf("accountsChanged"),
                accounts = listOf("eip155:1:0xabc")
            )
        )
        val sessionProperties = mapOf("foo" to "bar")
        val scopedProperties = mapOf("scope" to "value")
        val proposal = mockk<ProposalVO>(relaxed = true) { every { expiry } returns null }
        val sessionTopic = Topic("sessionTopic")

        coEvery { proposalStorageRepository.getProposalByKey(any()) } returns proposal
        coEvery { crypto.generateAndStoreX25519KeyPair() } returns PublicKey("self")
        coEvery { crypto.generateTopicFromKeyAgreement(any(), any()) } returns sessionTopic

        approveSessionUseCase.approve(
            proposerPublicKey,
            sessionNamespaces,
            sessionProperties = sessionProperties,
            scopedProperties = scopedProperties,
            onSuccess = {},
            onFailure = {}
        )

        coVerify {
            jsonRpcInteractor.approveSession(
                any(),
                eq(sessionTopic),
                any(),
                any(),
                eq(listOf("eip155:1")),
                eq(listOf("eth_sign")),
                eq(listOf("accountsChanged")),
                eq(sessionProperties),
                eq(scopedProperties),
                any(),
                any(),
                any()
            )
        }
    }
}
