package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCBorderRadius
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.pos.Pos

enum class TransactionFilter(val label: String, val statuses: List<Pos.TransactionStatus>?) {
    ALL("All", null),
    FAILED("Failed", listOf(Pos.TransactionStatus.FAILED)),
    CANCELLED("Cancelled", listOf(Pos.TransactionStatus.CANCELLED)),
    EXPIRED("Expired", listOf(Pos.TransactionStatus.EXPIRED)),
    PENDING("Pending", listOf(Pos.TransactionStatus.REQUIRES_ACTION, Pos.TransactionStatus.PROCESSING)),
    COMPLETED("Completed", listOf(Pos.TransactionStatus.SUCCEEDED))
}

private val PillShape = RoundedCornerShape(WCBorderRadius.radius4)

@Composable
fun FilterTabs(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        TransactionFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            val bgColor = if (isSelected) WCTheme.colors.foregroundAccentPrimary10 else WCTheme.colors.foregroundPrimary
            val borderMod = if (isSelected) Modifier.border(1.dp, WCTheme.colors.borderAccentPrimary, PillShape) else Modifier

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .then(borderMod)
                    .background(bgColor)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = WCTheme.spacing.spacing5, vertical = WCTheme.spacing.spacing4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor: Color? = when (filter) {
                    TransactionFilter.COMPLETED -> WCTheme.colors.iconSuccess
                    TransactionFilter.FAILED, TransactionFilter.CANCELLED, TransactionFilter.EXPIRED -> WCTheme.colors.iconError
                    TransactionFilter.PENDING -> WCTheme.colors.foregroundTertiary
                    TransactionFilter.ALL -> null
                }
                dotColor?.let { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(WCTheme.spacing.spacing2))
                }

                Text(
                    text = filter.label,
                    style = WCTheme.typography.bodyLgRegular,
                    color = if (isSelected) WCTheme.colors.textPrimary else WCTheme.colors.textSecondary
                )
            }
        }
    }
}
