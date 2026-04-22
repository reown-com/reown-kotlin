package com.walletconnect.sample.pos

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.pos.Pos
import com.walletconnect.pos.PosClient
import com.walletconnect.sample.pos.log.PosLogStore
import com.walletconnect.sample.pos.nfc.NfcManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import com.walletconnect.sample.pos.components.TransactionFilter
import com.walletconnect.sample.pos.credentials.MerchantCredentialsManager
import com.walletconnect.sample.pos.model.Currency
import com.walletconnect.sample.pos.model.PosVariant
import com.walletconnect.sample.pos.model.ThemeMode
import com.walletconnect.sample.pos.model.formatAmountWithSymbol
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
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

sealed interface PinFlowState {
    data object Hidden : PinFlowState
    data class SetNew(val firstPin: String? = null, val pendingAction: PendingCredentialSave) : PinFlowState
    data class Verify(val pendingAction: PendingCredentialSave) : PinFlowState
    data class Error(val message: String, val previousState: PinFlowState) : PinFlowState
}

data class PendingCredentialSave(
    val merchantId: String? = null,
    val apiKey: String? = null
)

class POSViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    internal val credentialsManager = MerchantCredentialsManager(application)

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

    // Selected currency (persisted)
    private val _selectedCurrency = MutableStateFlow(
        prefs.getString(KEY_CURRENCY, null)?.let { Currency.fromCode(it) } ?: Currency.USD
    )
    val selectedCurrency = _selectedCurrency.asStateFlow()

    fun setCurrency(currency: Currency) {
        _selectedCurrency.value = currency
        prefs.edit().putString(KEY_CURRENCY, currency.code).apply()
    }

    // Selected theme mode (persisted)
    private val _selectedThemeMode = MutableStateFlow(
        prefs.getString(KEY_THEME, null)?.let { name ->
            ThemeMode.entries.find { it.name == name }
        } ?: ThemeMode.SYSTEM
    )
    val selectedThemeMode = _selectedThemeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _selectedThemeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    // Selected variant (persisted)
    private val _selectedVariant = MutableStateFlow(
        prefs.getString(KEY_VARIANT, null)?.let { name ->
            PosVariant.entries.find { it.name == name }
        } ?: PosVariant.DEFAULT
    )
    val selectedVariant = _selectedVariant.asStateFlow()

    fun setVariant(variant: PosVariant) {
        _selectedVariant.value = variant
        prefs.edit().putString(KEY_VARIANT, variant.name).apply()
        variant.defaultTheme?.let { setThemeMode(it) }
    }

    // Merchant credentials (persisted securely)
    private val _merchantId = MutableStateFlow(credentialsManager.getMerchantId())
    val merchantId = _merchantId.asStateFlow()

    private val _hasApiKey = MutableStateFlow(credentialsManager.hasApiKey())
    val hasApiKey = _hasApiKey.asStateFlow()

    // PIN flow state
    private val _pinFlowState = MutableStateFlow<PinFlowState>(PinFlowState.Hidden)
    val pinFlowState = _pinFlowState.asStateFlow()

    fun requestSaveMerchantId(value: String) {
        val pending = PendingCredentialSave(merchantId = value)
        if (credentialsManager.isPinSet()) {
            _pinFlowState.value = PinFlowState.Verify(pending)
        } else {
            _pinFlowState.value = PinFlowState.SetNew(pendingAction = pending)
        }
    }

    fun requestSaveApiKey(value: String) {
        val pending = PendingCredentialSave(apiKey = value)
        if (credentialsManager.isPinSet()) {
            _pinFlowState.value = PinFlowState.Verify(pending)
        } else {
            _pinFlowState.value = PinFlowState.SetNew(pendingAction = pending)
        }
    }

    fun onPinEntered(pin: String) {
        when (val state = _pinFlowState.value) {
            is PinFlowState.SetNew -> {
                if (state.firstPin == null) {
                    // First entry — move to confirm step
                    _pinFlowState.value = state.copy(firstPin = pin)
                } else {
                    // Confirm step
                    if (pin == state.firstPin) {
                        credentialsManager.setPin(pin)
                        executePendingSave(state.pendingAction)
                        _pinFlowState.value = PinFlowState.Hidden
                    } else {
                        _pinFlowState.value = PinFlowState.Error("PINs don't match", state.copy(firstPin = null))
                    }
                }
            }
            is PinFlowState.Verify -> {
                if (credentialsManager.verifyPin(pin)) {
                    executePendingSave(state.pendingAction)
                    _pinFlowState.value = PinFlowState.Hidden
                } else {
                    _pinFlowState.value = PinFlowState.Error("Incorrect PIN", state)
                }
            }
            is PinFlowState.Error -> {
                // Re-process with the underlying state
                _pinFlowState.value = state.previousState
                onPinEntered(pin)
            }
            is PinFlowState.Hidden -> { /* no-op */ }
        }
    }

    fun cancelPinFlow() {
        _pinFlowState.value = PinFlowState.Hidden
    }

    private fun executePendingSave(pending: PendingCredentialSave) {
        pending.merchantId?.let {
            credentialsManager.saveMerchantId(it)
            _merchantId.value = it
        }
        pending.apiKey?.let {
            credentialsManager.saveApiKey(it)
            _hasApiKey.value = true
        }
        reinitializePosClient()
    }

    private fun reinitializePosClient() {
        val apiKey = credentialsManager.getApiKey()
        val merchantId = credentialsManager.getMerchantId()
        if (apiKey.isBlank() || merchantId.isBlank()) return

        val deviceId = credentialsManager.getDeviceId()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                PosClient.init(apiKey = apiKey, merchantId = merchantId, deviceId = deviceId, mtlsConfig = Pos.MtlsConfig.Disabled)
                PosClient.setDelegate(PosSampleDelegate)
                Timber.d("PosClient re-initialized with updated credentials")
            } catch (e: Exception) {
                Timber.e(e, "PosClient re-initialization failed")
            }
        }
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
                NfcManager.updatePaymentUri(paymentEvent.uri.toString())
                PosLogStore.info(
                    "Payment created",
                    source = "handlePaymentEvent",
                    data = "paymentId: ${paymentEvent.paymentId}\namount: ${paymentEvent.amount.value} ${paymentEvent.amount.unit}\ngatewayUrl: ${paymentEvent.uri}"
                )
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
                PosLogStore.info("Payment requested by wallet", source = "handlePaymentEvent")
                _posEventsFlow.emit(PosEvent.PaymentRequested)
            }

            is Pos.PaymentEvent.PaymentProcessing -> {
                PosLogStore.info("Payment processing", source = "handlePaymentEvent")
                _posEventsFlow.emit(PosEvent.PaymentProcessing)
            }

            is Pos.PaymentEvent.PaymentSuccess -> {
                PosClient.cancelPayment()
                PosLogStore.info(
                    "Payment success",
                    source = "handlePaymentEvent",
                    data = "paymentId: ${paymentEvent.paymentId}"
                )
                _posEventsFlow.emit(PosEvent.PaymentSuccess(paymentEvent.paymentId, paymentEvent.info))
                _posNavEventsFlow.emit(PosNavEvent.PaymentSuccessScreen(paymentEvent.paymentId, paymentEvent.info, _currentAmount.value))
            }

            is Pos.PaymentEvent.PaymentError -> {
                PosClient.cancelPayment()
                _isLoading.value = false
                val errorCode = when (paymentEvent) {
                    is Pos.PaymentEvent.PaymentError.PaymentExpired -> "expired"
                    is Pos.PaymentEvent.PaymentError.CreatePaymentFailed -> "create_failed"
                    is Pos.PaymentEvent.PaymentError.PaymentFailed -> "failed"
                    is Pos.PaymentEvent.PaymentError.PaymentCancelled -> "cancelled"
                    is Pos.PaymentEvent.PaymentError.PaymentNotFound -> "not_found"
                    is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest -> "invalid_request"
                    is Pos.PaymentEvent.PaymentError.SanctionedUser -> "sanctioned_user"
                    is Pos.PaymentEvent.PaymentError.Undefined -> "unknown"
                }
                PosLogStore.error(
                    "Payment error: $errorCode",
                    source = "handlePaymentEvent",
                    data = paymentEvent.toString()
                )
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

            PosLogStore.info(
                "Creating payment",
                source = "createPayment",
                data = "amount: $amountValue ${currency.unit}\nreferenceId: $referenceId"
            )

            PosClient.createPaymentIntent(
                amount = Pos.Amount(
                    unit = currency.unit,
                    value = amountValue
                ),
                referenceId = referenceId
            )
        } catch (e: Exception) {
            _isLoading.value = false
            PosLogStore.error(
                "Create payment failed",
                source = "createPayment",
                data = e.message
            )
            viewModelScope.launch {
                _posNavEventsFlow.emit(
                    PosNavEvent.ToErrorScreen(error = e.message ?: "Create payment error")
                )
            }
        }
    }

    fun cancelPayment() {
        PosLogStore.info("Payment cancelled", source = "cancelPayment")
        NfcManager.clearPaymentUri()
        PosClient.cancelPayment()
        _isLoading.value = false
    }

    fun stopPolling(): String? {
        _isLoading.value = false
        return PosClient.stopPolling()
    }

    fun resetForNewPayment() {
        NfcManager.clearPaymentUri()
        PosClient.cancelPayment()
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

    companion object {
        private const val PREFS_NAME = "pos_settings"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_THEME = "theme"
        private const val KEY_VARIANT = "variant"
    }
}
