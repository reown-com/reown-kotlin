package com.reown.appkit.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.reown.android.internal.common.wcKoinApp
import com.reown.android.pulse.domain.SendEventInterface
import com.reown.android.pulse.model.EventType
import com.reown.android.pulse.model.properties.Properties
import com.reown.android.pulse.model.properties.Props
import com.reown.foundation.util.Logger
import com.reown.appkit.client.Modal
import com.reown.appkit.client.AppKit
import com.reown.appkit.domain.model.Session
import com.reown.appkit.domain.usecase.GetEthBalanceUseCase
import com.reown.appkit.domain.usecase.GetSessionUseCase
import com.reown.appkit.domain.usecase.ObserveSelectedChainUseCase
import com.reown.appkit.domain.usecase.ObserveSessionUseCase
import com.reown.appkit.engine.AppKitEngine
import com.reown.appkit.ui.components.ComponentDelegate
import com.reown.appkit.ui.components.ComponentEvent
import com.reown.appkit.ui.openAppKit
import com.reown.appkit.utils.getChainNetworkImageUrl
import com.reown.appkit.utils.getChains
import com.reown.appkit.utils.getSelectedChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun rememberAppKitState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavController
): AppKitState {
    return remember(navController) {
        AppKitState(coroutineScope, navController)
    }
}

class AppKitState(
    coroutineScope: CoroutineScope,
    private val navController: NavController
) {
    private val logger: Logger = wcKoinApp.koin.get()
    private val observeSelectedChainUseCase: ObserveSelectedChainUseCase = wcKoinApp.koin.get()
    private val observeSessionTopicUseCase: ObserveSessionUseCase = wcKoinApp.koin.get()
    private val getSessionUseCase: GetSessionUseCase = wcKoinApp.koin.get()
    private val getEthBalanceUseCase: GetEthBalanceUseCase = wcKoinApp.koin.get()
    private val appKitEngine: AppKitEngine = wcKoinApp.koin.get()
    private val sendEventUseCase: SendEventInterface = wcKoinApp.koin.get()
    private val sessionTopicFlow = observeSessionTopicUseCase()

    val isOpen = ComponentDelegate.modalComponentEvent
        .map { event ->
            sendModalCloseOrOpenEvents(event)
            event.isOpen
        }
        .stateIn(coroutineScope, started = SharingStarted.Lazily, ComponentDelegate.isModalOpen)

    val isConnected = sessionTopicFlow
        .map { it != null && getSessionUseCase() != null }
        .map { AppKit.getAccount() != null }
        .stateIn(coroutineScope, started = SharingStarted.Lazily, initialValue = false)

    internal val selectedChain = observeSelectedChainUseCase().map { savedChainId ->
        AppKit.chains.find { it.id == savedChainId }
    }

    internal val accountNormalButtonState = sessionTopicFlow.combine(selectedChain) { session, chain -> session to chain }
        .mapOrAccountState(AccountButtonType.NORMAL)
        .stateIn(coroutineScope, started = SharingStarted.Lazily, initialValue = AccountButtonState.Loading)

    internal val accountMixedButtonState = sessionTopicFlow.combine(selectedChain) { session, chain -> session to chain }
        .mapOrAccountState(AccountButtonType.MIXED)
        .stateIn(coroutineScope, started = SharingStarted.Lazily, initialValue = AccountButtonState.Loading)

    private fun Flow<Pair<Session?, Modal.Model.Chain?>>.mapOrAccountState(accountButtonType: AccountButtonType) =
        map { appKitEngine.getActiveSession()?.mapToAccountButtonState(accountButtonType) ?: AccountButtonState.Invalid }

    private fun sendModalCloseOrOpenEvents(event: ComponentEvent) {
        when {
            event.isOpen && isConnected.value -> sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.MODAL_OPEN, Properties(connected = true)))
            event.isOpen && !isConnected.value -> sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.MODAL_OPEN, Properties(connected = false)))
            !event.isOpen && isConnected.value -> sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.MODAL_CLOSE, Properties(connected = true)))
            !event.isOpen && !isConnected.value -> sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.MODAL_CLOSE, Properties(connected = false)))
        }
    }

    private suspend fun Session.mapToAccountButtonState(accountButtonType: AccountButtonType) = try {
        val chains = getChains()
        val selectedChain = chains.getSelectedChain(this.chain)
        val address = this.address
        when (accountButtonType) {
            AccountButtonType.NORMAL -> AccountButtonState.Normal(address = address)
            AccountButtonType.MIXED -> {
                val balance = getBalance(selectedChain, address)
                AccountButtonState.Mixed(
                    address = address,
                    chainImage = selectedChain.chainImage ?: getChainNetworkImageUrl(selectedChain.chainReference),
                    chainName = selectedChain.chainName,
                    balance = balance
                )
            }
        }
    } catch (e: Exception) {
        AccountButtonState.Invalid
    }

    private suspend fun getBalance(selectedChain: Modal.Model.Chain, address: String) =
        selectedChain.rpcUrl?.let { url -> getEthBalanceUseCase(selectedChain.token, url, address)?.valueWithSymbol }

    internal fun openAppKit(shouldOpenChooseNetwork: Boolean = false, isActiveNetwork: Boolean = false) {
        if (shouldOpenChooseNetwork && isActiveNetwork) {
            sendEventUseCase.send(Props(EventType.TRACK, EventType.Track.CLICK_NETWORKS))
        }

        navController.openAppKit(
            shouldOpenChooseNetwork = shouldOpenChooseNetwork,
            onError = { logger.error(it) }
        )
    }
}
