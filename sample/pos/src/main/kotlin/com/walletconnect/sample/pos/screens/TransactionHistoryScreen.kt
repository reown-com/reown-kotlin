package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCBorderRadius
import com.walletconnect.sample.pos.ui.theme.WCTheme
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.TransactionHistoryUiState
import com.walletconnect.sample.pos.model.Currency
import com.walletconnect.sample.pos.model.formatAmountWithSymbol
import com.walletconnect.sample.pos.components.BottomSheetHeader
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.components.SelectableOptionItem
import com.walletconnect.sample.pos.components.TransactionCard
import com.walletconnect.sample.pos.components.TransactionFilter
import kotlinx.coroutines.launch

private enum class ActivitySheet { STATUS, DATE_RANGE, TRANSACTION_DETAIL }

private val dateRangeOptions = listOf("All Time", "Today", "7 Days", "This Week", "This Month")

private val SheetShape = RoundedCornerShape(topStart = WCBorderRadius.radius8, topEnd = WCBorderRadius.radius8)
private val FilterButtonShape = RoundedCornerShape(WCBorderRadius.radius4)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: POSViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.transactionHistoryState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedDateRangeOptionIndex by viewModel.selectedDateRangeOptionIndex.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedTransaction by remember { mutableStateOf<Pos.Transaction?>(null) }
    var activeSheet by remember { mutableStateOf(ActivitySheet.STATUS) }
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    // Load more when reaching end of list
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState is TransactionHistoryUiState.Success) {
            viewModel.loadMoreTransactions()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = SheetShape,
        sheetBackgroundColor = WCTheme.colors.bgPrimary,
        sheetElevation = 4.dp,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetContent = {
            when (activeSheet) {
                ActivitySheet.STATUS -> StatusFilterBottomSheet(
                    selectedFilter = selectedFilter,
                    onSelect = { filter ->
                        viewModel.setFilter(filter)
                        scope.launch { sheetState.hide() }
                    },
                    onDismiss = { scope.launch { sheetState.hide() } }
                )
                ActivitySheet.DATE_RANGE -> DateRangeFilterBottomSheet(
                    selectedOptionIndex = selectedDateRangeOptionIndex,
                    onSelect = { index ->
                        viewModel.setDateRangeOption(index)
                        scope.launch { sheetState.hide() }
                    },
                    onDismiss = { scope.launch { sheetState.hide() } }
                )
                ActivitySheet.TRANSACTION_DETAIL -> {
                    if (selectedTransaction != null) {
                        com.walletconnect.sample.pos.components.TransactionDetailContent(
                            transaction = selectedTransaction!!,
                            onClose = { scope.launch { sheetState.hide() } }
                        )
                    } else {
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WCTheme.colors.bgPrimary)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            PosHeader(onBack = onClose)

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // Filter buttons
            Row(
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5),
                horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
            ) {
                FilterButton(
                    label = if (selectedFilter == TransactionFilter.ALL) "Status" else selectedFilter.label,
                    onClick = {
                        activeSheet = ActivitySheet.STATUS
                        scope.launch { sheetState.show() }
                    }
                )
                FilterButton(
                    label = if (selectedDateRangeOptionIndex == 0) "Date range" else dateRangeOptions[selectedDateRangeOptionIndex],
                    onClick = {
                        activeSheet = ActivitySheet.DATE_RANGE
                        scope.launch { sheetState.show() }
                    }
                )
            }

            // Total amount summary
            val currentState = uiState
            val stats = when (currentState) {
                is TransactionHistoryUiState.Success -> currentState.stats
                is TransactionHistoryUiState.LoadingMore -> currentState.stats
                else -> null
            }
            if (stats != null) {
                TotalAmountSummary(stats = stats)
            } else {
                Spacer(Modifier.height(WCTheme.spacing.spacing3))
            }

            // Content
            when (val state = uiState) {
                TransactionHistoryUiState.Idle,
                TransactionHistoryUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = WCTheme.colors.bgAccentPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                is TransactionHistoryUiState.Success -> {
                    if (state.transactions.isEmpty()) {
                        EmptyState(modifier = Modifier.weight(1f))
                    } else {
                        TransactionList(
                            transactions = state.transactions,
                            isLoadingMore = false,
                            listState = listState,
                            onTransactionClick = { tx ->
                                selectedTransaction = tx
                                activeSheet = ActivitySheet.TRANSACTION_DETAIL
                                scope.launch { sheetState.show() }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is TransactionHistoryUiState.LoadingMore -> {
                    TransactionList(
                        transactions = state.transactions,
                        isLoadingMore = true,
                        listState = listState,
                        onTransactionClick = { tx ->
                            selectedTransaction = tx
                            activeSheet = ActivitySheet.TRANSACTION_DETAIL
                            scope.launch { sheetState.show() }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                is TransactionHistoryUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refreshTransactionHistory() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Close button at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WCTheme.spacing.spacing4),
                contentAlignment = Alignment.Center
            ) {
                CloseButton(onClick = onClose)
            }
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(FilterButtonShape)
            .background(WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing5, vertical = WCTheme.spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary
        )
        Icon(
            painter = painterResource(R.drawable.ic_caret_up_down),
            contentDescription = null,
            tint = WCTheme.colors.iconDefault,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun TotalAmountSummary(stats: Pos.TransactionStats) {
    val revenue = stats.totalRevenue
    val formattedAmount = if (revenue != null) {
        val currency = Currency.fromCode(revenue.currency)
        formatAmountWithSymbol(String.format(java.util.Locale.US, "%.2f", revenue.amount), currency)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WCTheme.spacing.spacing5, vertical = WCTheme.spacing.spacing3)
    ) {
        if (formattedAmount != null) {
            Text(
                text = formattedAmount,
                style = WCTheme.typography.h4Medium,
                color = WCTheme.colors.textPrimary
            )
        }
        Text(
            text = "${stats.totalTransactions} transaction${if (stats.totalTransactions != 1) "s" else ""}",
            style = WCTheme.typography.bodySmRegular,
            color = WCTheme.colors.textSecondary
        )
    }
}

@Composable
private fun StatusFilterBottomSheet(
    selectedFilter: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        BottomSheetHeader(title = "Status", onDismiss = onDismiss)

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        TransactionFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val dotColor: Color? = when (filter) {
                TransactionFilter.COMPLETED -> WCTheme.colors.iconSuccess
                TransactionFilter.FAILED, TransactionFilter.CANCELLED, TransactionFilter.EXPIRED -> WCTheme.colors.iconError
                TransactionFilter.PENDING -> WCTheme.colors.foregroundTertiary
                TransactionFilter.ALL -> null
            }
            SelectableOptionItem(
                label = filter.label,
                isSelected = isSelected,
                onClick = { onSelect(filter) },
                leadingIcon = dotColor?.let { color ->
                    {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                    }
                }
            )
            if (filter != TransactionFilter.entries.last()) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun DateRangeFilterBottomSheet(
    selectedOptionIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        BottomSheetHeader(title = "Date range", onDismiss = onDismiss)

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        dateRangeOptions.forEachIndexed { index, label ->
            val isSelected = index == selectedOptionIndex
            SelectableOptionItem(
                label = label,
                isSelected = isSelected,
                onClick = { onSelect(index) }
            )
            if (index != dateRangeOptions.lastIndex) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun TransactionList(
    transactions: List<Pos.Transaction>,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTransactionClick: (Pos.Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = WCTheme.spacing.spacing5),
        verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        items(
            items = transactions,
            key = { it.paymentId }
        ) { transaction ->
            TransactionCard(
                transaction = transaction,
                onClick = { onTransactionClick(transaction) }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WCTheme.spacing.spacing4),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = WCTheme.colors.bgAccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No activity yet",
            style = WCTheme.typography.h6Medium,
            color = WCTheme.colors.textPrimary
        )
        Spacer(Modifier.height(WCTheme.spacing.spacing2))
        Text(
            text = "Your transaction history will appear here",
            style = WCTheme.typography.bodyMdRegular,
            color = WCTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load transactions",
            style = WCTheme.typography.h6Medium,
            color = WCTheme.colors.textPrimary
        )
        Spacer(Modifier.height(WCTheme.spacing.spacing2))
        Text(
            text = message,
            style = WCTheme.typography.bodyMdRegular,
            color = WCTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(WCTheme.spacing.spacing4))
        Box(
            modifier = Modifier
                .clickable(onClick = onRetry)
                .background(WCTheme.colors.bgAccentPrimary, WCTheme.borderRadius.shapeMedium)
                .padding(
                    horizontal = WCTheme.spacing.spacing5,
                    vertical = WCTheme.spacing.spacing2
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Retry",
                style = WCTheme.typography.bodyMdMedium,
                color = WCTheme.colors.textInvert
            )
        }
    }
}
