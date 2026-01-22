package com.walletconnect.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.pos.Pos
import com.walletconnect.pos.PosClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI

sealed interface PosNavEvent {
    data object ToStart : PosNavEvent
    data object ToAmount : PosNavEvent
    data object FlowFinished : PosNavEvent
    data class QrReady(val uri: URI, val amount: Pos.Amount, val paymentId: String) : PosNavEvent
    data class ToErrorScreen(val error: String) : PosNavEvent
    data class PaymentSuccessScreen(val paymentId: String) : PosNavEvent
    data object ToTransactionHistory : PosNavEvent
}

sealed interface PosEvent {
    data object PaymentRequested : PosEvent
    data object PaymentProcessing : PosEvent
    data class PaymentSuccess(val paymentId: String) : PosEvent
    data class PaymentError(val error: String) : PosEvent
}

sealed interface TransactionHistoryUiState {
    data object Idle : TransactionHistoryUiState
    data object Loading : TransactionHistoryUiState
    data object LoadingMore : TransactionHistoryUiState
    data class Success(
        val transactions: List<Pos.Transaction>,
        val hasMore: Boolean,
        val stats: Pos.TransactionStats?
    ) : TransactionHistoryUiState
    data class Error(val message: String) : TransactionHistoryUiState
}

class POSViewModel : ViewModel() {

    private val _posNavEventsFlow: MutableSharedFlow<PosNavEvent> = MutableSharedFlow()
    val posNavEventsFlow = _posNavEventsFlow.asSharedFlow()

    private val _posEventsFlow: MutableSharedFlow<PosEvent> = MutableSharedFlow()
    val posEventsFlow = _posEventsFlow.asSharedFlow()

    // Current payment info
    private var currentAmount: Pos.Amount? = null
    private var currentPaymentId: String? = null

    // Loading state for "Start Payment" button
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Transaction history state
    private val _transactionHistoryState = MutableStateFlow<TransactionHistoryUiState>(TransactionHistoryUiState.Idle)
    val transactionHistoryState = _transactionHistoryState.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<Pos.TransactionStatus?>(null)
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private var currentCursor: String? = null
    private val loadedTransactions = mutableListOf<Pos.Transaction>()
    private var currentStats: Pos.TransactionStats? = null

    init {
        viewModelScope.launch {
            PosSampleDelegate.paymentEventFlow.collect { paymentEvent ->
                handlePaymentEvent(paymentEvent)
            }
        }
    }

    private suspend fun handlePaymentEvent(paymentEvent: Pos.PaymentEvent) {
        when (paymentEvent) {
            is Pos.PaymentEvent.PaymentCreated -> {
                _isLoading.value = false
                currentAmount = paymentEvent.amount
                currentPaymentId = paymentEvent.paymentId
                _posNavEventsFlow.emit(
                    PosNavEvent.QrReady(
                        uri = paymentEvent.uri,
                        amount = paymentEvent.amount,
                        paymentId = paymentEvent.paymentId
                    )
                )
            }

            is Pos.PaymentEvent.PaymentRequested -> {
                _posEventsFlow.emit(PosEvent.PaymentRequested)
            }

            is Pos.PaymentEvent.PaymentProcessing -> {
                _posEventsFlow.emit(PosEvent.PaymentProcessing)
            }

            is Pos.PaymentEvent.PaymentSuccess -> {
                _posEventsFlow.emit(PosEvent.PaymentSuccess(paymentEvent.paymentId))
                _posNavEventsFlow.emit(PosNavEvent.PaymentSuccessScreen(paymentEvent.paymentId))
            }

            is Pos.PaymentEvent.PaymentError -> {
                _isLoading.value = false
                val errorMessage = when (val error: Pos.PaymentEvent.PaymentError = paymentEvent) {
                    is Pos.PaymentEvent.PaymentError.CreatePaymentFailed -> "Failed to create payment, try again: ${error.message}"
                    is Pos.PaymentEvent.PaymentError.PaymentFailed -> "Payment failed, try again: ${error.message}"
                    is Pos.PaymentEvent.PaymentError.PaymentNotFound -> "Payment not found, try again: ${error.message}"
                    is Pos.PaymentEvent.PaymentError.PaymentExpired -> "Payment expired, try again: ${error.message}"
                    is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest -> "Invalid request, try again: ${error.message}"
                    is Pos.PaymentEvent.PaymentError.Undefined -> "Undefined Error, try again: ${error.message}"
                }
                _posEventsFlow.emit(PosEvent.PaymentError(errorMessage))
                _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = errorMessage))
            }
        }
    }

    fun navigateToAmountScreen() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToAmount) }
    }

    /**
     * Creates a payment intent with the specified amount.
     *
     * @param amountValue Amount in minor units (cents for USD)
     * @param currency Currency code (e.g., "USD", "EUR")
     */
    fun createPayment(amountValue: String, currency: String = "USD") {
        try {
            val referenceId = "ORDER-${System.currentTimeMillis()}"
            _isLoading.value = true

            PosClient.createPaymentIntent(
                amount = Pos.Amount(
                    unit = "iso4217/$currency",
                    value = amountValue
                ),
                referenceId = referenceId
            )
        } catch (e: Exception) {
            _isLoading.value = false
            viewModelScope.launch {
                _posNavEventsFlow.emit(
                    PosNavEvent.ToErrorScreen(error = e.message ?: "Create payment error")
                )
            }
        }
    }

    fun cancelPayment() {
        PosClient.cancelPayment()
        _isLoading.value = false
    }

    fun resetForNewPayment() {
        currentAmount = null
        currentPaymentId = null
        _isLoading.value = false
    }

    fun getDisplayAmount(): String {
        val amount = currentAmount ?: return ""
        val valueInCents = amount.value.toLongOrNull() ?: 0L
        val dollars = valueInCents / 100.0
        val currency = amount.unit.substringAfter("/", "USD")
        return String.format("$%.2f %s", dollars, currency)
    }

    // Transaction History Methods

    fun navigateToTransactionHistory() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToTransactionHistory) }
        loadTransactionHistory(refresh = true)
    }

    fun loadTransactionHistory(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                currentCursor = null
                loadedTransactions.clear()
                currentStats = null
                _transactionHistoryState.value = TransactionHistoryUiState.Loading
            } else {
                _transactionHistoryState.value = TransactionHistoryUiState.LoadingMore
            }

            val result = PosClient.getTransactionHistory(
                limit = 20,
                cursor = currentCursor,
                status = _selectedStatusFilter.value
            )

            result.fold(
                onSuccess = { historyResult ->
                    loadedTransactions.addAll(historyResult.transactions)
                    currentCursor = historyResult.nextCursor
                    // Only update stats on first load (refresh)
                    if (refresh) {
                        currentStats = historyResult.stats
                    }
                    _transactionHistoryState.value = TransactionHistoryUiState.Success(
                        transactions = loadedTransactions.toList(),
                        hasMore = historyResult.hasMore,
                        stats = currentStats
                    )
                },
                onFailure = { error ->
                    _transactionHistoryState.value = TransactionHistoryUiState.Error(
                        error.message ?: "Failed to load transactions"
                    )
                }
            )
        }
    }

    fun loadMoreTransactions() {
        val state = _transactionHistoryState.value
        if (state is TransactionHistoryUiState.Success && state.hasMore) {
            loadTransactionHistory(refresh = false)
        }
    }

    fun setStatusFilter(status: Pos.TransactionStatus?) {
        _selectedStatusFilter.value = status
        loadTransactionHistory(refresh = true)
    }

    fun refreshTransactionHistory() {
        loadTransactionHistory(refresh = true)
    }
}
