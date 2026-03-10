package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme

private val dateRangeOptions = listOf("All Time", "Today", "7 Days", "This Week", "This Month")

private val PillShape = RoundedCornerShape(16.dp)

@Composable
fun DateRangeSelector(
    selectedOptionIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        dateRangeOptions.forEachIndexed { index, label ->
            val isSelected = selectedOptionIndex == index
            val bgColor = if (isSelected) WCTheme.colors.foregroundAccentPrimary10 else WCTheme.colors.foregroundPrimary
            val borderMod = if (isSelected) Modifier.border(1.dp, WCTheme.colors.borderAccentPrimary, PillShape) else Modifier

            Text(
                text = label,
                style = WCTheme.typography.bodyLgRegular,
                color = WCTheme.colors.textPrimary,
                modifier = Modifier
                    .clip(PillShape)
                    .then(borderMod)
                    .background(bgColor)
                    .clickable { onOptionSelected(index) }
                    .padding(horizontal = WCTheme.spacing.spacing5, vertical = WCTheme.spacing.spacing4)
            )
        }
    }
}
