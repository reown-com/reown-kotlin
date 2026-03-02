@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.reown.sample.common.ui.theme.WCTheme

@Composable
fun ChainIcons(
    chainIds: List<String>,
    size: Dp = WCTheme.spacing.spacing6,
    maxVisible: Int = 5,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius
    val uniqueChainIds = chainIds.distinct()
    val visibleChainIds = uniqueChainIds.take(maxVisible)
    val remainingCount = (uniqueChainIds.size - maxVisible).coerceAtLeast(0)

    val borderColor = colors.borderPrimary
    val containerSize = size + spacing.spacing1
    val pillHeight = size + spacing.spacing1
    val pillMinWidth = size + spacing.spacing3

    Row(
        horizontalArrangement = Arrangement.spacedBy(-spacing.spacing2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleChainIds.forEach { chainId ->
            val icon = getChainIcon(chainId)
            Box(
                modifier = Modifier
                    .size(containerSize)
                    .clip(CircleShape)
                    .background(colors.foregroundTertiary)
                    .border(spacing.spacing05, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val (color, label) = chainInfo(chainId)
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = WCTheme.typography.bodySmMedium.copy(color = colors.textInvert)
                        )
                    }
                }
            }
        }
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .height(pillHeight)
                    .widthIn(min = pillMinWidth)
                    .clip(borderRadius.shapeFull)
                    .background(colors.foregroundTertiary)
                    .border(spacing.spacing05, borderColor, borderRadius.shapeFull)
                    .padding(horizontal = spacing.spacing2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    style = WCTheme.typography.bodySmMedium.copy(color = colors.textPrimary)
                )
            }
        }
    }
}
