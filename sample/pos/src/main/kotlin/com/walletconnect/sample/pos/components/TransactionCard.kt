package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.pos.Pos
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun TransactionCard(
    transaction: Pos.Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick)
            .padding(WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Amount
        Text(
            text = transaction.formatFiatAmount() ?: "—",
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary
        )

        Spacer(Modifier.width(WCTheme.spacing.spacing3))

        // Date - fills available space
        transaction.createdAt?.let { timestamp ->
            Text(
                text = formatShortDate(timestamp),
                style = WCTheme.typography.bodyLgRegular,
                color = WCTheme.colors.textSecondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        } ?: Spacer(Modifier.weight(1f))

        Spacer(Modifier.width(WCTheme.spacing.spacing4))

        // Status badge
        StatusBadge(status = transaction.status)
    }
}

internal fun formatShortDate(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("MMM d, yy", Locale.getDefault())
        val date = inputFormat.parse(timestamp.substringBefore(".").substringBefore("Z"))
        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp
    }
}

internal fun formatFullDate(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timestamp.substringBefore(".").substringBefore("Z"))
        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp
    }
}
