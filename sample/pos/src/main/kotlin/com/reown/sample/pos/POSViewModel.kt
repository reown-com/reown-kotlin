package com.reown.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.pos.client.POS
import com.reown.pos.client.POS.Model.PaymentEvent
import com.reown.pos.client.POSClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.net.URI

sealed interface PosNavEvent {
    data object ToStart : PosNavEvent
    data object ToAmount : PosNavEvent
    data object ToSelectToken : PosNavEvent
    data object ToSelectNetwork : PosNavEvent
    data object FlowFinished : PosNavEvent
    data class QrReady(val uri: URI) : PosNavEvent

    data class ToErrorScreen(val error: String) : PosNavEvent
}

sealed interface PosEvent {
    data object Connected : PosEvent
    data object ConnectedRejected : PosEvent
    data object PaymentRequested : PosEvent
    data object PaymentBroadcasted : PosEvent
    data class PaymentRejected(val error: String) : PosEvent
    data class PaymentSuccessful(val txHash: String) : PosEvent
}

enum class Chain(val id: String) {
    ETHEREUM("eip155:1"),
    BASE("eip155:8453"),
    ARBITRUM("eip155:42161"),
    POLYGON("eip155:137"),
    OPTIMISM("eip155:10"),
    SOLANA("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"),
    SEPOLIA("eip155:11155111")
}

enum class StableCoin(val decimals: Int) {
    USDC(6),
    USDT(6),
}

data class Address(
    val standard: String,
    val address: String
)

object ContractAddresses {
    private val ADDRESSES = mapOf(
        Chain.ETHEREUM to mapOf(
            StableCoin.USDT to Address(
                standard = "erc20",
                address = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
            ),
            StableCoin.USDC to Address(
                standard = "erc20",
                address = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
            )
        ),
        Chain.BASE to mapOf(
            StableCoin.USDC to Address(
                standard = "erc20",
                address = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"
            ),
        ),
        Chain.ARBITRUM to mapOf(
            StableCoin.USDC to Address(
                standard = "erc20",
                address = "0xaf88d065e77c8cC2239327C5EDb3A432268e5831"
            ),
            StableCoin.USDT to Address(
                standard = "erc20",
                address = "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9"
            )
        ),
        Chain.OPTIMISM to mapOf(
            StableCoin.USDC to Address(
                standard = "erc20",
                address = "0x0b2C639c533813f4Aa9D7837CAf62653d097Ff85"
            ),
            StableCoin.USDT to Address(
                standard = "erc20",
                address = "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58"
            )
        ),
        Chain.SOLANA to mapOf(
            StableCoin.USDC to Address(
                standard = "token",
                address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
            )
        ),
        Chain.SEPOLIA to mapOf(
            StableCoin.USDC to Address(
                standard = "erc20",
                address = "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238"
            )
        )
    )

    fun getToken(chain: Chain, token: StableCoin): Token {
        val address = ADDRESSES[chain]?.get(token)
            ?: throw IllegalArgumentException("No address found for $token on $chain")
        return Token(
            standard = address.standard,
            symbol = token.name,
            address = address.address
        )
    }
}

data class Token(
    val standard: String,
    val symbol: String,
    val address: String
)

class POSViewModel : ViewModel() {

    private val _posNavEventsFlow: MutableSharedFlow<PosNavEvent> = MutableSharedFlow()
    val posNavEventsFlow = _posNavEventsFlow.asSharedFlow()

    private val _posEventsFlow: MutableSharedFlow<PosEvent> = MutableSharedFlow()
    val posEventsFlow = _posEventsFlow.asSharedFlow()

    internal var token: Token? = null
    internal var tokenSymbol: String? = null
    internal var amount: String? = null
    internal var network: String? = null


    init {
        viewModelScope.launch {
            PosSampleDelegate.paymentEventFlow.collect { paymentEvent ->
                when (paymentEvent) {
                    is PaymentEvent.QrReady -> {
                        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.QrReady(uri = paymentEvent.uri)) }
                    }

                    is PaymentEvent.Connected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.Connected) }
                    }

                    is PaymentEvent.ConnectionFailed -> {
                        viewModelScope.launch {
                            _posNavEventsFlow.emit(
                                PosNavEvent.ToErrorScreen(
                                    error = paymentEvent.error.message ?: "Connection Error"
                                )
                            )
                        }
                    }

                    is PaymentEvent.PaymentRequested -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRequested) }
                    }

                    is PaymentEvent.PaymentBroadcasted -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentBroadcasted) }
                    }

                    is PaymentEvent.PaymentSuccessful -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentSuccessful(paymentEvent.txHash)) }
                    }

                    PaymentEvent.ConnectionRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.ConnectedRejected) }
                    }

                    is PaymentEvent.Error -> {
                        viewModelScope.launch {
                            _posNavEventsFlow.emit(
                                PosNavEvent.ToErrorScreen(
                                    error = paymentEvent.error.message ?: "Connection Error"
                                )
                            )
                        }
                    }

                    is PaymentEvent.PaymentRejected -> {
                        viewModelScope.launch { _posEventsFlow.emit(PosEvent.PaymentRejected(error = paymentEvent.message)) }
                    }
                }
            }
        }
    }

    fun navigateToAmountScreen() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToAmount) }
    }

    fun navigateToTokenScreen(amount: String) {
        this.amount = amount
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToSelectToken) }
    }

    fun navigateToNetworkScreen(tokenSymbol: String) {
        this.tokenSymbol = tokenSymbol
        println("kobe: selected token: $tokenSymbol")

        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToSelectNetwork) }
    }

    fun createPaymentIntent(chainId: String, name: String) {
        try {
            this.network = chainId
            val chain = Chain.entries.firstOrNull { it.id == chainId }
                ?: throw IllegalArgumentException("Unsupported chainId: $chainId")
            val stableCoin = tokenSymbol?.let { StableCoin.valueOf(it) }
                ?: throw IllegalStateException("Token symbol not selected")
            val token = ContractAddresses.getToken(chain, stableCoin)
            this.token = token
            val paymentIntents =
                listOf(
                    POS.Model.PaymentIntent(
                        token = POS.Model.Token(
                            network = POS.Model.Network(
                                chainId = chainId,
                                name = name
                            ),
                            standard = this.token?.standard ?: "",
                            symbol = this.tokenSymbol ?: "",
                            address = this.token?.address ?: ""
                        ),
                        amount = this.amount ?: "",
                        recipient = "${chainId}:0x228311b83dAF3FC9a0D0a46c0B329942fc8Cb2eD" //Hardcoded
                    )
                )

            POSClient.createPaymentIntent(intents = paymentIntents)
        } catch (e: Exception) {
            println("kobe: createPaymentIntent error: ${e.message}")

            viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = e.message ?: "Create intent error")) }
        }
    }
}
