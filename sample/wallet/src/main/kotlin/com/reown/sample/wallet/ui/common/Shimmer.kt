@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme

@Composable
fun Shimmer(
    width: Dp,
    height: Dp,
    cornerRadius: Dp = 4.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val base = WCTheme.colors.foregroundSecondary
    val highlight = WCTheme.colors.foregroundTertiary
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateX * 200f, 0f),
        end = Offset(translateX * 200f + 200f, 0f),
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

@Composable
fun ShimmerSolid(
    width: Dp,
    height: Dp,
    cornerRadius: Dp = 4.dp,
    color: Color = WCTheme.colors.foregroundSecondary,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color),
    )
}
