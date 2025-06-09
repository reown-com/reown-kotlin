package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.exception.CannotFindSequenceForTopic
import com.reown.android.internal.common.exception.InvalidExpiryException
import com.reown.android.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Namespace
import com.reown.android.internal.common.model.Redirect
import com.reown.android.internal.common.model.TransportType
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.android.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import com.reown.android.pulse.domain.InsertEventUseCase
import com.reown.foundation.common.model.PublicKey
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.model.vo.sequence.SessionVO
import com.reown.sign.engine.domain.wallet_service.WalletServiceFinder
import com.reown.sign.engine.domain.wallet_service.WalletServiceRequester
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.engine.model.tvf.TVF
import com.reown.sign.storage.sequence.SessionStorageRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.URL

class SessionRequestUseCaseTest {
    private val sessionStorageRepository = mockk<SessionStorageRepository>()
    private val jsonRpcInteractor = mockk<RelayJsonRpcInteractorInterface>()
    private val logger = mockk<Logger>()
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface = mockk()
    private val tvf: TVF = mockk()
    private val walletServiceFinder: WalletServiceFinder = mockk()
    private val walletServiceRequester: WalletServiceRequester = mockk()
    private val metadataStorageRepository = mockk<MetadataStorageRepositoryInterface>()
    private val insertEventUseCase = mockk<InsertEventUseCase>()
    private val sessionRequestUseCase = SessionRequestUseCase(
        sessionStorageRepository,
        jsonRpcInteractor,
        linkModeJsonRpcInteractor,
        metadataStorageRepository,
        insertEventUseCase,
        "clientId",
        logger,
        tvf,
        walletServiceFinder,
        walletServiceRequester
    )

    @Before
    fun setUp() {
        every { logger.error(any() as String) } answers { }
        every { logger.error(any() as Exception) } answers { }
    }

    @Test
    fun `onFailure is called when sessionStorageRepository isSessionValid is false`() = runTest {
        every { sessionStorageRepository.isSessionValid(any()) } returns false

        sessionRequestUseCase.sessionRequest(
            request = EngineDO.Request(
                topic = "topic",
                method = "method",
                params = "params",
                chainId = "chainId",
            ),
            onSuccess = {
                fail("onSuccess should not be called since should have validation failed")
            },
            onFailure = { error ->
                assertSame(CannotFindSequenceForTopic::class, error::class)
            }
        )
    }

    @Test
    fun `should fail if expiry is not within bounds`() = runTest {
        val session = SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = emptyMap(),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = null
        )
        // Arrange
        coEvery { sessionStorageRepository.isSessionValid(any()) } returns true
        coEvery { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns session
        coEvery { metadataStorageRepository.getByTopicAndType(any(), any()) } returns AppMetaData("", "", listOf(), "")

        val request = EngineDO.Request(
            topic = "topic",
            method = "method",
            params = "params",
            chainId = "chainId",
            expiry = Expiry(0)
        )

        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)

        // Act
        sessionRequestUseCase.sessionRequest(request, onSuccess = {}, onFailure = onFailure)

        // Assert
        coVerify { onFailure(ofType(InvalidExpiryException::class)) }
    }

    @Test
    fun `should send session request using link mode if peer link mode is enabled`() = runTest {
        val session = SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = mapOf(
                "eip155" to Namespace.Session(
                    chains = listOf("eip155:1", "eip155:42161"),
                    methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                    events = listOf("chainChanged", "accountsChanged"),
                    accounts = listOf("eip155:1:0x1234556")
                )
            ),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            peerAppMetaData = AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = true, universal = "link")),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = TransportType.LINK_MODE
        )
        // Arrange
        coEvery { sessionStorageRepository.isSessionValid(any()) } returns true
        coEvery { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns session
        coEvery { metadataStorageRepository.getByTopicAndType(any(), any()) } returns AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = true, universal = "link"))
        coEvery { insertEventUseCase.invoke(any()) } just Runs
        coEvery { linkModeJsonRpcInteractor.triggerRequest(any(), any(), any()) } just Runs
        coEvery { walletServiceFinder.findMatchingWalletService(any(), any()) } returns null

        val onSuccess: (Long) -> Unit = mockk(relaxed = true)

        val request = EngineDO.Request(
            topic = "topic",
            method = "personal_sign",
            params = "params",
            chainId = "eip155:1",
        )

        // Act
        sessionRequestUseCase.sessionRequest(request, onSuccess = onSuccess, onFailure = {})

        // Assert
        coVerify { linkModeJsonRpcInteractor.triggerRequest(any(), any(), any()) }
    }

    @Test
    fun `should publish session request through jsonRpcInteractor when transport type is RELAY`() = runTest {
        val session = SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = mapOf(
                "eip155" to Namespace.Session(
                    chains = listOf("eip155:1", "eip155:42161"),
                    methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                    events = listOf("chainChanged", "accountsChanged"),
                    accounts = listOf("eip155:1:0x1234556")
                )
            ),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            peerAppMetaData = AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = false, universal = "link")),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = TransportType.RELAY
        )
        // Arrange
        coEvery { sessionStorageRepository.isSessionValid(any()) } returns true
        coEvery { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns session
        coEvery { metadataStorageRepository.getByTopicAndType(any(), any()) } returns AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = true, universal = "link"))
        coEvery { insertEventUseCase.invoke(any()) } just Runs
        coEvery { linkModeJsonRpcInteractor.triggerRequest(any(), any(), any()) } just Runs
        every { tvf.collect(any(), any(), any()) } returns Triple(listOf("rpcMethod"), listOf("contractAddress"), "chainId")
        every { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any(), captureLambda(), any()) } answers {
            lambda<() -> Unit>().invoke()
        }
        every { logger.log(any<String>()) } just Runs
        coEvery { walletServiceFinder.findMatchingWalletService(any(), any()) } returns null

        val onSuccess: (Long) -> Unit = mockk(relaxed = true)
        val request = EngineDO.Request(
            topic = "topic",
            method = "personal_sign",
            params = "params",
            chainId = "eip155:1",
        )

        // Act
        sessionRequestUseCase.sessionRequest(request, onSuccess = onSuccess, onFailure = {})

        // Assert
        coVerify { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any(), any(), any()) }
        coVerify { onSuccess(any()) }
    }

    @Test
    fun `should handle failure when publishing session request fails`() = runTest {
        // Arrange
        val session = SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = mapOf(
                "eip155" to Namespace.Session(
                    chains = listOf("eip155:1", "eip155:42161"),
                    methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData"),
                    events = listOf("chainChanged", "accountsChanged"),
                    accounts = listOf("eip155:1:0x1234556")
                )
            ),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            peerAppMetaData = AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = false, universal = "link")),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = TransportType.RELAY
        )
        // Arrange
        coEvery { sessionStorageRepository.isSessionValid(any()) } returns true
        coEvery { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns session
        coEvery { metadataStorageRepository.getByTopicAndType(any(), any()) } returns AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = true, universal = "link"))
        coEvery { insertEventUseCase.invoke(any()) } just Runs
        coEvery { linkModeJsonRpcInteractor.triggerRequest(any(), any(), any()) } just Runs
        every { tvf.collect(any(), any(), any()) } returns Triple(listOf("rpcMethod"), listOf("contractAddress"), "chainId")
        every { jsonRpcInteractor.publishJsonRpcRequest(any(), any(), any(), any(), any(), any(), captureLambda()) } answers {
            lastArg<(Throwable) -> Unit>().invoke(Exception("Session request failure"))
        }
        every { logger.log(any<String>()) } just Runs
        coEvery { walletServiceFinder.findMatchingWalletService(any(), any()) } returns null

        val onFailure: (Throwable) -> Unit = mockk(relaxed = true)
        val request = EngineDO.Request(
            topic = "topic",
            method = "personal_sign",
            params = "params",
            chainId = "eip155:1",
        )

        // Act
        sessionRequestUseCase.sessionRequest(request, onSuccess = {}, onFailure = onFailure)

        // Assert
        coVerify { onFailure(ofType(Exception::class)) }
    }

    @Test
    fun `should intercept wallet_getAssets with wallet service`() = runTest {
        val session = SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = mapOf(
                "eip155" to Namespace.Session(
                    chains = listOf("eip155:1", "eip155:42161"),
                    methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData", "wallet_getAssets"),
                    events = listOf("chainChanged", "accountsChanged"),
                    accounts = listOf("eip155:1:0x1234556")
                )
            ),
            scopedProperties = mapOf(
                "eip155" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=12345678&st=wkca&sv=reown-kotlin-1.0.0\", \"methods\":[\"wallet_getAssets\"]}]}"
            ),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            peerAppMetaData = AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = false, universal = "link")),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = TransportType.RELAY
        )
        // Arrange
        coEvery { sessionStorageRepository.isSessionValid(any()) } returns true
        coEvery { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns session
        coEvery { metadataStorageRepository.getByTopicAndType(any(), any()) } returns AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = true, universal = "link"))
        coEvery { insertEventUseCase.invoke(any()) } just Runs
        every { tvf.collect(any(), any(), any()) } returns Triple(listOf("rpcMethod"), listOf("contractAddress"), "chainId")
        every { logger.log(any<String>()) } just Runs
        coEvery { walletServiceFinder.findMatchingWalletService(any(), any()) } returns URL("https://rpc.walletconnect.org/v1/wallet?projectId=12345678&st=wkca&sv=reown-kotlin-1.0.0")
        coEvery { walletServiceRequester.request(any(), any()) } returns "result"

        val onSuccess: (Long) -> Unit = mockk(relaxed = true)
        val request = EngineDO.Request(
            topic = "topic",
            method = "wallet_getAssets",
            params = JSONObject()
                .put("account", "1243")
                .put("chainFilter", JSONArray().put("0xa"))
                .toString(),
            chainId = "eip155:1",
        )

        // Act
        sessionRequestUseCase.sessionRequest(request, onSuccess = onSuccess, onFailure = {})

        // Assert
        coVerify { walletServiceRequester.request(any(), any()) }
    }

    @Test
    fun `should intercept wallet_getAssets with wallet service and handle error gracefully when wallet service request fails`() = runTest {
        val session = SessionVO(
            topic = Topic("topic"),
            expiry = Expiry(0),
            relayProtocol = "relayProtocol",
            relayData = "relayData",
            controllerKey = PublicKey(""),
            selfPublicKey = PublicKey(""),
            sessionNamespaces = mapOf(
                "eip155" to Namespace.Session(
                    chains = listOf("eip155:1", "eip155:42161"),
                    methods = listOf("eth_sendTransaction", "eth_signTransaction", "personal_sign", "eth_signTypedData", "wallet_getAssets"),
                    events = listOf("chainChanged", "accountsChanged"),
                    accounts = listOf("eip155:1:0x1234556")
                )
            ),
            scopedProperties = mapOf(
                "eip155" to "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=12345678&st=wkca&sv=reown-kotlin-1.0.0\", \"methods\":[\"wallet_getAssets\"]}]}"
            ),
            requiredNamespaces = emptyMap(),
            optionalNamespaces = emptyMap(),
            peerAppMetaData = AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = false, universal = "link")),
            isAcknowledged = false,
            pairingTopic = "pairingTopic",
            transportType = TransportType.RELAY
        )
        // Arrange
        coEvery { sessionStorageRepository.isSessionValid(any()) } returns true
        coEvery { sessionStorageRepository.getSessionWithoutMetadataByTopic(any()) } returns session
        coEvery { metadataStorageRepository.getByTopicAndType(any(), any()) } returns AppMetaData("", "", listOf(), "", redirect = Redirect(linkMode = true, universal = "link"))
        coEvery { insertEventUseCase.invoke(any()) } just Runs
        every { tvf.collect(any(), any(), any()) } returns Triple(listOf("rpcMethod"), listOf("contractAddress"), "chainId")
        every { logger.error(any<String>()) } just Runs
        coEvery { walletServiceFinder.findMatchingWalletService(any(), any()) } returns URL("https://rpc.walletconnect.org/v1/wallet?projectId=12345678&st=wkca&sv=reown-kotlin-1.0.0")
        coEvery { walletServiceRequester.request(any(), any()) } throws Exception("Wallet service request failed")

        val onSuccess: (Long) -> Unit = mockk(relaxed = true)
        val request = EngineDO.Request(
            topic = "topic",
            method = "wallet_getAssets",
            params = JSONObject()
                .put("account", "1243")
                .put("chainFilter", JSONArray().put("0xa"))
                .toString(),
            chainId = "eip155:1",
        )

        // Act
        sessionRequestUseCase.sessionRequest(request, onSuccess = onSuccess, onFailure = {})

        // Assert
        coVerify { logger.error(any<String>()) }
    }
}