package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCBorderRadius
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.pos.Pos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = WCBorderRadius.radius8, topEnd = WCBorderRadius.radius8)
private val CardShape = RoundedCornerShape(WCBorderRadius.radius5)
private val CloseButtonShape = RoundedCornerShape(WCBorderRadius.radius3)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionDetailSheet(
    sheetState: ModalBottomSheetState,
    transaction: Pos.Transaction?,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = SheetShape,
        sheetBackgroundColor = WCTheme.colors.bgPrimary,
        sheetContent = {
            if (transaction != null) {
                TransactionDetailContent(
                    transaction = transaction,
                    onClose = { scope.launch { sheetState.hide() } }
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        },
        content = content
    )
}

@Composable
private fun TransactionDetailContent(
    transaction: Pos.Transaction,
    onClose: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Close button - top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CloseButtonShape)
                    .border(1.dp, WCTheme.colors.borderSecondary, CloseButtonShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = WCTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))

        // Detail rows as cards
        Column(
            verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
        ) {
            // Date
            transaction.createdAt?.let {
                DetailCard("Date", formatFullDate(it))
            }

            // Status
            DetailCardWithBadge("Status", transaction.status)

            // Amount
            transaction.formatFiatAmount()?.let {
                DetailCard("Amount", it)
            }

            // Crypto received
            transaction.formatTokenAmount()?.let { tokenAmount ->
                DetailCard("Crypto received", "$tokenAmount ${transaction.tokenSymbol ?: ""}")
            }

            // Payment ID
            CopyableDetailCard(
                label = "Payment ID",
                displayValue = transaction.paymentId.truncateMiddle(6, 4),
                fullValue = transaction.paymentId,
                onCopy = { clipboardManager.setText(AnnotatedString(it)) }
            )

            // Hash ID
            transaction.txHash?.let { hash ->
                CopyableDetailCard(
                    label = "Hash ID",
                    displayValue = hash.truncateMiddle(4, 4),
                    fullValue = hash,
                    underline = true,
                    onCopy = { clipboardManager.setText(AnnotatedString(it)) }
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun DetailCard(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.foregroundPrimary, CardShape)
            .padding(WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun DetailCardWithBadge(label: String, status: Pos.TransactionStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.foregroundPrimary, CardShape)
            .padding(WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        StatusBadge(status = status)
    }
}

@Composable
private fun CopyableDetailCard(
    label: String,
    displayValue: String,
    fullValue: String,
    underline: Boolean = false,
    onCopy: (String) -> Unit
) {
    var showCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(WCTheme.colors.foregroundPrimary)
            .clickable {
                onCopy(fullValue)
                showCopied = true
                scope.launch {
                    delay(2000)
                    showCopied = false
                }
            }
            .padding(WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (showCopied) "Copied!" else displayValue,
            style = WCTheme.typography.bodyLgRegular,
            color = if (showCopied) WCTheme.colors.textAccentPrimary else WCTheme.colors.textPrimary,
            textDecoration = if (underline && !showCopied) TextDecoration.Underline else TextDecoration.None
        )
    }
}

private fun String.truncateMiddle(startChars: Int = 10, endChars: Int = 6): String {
    return if (length <= startChars + endChars + 3) this
    else "${take(startChars)}...${takeLast(endChars)}"
}
