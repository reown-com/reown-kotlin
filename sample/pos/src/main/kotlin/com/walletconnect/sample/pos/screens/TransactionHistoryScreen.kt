package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.TransactionHistoryUiState
import java.text.SimpleDateFormat
import java.util.*

private val BrandColor = Color(0xFF0988F0)

@Composable
fun TransactionHistoryScreen(
    viewModel: POSViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.transactionHistoryState.collectAsState()
    val selectedFilter by viewModel.selectedStatusFilter.collectAsState()
    val listState = rememberLazyListState()

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header with back button
        TransactionHistoryHeader(onBackClick = onBackClick)

        // Status filter chips
        StatusFilterChips(
            selectedStatus = selectedFilter,
            onStatusSelected = { viewModel.setStatusFilter(it) }
        )

        // Content
        when (val state = uiState) {
            TransactionHistoryUiState.Idle,
            TransactionHistoryUiState.Loading -> {
                LoadingContent()
            }

            is TransactionHistoryUiState.Success -> {
                if (state.transactions.isEmpty()) {
                    EmptyStateContent()
                } else {
                    TransactionList(
                        transactions = state.transactions,
                        hasMore = state.hasMore,
                        isLoadingMore = false,
                        listState = listState,
                        stats = state.stats
                    )
                }
            }

            is TransactionHistoryUiState.LoadingMore -> {
                // Show existing list with loading indicator at bottom
                TransactionList(
                    transactions = state.transactions,
                    hasMore = true,
                    isLoadingMore = true,
                    listState = listState,
                    stats = state.stats
                )
            }

            is TransactionHistoryUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshTransactionHistory() }
                )
            }
        }
    }
}

@Composable
private fun TransactionHistoryHeader(onBackClick: () -> Unit) {
    Surface(
        color = BrandColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                "Transaction History",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusFilterChips(
    selectedStatus: Pos.TransactionStatus?,
    onStatusSelected: (Pos.TransactionStatus?) -> Unit
) {
    val filters = listOf(
        null to "All",
        Pos.TransactionStatus.SUCCEEDED to "Succeeded",
        Pos.TransactionStatus.FAILED to "Failed",
        Pos.TransactionStatus.PROCESSING to "Processing",
        Pos.TransactionStatus.EXPIRED to "Expired"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (status, label) ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandColor,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun TransactionList(
    transactions: List<Pos.Transaction>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    stats: Pos.TransactionStats?
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats summary card
        stats?.let { statsData ->
            item {
                StatsCard(stats = statsData)
            }
        }

        items(
            items = transactions,
            key = { it.paymentId }
        ) { transaction ->
            TransactionItem(transaction = transaction)
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = BrandColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (!hasMore && transactions.isNotEmpty()) {
            item {
                Text(
                    "No more transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: Pos.TransactionStats) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BrandColor.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Transactions",
                value = stats.totalTransactions.toString()
            )
            StatItem(
                label = "Customers",
                value = stats.totalCustomers.toString()
            )
            stats.totalRevenue?.let { revenue ->
                StatItem(
                    label = "Revenue",
                    value = revenue.format()
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun TransactionItem(transaction: Pos.Transaction) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top row: Status + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(status = transaction.status)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = transaction.status.displayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = transaction.status.color()
                    )
                }

                // Amount
                transaction.formatFiatAmount()?.let { amount ->
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(Modifier.height(12.dp))

            // Details
            DetailRow("Payment ID", transaction.paymentId.truncateMiddle())

            transaction.txHash?.let { hash ->
                DetailRow("TX Hash", hash.truncateMiddle())
            }

            transaction.network?.let { network ->
                DetailRow("Network", network)
            }

            DetailRow("Wallet", transaction.walletName)

            transaction.createdAt?.let { timestamp ->
                DetailRow("Date", formatTimestamp(timestamp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

@Composable
private fun StatusIcon(status: Pos.TransactionStatus) {
    val (icon, color) = status.iconAndColor()
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp)
    )
}

private fun Pos.TransactionStatus.iconAndColor(): Pair<ImageVector, Color> {
    return when (this) {
        Pos.TransactionStatus.SUCCEEDED -> Icons.Filled.CheckCircle to Color(0xFF4CAF50)
        Pos.TransactionStatus.FAILED, Pos.TransactionStatus.EXPIRED -> Icons.Filled.Warning to Color(0xFFF44336)
        Pos.TransactionStatus.PROCESSING, Pos.TransactionStatus.REQUIRES_ACTION -> Icons.Filled.Refresh to Color(0xFFFF9800)
        Pos.TransactionStatus.UNKNOWN -> Icons.Filled.Refresh to Color.Gray
    }
}

private fun Pos.TransactionStatus.displayName(): String = when (this) {
    Pos.TransactionStatus.SUCCEEDED -> "Completed"
    Pos.TransactionStatus.FAILED -> "Failed"
    Pos.TransactionStatus.EXPIRED -> "Expired"
    Pos.TransactionStatus.PROCESSING -> "Processing"
    Pos.TransactionStatus.REQUIRES_ACTION -> "Pending"
    Pos.TransactionStatus.UNKNOWN -> "Unknown"
}

private fun Pos.TransactionStatus.color(): Color = when (this) {
    Pos.TransactionStatus.SUCCEEDED -> Color(0xFF4CAF50)
    Pos.TransactionStatus.FAILED, Pos.TransactionStatus.EXPIRED -> Color(0xFFF44336)
    Pos.TransactionStatus.PROCESSING, Pos.TransactionStatus.REQUIRES_ACTION -> Color(0xFFFF9800)
    Pos.TransactionStatus.UNKNOWN -> Color.Gray
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        // Use Locale.ROOT for parsing ISO 8601 dates to avoid locale-specific parsing issues
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timestamp.substringBefore(".").substringBefore("Z"))
        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp
    }
}

private fun String.truncateMiddle(startChars: Int = 10, endChars: Int = 6): String {
    return if (length <= startChars + endChars + 3) {
        this
    } else {
        "${take(startChars)}...${takeLast(endChars)}"
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = BrandColor)
    }
}

@Composable
private fun EmptyStateContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No Transactions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your transaction history will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFFF44336),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Failed to load transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandColor)
        ) {
            Text("Retry")
        }
    }
}
