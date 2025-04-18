package com.reown.sign.test.client

import com.reown.android.Core
import com.reown.sign.BuildConfig
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.reown.sign.test.scenario.SignClientInstrumentedActivityScenario
import com.reown.sign.test.utils.TestClient
import com.reown.sign.test.utils.dapp.AutoApproveDappDelegate
import com.reown.sign.test.utils.dapp.DappDelegate
import com.reown.sign.test.utils.dapp.DappSignClient
import com.reown.sign.test.utils.dapp.dappClientConnect
import com.reown.sign.test.utils.dapp.dappClientSendRequest
import com.reown.sign.test.utils.globalOnError
import com.reown.sign.test.utils.sessionChains
import com.reown.sign.test.utils.sessionEvents
import com.reown.sign.test.utils.sessionNamespaceKey
import com.reown.sign.test.utils.wallet.AutoApproveSessionWalletDelegate
import com.reown.sign.test.utils.wallet.WalletDelegate
import com.reown.sign.test.utils.wallet.WalletSignClient
import com.reown.sign.test.utils.wallet.dappClientExtendSession
import com.reown.sign.test.utils.wallet.rejectOnSessionProposal
import com.reown.sign.test.utils.wallet.walletClientEmitEvent
import com.reown.sign.test.utils.wallet.walletClientExtendSession
import com.reown.sign.test.utils.wallet.walletClientRespondToRequest
import com.reown.sign.test.utils.wallet.walletClientUpdateSession
import junit.framework.TestCase.fail
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

class SignClientInstrumentedAndroidTest {
	@get:Rule
	val scenarioExtension = SignClientInstrumentedActivityScenario()

	private fun setDelegates(walletDelegate: SignClient.WalletDelegate, dappDelegate: SignClient.DappDelegate) {
		WalletSignClient.setWalletDelegate(walletDelegate)
		DappSignClient.setDappDelegate(dappDelegate)
	}

	private fun launch(walletDelegate: SignClient.WalletDelegate, dappDelegate: SignClient.DappDelegate) {
		setDelegates(walletDelegate, dappDelegate)
		scenarioExtension.launch(BuildConfig.TEST_TIMEOUT_SECONDS.toLong()) { pairAndConnect() }
	}

	@Test
	fun pair() {
		Timber.d("pair: start")
		setDelegates(WalletDelegate(), DappDelegate())

		scenarioExtension.launch(BuildConfig.TEST_TIMEOUT_SECONDS.toLong()) { pairDappAndWallet { scenarioExtension.closeAsSuccess().also { Timber.d("pair: finish") } } }
	}

	@Test
	fun establishSession() {
		Timber.d("establishSession: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()
		val dappDelegate = AutoApproveDappDelegate { scenarioExtension.closeAsSuccess().also { Timber.d("establishSession: finish") } }
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveSessionProposal() {
		Timber.d("receiveRejectSession: start")

		val walletDelegate = object : WalletDelegate() {
			override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal, verifyContext: Sign.Model.VerifyContext) {
				scenarioExtension.closeAsSuccess().also { Timber.d("proposal received: finish") }
			}
		}
		val dappDelegate = object : DappDelegate() {}

		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveRejectSession() {
		Timber.d("receiveRejectSession: start")

		val walletDelegate = object : WalletDelegate() {
			override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal, verifyContext: Sign.Model.VerifyContext) {
				sessionProposal.rejectOnSessionProposal()
			}
		}

		val dappDelegate = object : DappDelegate() {
			override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveRejectSession: finish") }
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveDisconnectSessionFromDapp() {
		Timber.d("receiveDisconnectSessionFromDapp: start")

		val walletDelegate = object : AutoApproveSessionWalletDelegate() {
			override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveDisconnectSessionFromDapp: finish") }
			}
		}

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			DappSignClient.disconnect(
				Sign.Params.Disconnect(approvedSession.topic),
				onSuccess = { Timber.d("Dapp: disconnectOnSuccess") },
				onError = ::globalOnError
			)
		}

		val dappDelegate = AutoApproveDappDelegate(onSessionApprovedSuccess)

		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveDisconnectSessionFromWallet() {
		Timber.d("receiveDisconnectSessionFromWallet: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			WalletSignClient.disconnect(
				Sign.Params.Disconnect(approvedSession.topic),
				onSuccess = { Timber.d("Wallet: disconnectOnSuccess") },
				onError = ::globalOnError
			)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveDisconnectSessionFromWallet: finish") }
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveRespondWithResultToSessionRequest() {
		Timber.d("receiveRespondWithResultToSessionRequest: start")

		val walletDelegate = object : AutoApproveSessionWalletDelegate() {
			override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest, verifyContext: Sign.Model.VerifyContext) {
				walletClientRespondToRequest(sessionRequest.topic, Sign.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.request.id, "dummy"))
			}
		}

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			dappClientSendRequest(approvedSession.topic)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
				when (response.result) {
					is Sign.Model.JsonRpcResponse.JsonRpcError -> fail("Expected result response not error")
					is Sign.Model.JsonRpcResponse.JsonRpcResult -> {
						// Validate the result
						scenarioExtension.closeAsSuccess().also { Timber.d("receiveRespondWithResultToSessionRequest: finish") }
					}
				}
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveRespondWithErrorToSessionRequest() {
		Timber.d("receiveRespondWithErrorToSessionRequest: start")

		val walletDelegate = object : AutoApproveSessionWalletDelegate() {
			override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest, verifyContext: Sign.Model.VerifyContext) {
				walletClientRespondToRequest(sessionRequest.topic, Sign.Model.JsonRpcResponse.JsonRpcError(sessionRequest.request.id, 0, "test error"))
			}
		}

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			dappClientSendRequest(approvedSession.topic)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
				when (response.result) {
					is Sign.Model.JsonRpcResponse.JsonRpcError -> scenarioExtension.closeAsSuccess().also { Timber.d("receiveRespondWithErrorToSessionRequest: finish") }
					is Sign.Model.JsonRpcResponse.JsonRpcResult -> fail("Expected error response not result")
				}
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveSessionUpdate() {
		Timber.d("receiveSessionUpdate: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			walletClientUpdateSession(approvedSession.topic)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {
				assert(updatedSession.namespaces[sessionNamespaceKey]?.chains?.size == sessionChains.size + 1)
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveSessionUpdate: finish") }
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveSessionEvent() {
		Timber.d("receiveSessionEvent: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			walletClientEmitEvent(approvedSession.topic)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {
				assert(sessionEvent.data.contains("0x1111"))
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveSessionEvent: finish") }
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun receiveEvent() {
		Timber.d("receiveEvent: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			walletClientEmitEvent(approvedSession.topic)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionEvent(sessionEvent: Sign.Model.Event) {
				assert(sessionEvent.name == sessionEvents.first())
				assert(sessionEvent.data.contains("0x1111"))
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveEvent: finish") }
			}
		}
		launch(walletDelegate, dappDelegate)
	}


	@Test
	fun receiveEventAndSessionEvent() {
		Timber.d("receiveEventAndSessionEvent: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			walletClientEmitEvent(approvedSession.topic)
		}

		var isOnEventReceived = false
		var isOnSessionEventReceived = false

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionEvent(sessionEvent: Sign.Model.Event) {
				assert(sessionEvent.name == sessionEvents.first())
				assert(sessionEvent.data.contains("0x1111"))
				Timber.d("receiveEventAndSessionEvent: onEvent")
				isOnEventReceived = true
				if (isOnSessionEventReceived) {
					scenarioExtension.closeAsSuccess()
				}
			}

			override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {
				assert(sessionEvent.name == sessionEvents.first())
				assert(sessionEvent.data.contains("0x1111"))
				Timber.d("receiveEventAndSessionEvent: onSessionEvent")
				isOnSessionEventReceived = true
				if (isOnEventReceived) {
					scenarioExtension.closeAsSuccess()
				}
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun extendSessionByWallet() {
		Timber.d("receiveSessionExtendByWallet: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			walletClientExtendSession(approvedSession.topic)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionExtend(session: Sign.Model.Session) {
				scenarioExtension.closeAsSuccess().also { Timber.d("receiveSessionExtend: finish") }
			}
		}

		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun extendSessionByDapp() {
		Timber.d("receiveSessionExtendByDapp: start")

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			Timber.d("session approved: ${approvedSession.topic}")
			dappClientExtendSession(approvedSession.topic)
		}

		val dappDelegate = AutoApproveDappDelegate(onSessionApprovedSuccess)

		val walletDelegate = object : AutoApproveSessionWalletDelegate() {
			override fun onSessionExtend(session: Sign.Model.Session) {
				scenarioExtension.closeAsSuccess().also { Timber.d("Wallet receiveSessionExtend: finish: ${session.expiry}") }
			}
		}

		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun interceptWalletGetAssetsWithWalletService() {
		Timber.d("interceptWalletGetAssetsWithWalletService: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			DappSignClient.request(
				Sign.Params.Request(
					approvedSession.topic, "wallet_getAssets", JSONObject()
						.put("account", "0x9CAaB7E1D1ad6eaB4d6a7f479Cb8800da551cbc0")
						.put("chainFilter", JSONArray().put("0xa"))
						.toString(), sessionChains.first()
				),
				onSuccess = { _: Sign.Model.SentRequest -> Timber.d("Dapp: requestOnSuccess") },
				onError = ::globalOnError
			)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
				when (response.result) {
					is Sign.Model.JsonRpcResponse.JsonRpcError -> fail("Expected result response not error: ${(response.result as Sign.Model.JsonRpcResponse.JsonRpcError).message}")
					is Sign.Model.JsonRpcResponse.JsonRpcResult -> {
						assert(response.method == "wallet_getAssets")
						assert((response.result as Sign.Model.JsonRpcResponse.JsonRpcResult).result.contains("balance"))
						scenarioExtension.closeAsSuccess()
							.also { Timber.d("receive session request response: finish: ${(response.result as Sign.Model.JsonRpcResponse.JsonRpcResult).result}") }
					}
				}
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	@Test
	fun interceptWalletGetAssetsWithWalletServiceThatReturnsAnEmptyResponse() {
		Timber.d("interceptWalletGetAssetsWithWalletService: start")

		val walletDelegate = AutoApproveSessionWalletDelegate()

		val onSessionApprovedSuccess = { approvedSession: Sign.Model.ApprovedSession ->
			DappSignClient.request(
				Sign.Params.Request(
					approvedSession.topic, "wallet_getAssets", JSONObject()
						.put("account", "0x9CAaB7E1D1ad6eaB4d6a7f479Cb8800da5512234")
						.put("chainFilterrrr", JSONArray().put("111"))
						.put("test", JSONArray().put("qqq"))
						.toString(), sessionChains.first()
				),
				onSuccess = { _: Sign.Model.SentRequest -> Timber.d("Dapp: requestOnSuccess") },
				onError = ::globalOnError
			)
		}

		val dappDelegate = object : AutoApproveDappDelegate(onSessionApprovedSuccess) {
			override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
				when (response.result) {
					is Sign.Model.JsonRpcResponse.JsonRpcError -> {
						fail("Expected result response not error")
					}

					is Sign.Model.JsonRpcResponse.JsonRpcResult -> {
						assert(response.method == "wallet_getAssets")
						scenarioExtension.closeAsSuccess().also { Timber.d("receive session request response error: finish") }
					}
				}
			}
		}
		launch(walletDelegate, dappDelegate)
	}

	private fun pairDappAndWallet(onPairSuccess: (pairing: Core.Model.Pairing) -> Unit) {
		val pairing: Core.Model.Pairing = (TestClient.Dapp.Pairing.create(onError = ::globalOnError) ?: fail("Unable to create a Pairing")) as Core.Model.Pairing
		Timber.d("DappClient.pairing.create: $pairing")

		TestClient.Wallet.Pairing.pair(Core.Params.Pair(pairing.uri), onError = ::globalOnError, onSuccess = {
			Timber.d("WalletClient.pairing.pair: $pairing")
			onPairSuccess(pairing)
		})
	}

	private fun pairAndConnect() {
		pairDappAndWallet { pairing -> dappClientConnect(pairing) }
	}
}