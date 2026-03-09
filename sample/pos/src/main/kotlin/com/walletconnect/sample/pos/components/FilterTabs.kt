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
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCBorderRadius
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.pos.Pos

data class FilterTab(
    val label: String,
    val status: Pos.TransactionStatus?
)

val defaultFilterTabs = listOf(
    FilterTab("All", null),
    FilterTab("Failed", Pos.TransactionStatus.FAILED),
    FilterTab("Pending", Pos.TransactionStatus.PROCESSING),
    FilterTab("Completed", Pos.TransactionStatus.SUCCEEDED)
)

private val PillShape = RoundedCornerShape(WCBorderRadius.radius4)

@Composable
fun FilterTabs(
    selectedStatus: Pos.TransactionStatus?,
    onStatusSelected: (Pos.TransactionStatus?) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<FilterTab> = defaultFilterTabs
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedStatus == tab.status
            val bgColor = if (isSelected) WCTheme.colors.foregroundAccentPrimary10 else WCTheme.colors.foregroundPrimary
            val borderMod = if (isSelected) Modifier.border(1.dp, WCTheme.colors.borderAccentPrimary, PillShape) else Modifier

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .then(borderMod)
                    .background(bgColor)
                    .clickable { onStatusSelected(tab.status) }
                    .padding(horizontal = WCTheme.spacing.spacing5, vertical = WCTheme.spacing.spacing4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot (not shown for "All")
                val dotColor = when (tab.status) {
                    Pos.TransactionStatus.SUCCEEDED -> WCTheme.colors.iconSuccess
                    Pos.TransactionStatus.FAILED -> WCTheme.colors.iconError
                    Pos.TransactionStatus.PROCESSING, Pos.TransactionStatus.REQUIRES_ACTION -> WCTheme.colors.foregroundTertiary
                    else -> null
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
                    text = tab.label,
                    style = WCTheme.typography.bodyLgRegular,
                    color = if (isSelected) WCTheme.colors.textPrimary else WCTheme.colors.textSecondary
                )
            }
        }
    }
}
