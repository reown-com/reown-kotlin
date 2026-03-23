package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCBorderRadius
import com.walletconnect.sample.pos.ui.theme.WCTheme

@Composable
fun BottomSheetHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = WCTheme.typography.h6Regular,
            color = WCTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(WCTheme.spacing.spacing3))
                .border(
                    width = 1.dp,
                    color = WCTheme.colors.borderSecondary,
                    shape = RoundedCornerShape(WCTheme.spacing.spacing3)
                )
                .clickable(onClick = onDismiss),
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
}

@Composable
fun RadioIndicator() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, WCTheme.colors.iconAccentPrimary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(WCTheme.colors.iconAccentPrimary, CircleShape)
        )
    }
}

@Composable
fun SelectableOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(WCBorderRadius.radius4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.dp, WCTheme.colors.borderAccentPrimary, shape)
                        .background(WCTheme.colors.foregroundAccentPrimary10, shape)
                } else {
                    Modifier.background(WCTheme.colors.foregroundPrimary, shape)
                }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
        }
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            RadioIndicator()
        }
    }
}
