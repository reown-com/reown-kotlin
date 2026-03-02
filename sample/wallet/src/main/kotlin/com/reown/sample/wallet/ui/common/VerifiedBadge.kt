@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.ui.common.peer.Validation

// Colors matching RN theme: text-success, text-warning, text-error, foreground-tertiary
private val verifiedColor = Color(0xFF30A46B)
private val mismatchColor = Color(0xFFF3A13F)
private val unsafeColor = Color(0xFFDF4A34)

@Composable
fun VerifiedBadge(
    validation: Validation?,
    isScam: Boolean?,
) {
    val (label, bgColor) = when {
        isScam == true -> "Unsafe" to unsafeColor
        validation == Validation.INVALID -> "Mismatch" to mismatchColor
        validation == Validation.VALID -> "Verified" to verifiedColor
        else -> "Unverified" to themedColor(darkColor = Color(0xFF363636), lightColor = Color(0xFFD0D0D0))
    }

    val textColor = if (validation == null && isScam != true) {
        themedColor(darkColor = Color.White, lightColor = Color(0xFF202020))
    } else {
        Color.White
    }

    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyMdMedium.copy(color = textColor)
        )
    }
}
