package com.walletconnect.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.pos.Pos
import com.walletconnect.pos.PosClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    data class PaymentSuccessScreen(val paymentId: String, val info: Pos.PaymentInfo?) : PosNavEvent
    data object ToTransactionHistory : PosNavEvent
}

sealed interface PosEvent {
    data object PaymentRequested : PosEvent
    data object PaymentProcessing : PosEvent
    data class PaymentSuccess(val paymentId: String, val info: Pos.PaymentInfo?) : PosEvent
    data class PaymentError(val error: String) : PosEvent
}

sealed interface TransactionHistoryUiState {
    data object Idle : TransactionHistoryUiState
    data object Loading : TransactionHistoryUiState
    data class LoadingMore(
        val transactions: List<Pos.Transaction>,
        val stats: Pos.TransactionStats?
    ) : TransactionHistoryUiState
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

    // Store selected date range option index (not the computed DateRange) to avoid comparison issues
    // Index mapping: 0=All Time, 1=Today, 2=Last 7 Days, 3=This Week, 4=This Month
    private val _selectedDateRangeOptionIndex = MutableStateFlow(1) // Default to "Today" (index 1)
    val selectedDateRangeOptionIndex = _selectedDateRangeOptionIndex.asStateFlow()

    private var currentCursor: String? = null
    private val loadedTransactions = mutableListOf<Pos.Transaction>()
    private var currentStats: Pos.TransactionStats? = null
    private var transactionLoadingJob: Job? = null

    /**
     * Computes the DateRange from the selected option index.
     * Index mapping: 0=All Time, 1=Today, 2=Last 7 Days, 3=This Week, 4=This Month
     */
    private fun getDateRangeForOptionIndex(index: Int): Pos.DateRange? {
        return when (index) {
            0 -> null // All Time
            1 -> Pos.DateRanges.today()
            2 -> Pos.DateRanges.lastDays(7)
            3 -> Pos.DateRanges.thisWeek()
            4 -> Pos.DateRanges.thisMonth()
            else -> null
        }
    }

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
                _posEventsFlow.emit(PosEvent.PaymentSuccess(paymentEvent.paymentId, paymentEvent.info))
                _posNavEventsFlow.emit(PosNavEvent.PaymentSuccessScreen(paymentEvent.paymentId, paymentEvent.info))
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
        // Cancel any in-flight request when starting a new one
        transactionLoadingJob?.cancel()
        transactionLoadingJob = viewModelScope.launch {
            if (refresh) {
                currentCursor = null
                loadedTransactions.clear()
                currentStats = null
                _transactionHistoryState.value = TransactionHistoryUiState.Loading
            } else {
                _transactionHistoryState.value = TransactionHistoryUiState.LoadingMore(
                    transactions = loadedTransactions.toList(),
                    stats = currentStats
                )
            }

            val result = PosClient.getTransactionHistory(
                limit = 20,
                cursor = currentCursor,
                status = _selectedStatusFilter.value,
                dateRange = getDateRangeForOptionIndex(_selectedDateRangeOptionIndex.value)
            )

            result.fold(
                onSuccess = { historyResult ->
                    // Build updated list immutably to avoid race conditions
                    val updatedList = if (refresh) {
                        historyResult.transactions
                    } else {
                        loadedTransactions + historyResult.transactions
                    }
                    loadedTransactions.clear()
                    loadedTransactions.addAll(updatedList)
                    currentCursor = historyResult.nextCursor
                    // Only update stats on first load (refresh)
                    if (refresh) {
                        currentStats = historyResult.stats
                    }
                    _transactionHistoryState.value = TransactionHistoryUiState.Success(
                        transactions = updatedList,
                        hasMore = historyResult.hasMore,
                        stats = currentStats
                    )
                },
                onFailure = { error ->
                    // Propagate cancellation instead of showing error
                    if (error is CancellationException) throw error
                    _transactionHistoryState.value = TransactionHistoryUiState.Error(
                        error.message ?: "Failed to load transactions"
                    )
                }
            )
        }
    }

    fun loadMoreTransactions() {
        val state = _transactionHistoryState.value
        // Guard against concurrent loading - only load more if in Success state with more items
        if (state is TransactionHistoryUiState.Success && state.hasMore) {
            loadTransactionHistory(refresh = false)
        }
    }

    fun setStatusFilter(status: Pos.TransactionStatus?) {
        _selectedStatusFilter.value = status
        loadTransactionHistory(refresh = true)
    }

    fun setDateRangeOption(optionIndex: Int) {
        _selectedDateRangeOptionIndex.value = optionIndex
        loadTransactionHistory(refresh = true)
    }

    fun refreshTransactionHistory() {
        loadTransactionHistory(refresh = true)
    }
}
