package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCTheme
import com.walletconnect.pos.Pos

private val BadgeShape = RoundedCornerShape(8.dp)
private val SuccessBg = Color(0xE630A46B) // rgba(48,164,107,0.9)
private val ErrorBg = Color(0xE6DF4A34)   // rgba(223,74,52,0.9)

@Composable
fun StatusBadge(status: Pos.TransactionStatus, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (status) {
        Pos.TransactionStatus.SUCCEEDED -> Triple("Completed", SuccessBg, Color.White)
        Pos.TransactionStatus.FAILED -> Triple("Failed", ErrorBg, Color.White)
        Pos.TransactionStatus.EXPIRED -> Triple("Expired", ErrorBg, Color.White)
        Pos.TransactionStatus.CANCELLED -> Triple("Cancelled", ErrorBg, Color.White)
        Pos.TransactionStatus.PROCESSING, Pos.TransactionStatus.REQUIRES_ACTION ->
            Triple("Pending", WCTheme.colors.foregroundTertiary, WCTheme.colors.textPrimary)
        Pos.TransactionStatus.UNKNOWN ->
            Triple("Unknown", WCTheme.colors.foregroundTertiary, WCTheme.colors.textPrimary)
    }

    BadgePill(label = label, bgColor = bgColor, textColor = textColor, modifier = modifier)
}

@Composable
fun TransactionBadge(transaction: Pos.Transaction, modifier: Modifier = Modifier) {
    if (transaction.isRefunded) {
        BadgePill(
            label = "Refunded",
            bgColor = WCTheme.colors.foregroundSecondary,
            textColor = WCTheme.colors.textSecondary,
            modifier = modifier
        )
    } else {
        StatusBadge(status = transaction.status, modifier = modifier)
    }
}

@Composable
private fun BadgePill(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(bgColor, BadgeShape)
            .padding(horizontal = WCTheme.spacing.spacing2, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyMdMedium,
            color = textColor
        )
    }
}
