package com.walletconnect.sample.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.pos.Pos
import com.walletconnect.pos.PosClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.walletconnect.sample.pos.components.TransactionFilter
import com.walletconnect.sample.pos.model.Currency
import com.walletconnect.sample.pos.model.formatAmountWithSymbol
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URI

sealed interface PosNavEvent {
    data object ToStart : PosNavEvent
    data object ToAmount : PosNavEvent
    data object FlowFinished : PosNavEvent
    data class QrReady(val uri: URI, val amount: Pos.Amount, val paymentId: String, val expiresAt: Long) : PosNavEvent
    data class ToErrorScreen(val error: String) : PosNavEvent
    data class PaymentSuccessScreen(val paymentId: String, val info: Pos.PaymentInfo?, val amount: Pos.Amount?) : PosNavEvent
    data object ToTransactionHistory : PosNavEvent
    data object ToSettings : PosNavEvent
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
    private val _currentAmount = MutableStateFlow<Pos.Amount?>(null)
    private var currentPaymentId: String? = null

    // Last successful payment info for success screen
    var lastPaymentInfo: Pos.PaymentInfo? = null
        private set

    fun storeLastPaymentInfo(info: Pos.PaymentInfo?) {
        lastPaymentInfo = info
    }

    // Selected currency
    private val _selectedCurrency = MutableStateFlow(Currency.USD)
    val selectedCurrency = _selectedCurrency.asStateFlow()

    fun setCurrency(currency: Currency) {
        _selectedCurrency.value = currency
    }

    // Loading state for "Start Payment" button
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Transaction history state
    private val _transactionHistoryState = MutableStateFlow<TransactionHistoryUiState>(TransactionHistoryUiState.Idle)
    val transactionHistoryState = _transactionHistoryState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

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
                _currentAmount.value = paymentEvent.amount
                currentPaymentId = paymentEvent.paymentId
                _posNavEventsFlow.emit(
                    PosNavEvent.QrReady(
                        uri = paymentEvent.uri,
                        amount = paymentEvent.amount,
                        paymentId = paymentEvent.paymentId,
                        expiresAt = paymentEvent.expiresAt
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
                _posNavEventsFlow.emit(PosNavEvent.PaymentSuccessScreen(paymentEvent.paymentId, paymentEvent.info, _currentAmount.value))
            }

            is Pos.PaymentEvent.PaymentError -> {
                _isLoading.value = false
                val errorCode = when (paymentEvent) {
                    is Pos.PaymentEvent.PaymentError.PaymentExpired -> "expired"
                    is Pos.PaymentEvent.PaymentError.CreatePaymentFailed -> "create_failed"
                    is Pos.PaymentEvent.PaymentError.PaymentFailed -> "failed"
                    is Pos.PaymentEvent.PaymentError.PaymentCancelled -> "cancelled"
                    is Pos.PaymentEvent.PaymentError.PaymentNotFound -> "not_found"
                    is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest -> "invalid_request"
                    is Pos.PaymentEvent.PaymentError.Undefined -> "unknown"
                }
                _posEventsFlow.emit(PosEvent.PaymentError(errorCode))
                _posNavEventsFlow.emit(PosNavEvent.ToErrorScreen(error = errorCode))
            }
        }
    }

    fun navigateToAmountScreen() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToAmount) }
    }

    fun navigateToSettings() {
        viewModelScope.launch { _posNavEventsFlow.emit(PosNavEvent.ToSettings) }
    }

    /**
     * Creates a payment intent with the specified amount.
     *
     * @param amountValue Amount in minor units (cents for USD)
     * @param currency Currency code (e.g., "USD", "EUR")
     */
    fun createPayment(amountValue: String) {
        try {
            val currency = _selectedCurrency.value
            val referenceId = "ORDER-${System.currentTimeMillis()}"
            _isLoading.value = true

            PosClient.createPaymentIntent(
                amount = Pos.Amount(
                    unit = currency.unit,
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

    fun printReceipt() {
        // TODO: Implement receipt printing via POS terminal SDK
    }

    fun resetForNewPayment() {
        _currentAmount.value = null
        currentPaymentId = null
        _isLoading.value = false
    }

    val displayAmount = _currentAmount.map { amount ->
        if (amount == null) return@map ""
        val valueInCents = amount.value.toLongOrNull() ?: 0L
        val majorUnits = valueInCents / 100.0
        val currencyCode = amount.unit.substringAfter("/", "USD")
        val currency = Currency.fromCode(currencyCode)
        formatAmountWithSymbol(String.format(java.util.Locale.US, "%.2f", majorUnits), currency)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

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
                statuses = _selectedFilter.value.statuses,
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

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
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
