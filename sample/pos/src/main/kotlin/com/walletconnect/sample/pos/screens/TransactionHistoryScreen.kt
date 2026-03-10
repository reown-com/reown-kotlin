package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.TransactionHistoryUiState
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.DateRangeSelector
import com.walletconnect.sample.pos.components.FilterTabs
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.components.TransactionCard
import com.walletconnect.sample.pos.components.TransactionDetailSheet
import kotlinx.coroutines.launch

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

    TransactionDetailSheet(
        sheetState = sheetState,
        transaction = selectedTransaction
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

            // Filter tabs (status)
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // Date range selector
            DateRangeSelector(
                selectedOptionIndex = selectedDateRangeOptionIndex,
                onOptionSelected = { viewModel.setDateRangeOption(it) }
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing3))

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
