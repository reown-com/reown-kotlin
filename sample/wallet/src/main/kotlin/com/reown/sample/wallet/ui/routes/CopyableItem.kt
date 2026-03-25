@file:JvmSynthetic

package com.reown.sample.wallet.ui.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

@Composable
fun CopyableItem(
    key: String,
    value: String,
    onCopy: (String) -> Unit,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius3))
            .background(color = colors.foregroundSecondary)
            .clickable { onCopy(value) }
            .padding(spacing.spacing3)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = key,
                style = WCTheme.typography.bodySmMedium.copy(
                    color = colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.width(spacing.spacing2))
            Icon(
                painter = painterResource(id = R.drawable.ic_copy_small),
                contentDescription = "Copy",
                tint = colors.iconDefault
            )
        }
        Text(
            text = value,
            style = WCTheme.typography.bodySmRegular.copy(
                color = colors.textSecondary
            )
        )
    }
}
