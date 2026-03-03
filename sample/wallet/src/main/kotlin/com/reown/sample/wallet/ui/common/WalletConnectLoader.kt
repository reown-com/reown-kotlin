@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LightGrayColor = Color(0xFFE8E8E8)
private val DarkGrayColor = Color(0xFF363636)
private val GrayColor = Color(0xFF6C6C6C)
private val AccentBlueColor = Color(0xFF0988F0)

private const val CYCLE_DURATION = 4000
private const val FADE_TIME = 80

@Composable
fun WalletConnectLoader(size: Dp = 120.dp) {
    val gap = 2.dp
    val squareSize = (size - gap) / 2
    val halfSquare = squareSize / 2
    val squareSizeFloat = squareSize.value

    val infiniteTransition = rememberInfiniteTransition(label = "wc_loader")

    // --- Opacity animations (staggered fade-in: BL -> BR -> TR -> TL) ---

    val opacityBL by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                0f at 30 using LinearEasing
                1f at 30 + FADE_TIME using LinearEasing
                1f at 3580 using LinearEasing
                0f at 3580 + FADE_TIME using LinearEasing
            }
        ),
        label = "opacityBL"
    )

    val opacityBR by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                0f at 120 using LinearEasing
                1f at 120 + FADE_TIME using LinearEasing
                1f at 3660 using LinearEasing
                0f at 3660 + FADE_TIME using LinearEasing
            }
        ),
        label = "opacityBR"
    )

    val opacityTR by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                0f at 200 using LinearEasing
                1f at 200 + FADE_TIME using LinearEasing
                1f at 3740 using LinearEasing
                0f at 3740 + FADE_TIME using LinearEasing
            }
        ),
        label = "opacityTR"
    )

    val opacityTL by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                0f at 250 using LinearEasing
                1f at 250 + FADE_TIME using LinearEasing
                1f at 3780 using LinearEasing
                0f at 3780 + FADE_TIME using LinearEasing
            }
        ),
        label = "opacityTL"
    )

    // --- Border radius animations (unique morphing per square) ---

    val cornerBL by infiniteTransition.animateFloat(
        initialValue = squareSizeFloat * 0.10f,
        targetValue = squareSizeFloat * 0.10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                (squareSizeFloat * 0.10f) at 30 using FastOutSlowInEasing
                (squareSizeFloat * 0.15f) at 80 using LinearEasing
                (squareSizeFloat * 0.15f) at 1230 using FastOutSlowInEasing
                (squareSizeFloat * 0.25f) at 2230 using LinearEasing
                (squareSizeFloat * 0.25f) at 2730 using FastOutSlowInEasing
                (squareSizeFloat * 0.10f) at 3730 using LinearEasing
            }
        ),
        label = "cornerBL"
    )

    val cornerBR by infiniteTransition.animateFloat(
        initialValue = squareSizeFloat * 0.12f,
        targetValue = squareSizeFloat * 0.12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                (squareSizeFloat * 0.12f) at 120 using FastOutSlowInEasing
                (squareSizeFloat * 0.20f) at 970 using LinearEasing
                (squareSizeFloat * 0.20f) at 1570 using FastOutSlowInEasing
                (squareSizeFloat * 0.48f) at 2670 using FastOutSlowInEasing
                (squareSizeFloat * 0.12f) at 3570 using LinearEasing
            }
        ),
        label = "cornerBR"
    )

    val cornerTR by infiniteTransition.animateFloat(
        initialValue = squareSizeFloat * 0.12f,
        targetValue = squareSizeFloat * 0.12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                (squareSizeFloat * 0.12f) at 200 using FastOutSlowInEasing
                (squareSizeFloat * 0.45f) at 1000 using LinearEasing
                (squareSizeFloat * 0.45f) at 1500 using FastOutSlowInEasing
                (squareSizeFloat * 0.20f) at 2400 using LinearEasing
                (squareSizeFloat * 0.20f) at 3000 using FastOutSlowInEasing
                (squareSizeFloat * 0.12f) at 3700 using LinearEasing
            }
        ),
        label = "cornerTR"
    )

    val cornerTL by infiniteTransition.animateFloat(
        initialValue = squareSizeFloat * 0.12f,
        targetValue = squareSizeFloat * 0.12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE_DURATION
                (squareSizeFloat * 0.12f) at 250 using FastOutSlowInEasing
                (squareSizeFloat * 0.30f) at 1000 using LinearEasing
                (squareSizeFloat * 0.30f) at 1600 using FastOutSlowInEasing
                (squareSizeFloat * 0.20f) at 2600 using LinearEasing
                (squareSizeFloat * 0.20f) at 3600 using FastOutSlowInEasing
                (squareSizeFloat * 0.12f) at 4000
            }
        ),
        label = "cornerTL"
    )

    // --- Layout: 2x2 grid with absolute positioning ---

    Box(modifier = Modifier.size(size)) {
        // Top-left: Light gray square
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 0.dp)
                .size(squareSize)
                .clip(RoundedCornerShape(cornerTL.dp))
                .background(LightGrayColor.copy(alpha = opacityTL))
        )

        // Top-right: Dark gray square
        Box(
            modifier = Modifier
                .offset(x = squareSize + gap, y = 0.dp)
                .size(squareSize)
                .clip(RoundedCornerShape(cornerTR.dp))
                .background(DarkGrayColor.copy(alpha = opacityTR))
        )

        // Bottom-left: Gray half-height pill
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = squareSize + gap + halfSquare)
                .size(width = squareSize, height = halfSquare)
                .clip(RoundedCornerShape(cornerBL.dp))
                .background(GrayColor.copy(alpha = opacityBL))
        )

        // Bottom-right: Accent blue square
        Box(
            modifier = Modifier
                .offset(x = squareSize + gap, y = squareSize + gap)
                .size(squareSize)
                .clip(RoundedCornerShape(cornerBR.dp))
                .background(AccentBlueColor.copy(alpha = opacityBR))
        )
    }
}
