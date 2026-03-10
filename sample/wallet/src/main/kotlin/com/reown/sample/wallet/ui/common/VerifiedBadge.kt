@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.ui.common.peer.Validation

@Composable
fun VerifiedBadge(
    validation: Validation?,
    isScam: Boolean?,
) {
    val colors = WCTheme.colors
    val (label, bgColor) = when {
        isScam == true -> "Unsafe" to colors.textError
        validation == Validation.INVALID -> "Mismatch" to colors.textWarning
        validation == Validation.VALID -> "Verified" to colors.textSuccess
        else -> "Unverified" to colors.foregroundTertiary
    }

    val textColor = if (validation == null && isScam != true) {
        colors.textPrimary
    } else {
        colors.textInvert
    }

    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(WCTheme.borderRadius.shapeXSmall)
            .background(bgColor)
            .padding(horizontal = WCTheme.spacing.spacing2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyMdMedium.copy(color = textColor)
        )
    }
}
