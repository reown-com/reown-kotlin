package com.reown.sign.engine.use_case.calls

import com.reown.android.internal.common.crypto.kmr.KeyManagementRepository
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.RelayProtocolOptions
import com.reown.android.internal.common.model.SymmetricKey
import com.reown.android.internal.common.model.type.RelayJsonRpcInteractorInterface
import com.reown.foundation.common.model.Topic
import com.reown.foundation.util.Logger
import com.reown.sign.common.exceptions.InvalidNamespaceException
import com.reown.sign.engine.model.EngineDO
import com.reown.sign.storage.proposal.ProposalStorageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ProposeSessionUseCaseTest {
    private val jsonRpcInteractor = mockk<RelayJsonRpcInteractorInterface>()
    private val crypto = mockk<KeyManagementRepository>()
    private val proposalStorageRepository = mockk<ProposalStorageRepository>()
    private val selfAppMetaData = mockk<AppMetaData>()
    private val logger = mockk<Logger>()
    private val proposeSessionUseCase = ProposeSessionUseCase(jsonRpcInteractor, crypto, proposalStorageRepository, selfAppMetaData, logger)

    @Before
    fun setUp() {
        every { logger.error(any() as String) } answers { }
        every { logger.error(any() as Exception) } answers { }
    }

    @Test
    fun `onFailure is called when SignValidator validateProposalNamespaces fails`() = runTest {

        proposeSessionUseCase.proposeSession(
            requiredNamespaces = mapOf("required" to EngineDO.Namespace.Proposal(listOf("required"), listOf("required"), listOf("required"))),
            optionalNamespaces = mapOf("optional" to EngineDO.Namespace.Proposal(listOf("optional"), listOf("optional"), listOf("optional"))),
            properties = mapOf("key" to "value"),
            scopedProperties = null,
            pairing = com.reown.android.internal.common.model.Pairing(
                topic = Topic("topic"),
                relay = RelayProtocolOptions(),
                symmetricKey = SymmetricKey("symmetricKey"),
                methods = "",
                expiry = Expiry(12345678L)
            ),
            onSuccess = {
                fail("onSuccess should not be called since should have validation failed")
            },
            onFailure = { error ->
                assertSame(InvalidNamespaceException::class, error::class)
            }
        )
    }
}