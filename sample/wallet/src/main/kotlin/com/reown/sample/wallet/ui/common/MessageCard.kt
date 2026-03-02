@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme

@Composable
fun MessageCard(
    message: String,
    showTitle: Boolean = true,
    title: String = "Message",
    maxHeight: Dp = 120.dp,
) {
    if (message.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(
                themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFF3F3F3)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showTitle) {
            Text(
                text = title,
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = themedColor(darkColor = 0xFF9A9A9A, lightColor = 0xFF9A9A9A)
                )
            )
        }
        Text(
            text = message,
            style = WCTheme.typography.bodyMdRegular.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
            )
        )
    }
}
