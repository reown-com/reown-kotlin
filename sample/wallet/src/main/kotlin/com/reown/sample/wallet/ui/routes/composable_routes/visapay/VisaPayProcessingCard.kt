package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

private const val CARD_WIDTH_FRACTION = 0.72f
private const val FLOAT_DURATION_MS = 1_400
private const val SHIMMER_DURATION_MS = 1_300
private const val SHIMMER_ALPHA = 0.22f
private const val SHIMMER_BAND = 0.35f
private const val TILT_DEGREES = 6f
private const val PULSE_DURATION_MS = 700

/**
 * The VisaPay card animating while a payment is processed: it springs in, floats with a light sweep
 * passing over it and fills a progress bar over the processing time. On approval it settles flat and
 * a check pops onto its corner. Contactless payments also pulse the contactless symbol.
 */
@Composable
internal fun VisaPayProcessingCard(
    isApproved: Boolean,
    isContactless: Boolean,
    lastDigits: String,
) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val density = LocalDensity.current

    // Entry: spring up from slightly below and smaller.
    val entry = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entry.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    // Progress bar on the card, synced with the mocked processing time.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(VisaPayPaymentViewModel.PROCESSING_DURATION_MS, easing = LinearEasing))
    }

    // While processing the card floats and tilts a little; on approval it eases back to rest.
    val transition = rememberInfiniteTransition(label = "visapay_card")
    val wave by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(FLOAT_DURATION_MS), RepeatMode.Reverse),
        label = "card_float"
    )
    val shimmer by transition.animateFloat(
        initialValue = -SHIMMER_BAND,
        targetValue = 1f + SHIMMER_BAND,
        animationSpec = infiniteRepeatable(tween(SHIMMER_DURATION_MS, easing = LinearEasing)),
        label = "card_shimmer"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_DURATION_MS), RepeatMode.Reverse),
        label = "card_contactless_pulse"
    )
    val motion = remember { Animatable(1f) }
    LaunchedEffect(isApproved) {
        motion.animateTo(if (isApproved) 0f else 1f, tween(400))
    }

    // Approval check pops in with a bounce.
    val check = remember { Animatable(0f) }
    LaunchedEffect(isApproved) {
        if (isApproved) check.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    val floatOffsetPx = with(density) { spacing.spacing1.toPx() }
    val entryOffsetPx = with(density) { spacing.spacing10.toPx() }
    val shimmerColor = colors.textInvert.copy(alpha = SHIMMER_ALPHA)

    Box(
        modifier = Modifier.fillMaxWidth(CARD_WIDTH_FRACTION),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val scale = 0.85f + 0.15f * entry.value
                    scaleX = scale
                    scaleY = scale
                    alpha = entry.value.coerceIn(0f, 1f)
                    translationY = (1f - entry.value) * entryOffsetPx + wave * floatOffsetPx * motion.value
                    rotationX = wave * TILT_DEGREES * motion.value
                    rotationY = -wave * TILT_DEGREES * motion.value
                    cameraDistance = 12f * density.density
                }
                .clip(WCTheme.borderRadius.shapeLarge)
                .drawWithContent {
                    drawContent()
                    if (motion.value > 0f) {
                        val center = size.width * shimmer
                        val band = size.width * SHIMMER_BAND
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, shimmerColor.copy(alpha = shimmerColor.alpha * motion.value), Color.Transparent),
                                start = Offset(center - band, 0f),
                                end = Offset(center + band, size.height)
                            )
                        )
                    }
                }
        ) {
            VisaPayCardVisual {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "•••• $lastDigits",
                            modifier = Modifier.weight(1f),
                            style = WCTheme.typography.bodyLgRegular.copy(color = colors.textInvert)
                        )
                        if (isContactless) {
                            Image(
                                painter = painterResource(R.drawable.ic_nfc_contactless),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(spacing.spacing6)
                                    .graphicsLayer { alpha = if (isApproved) 1f else pulse },
                                colorFilter = ColorFilter.tint(colors.textInvert)
                            )
                        } else {
                            Text(text = "VISA", style = WCTheme.typography.bodyLgMedium.copy(color = colors.textInvert))
                        }
                    }
                    // Progress track along the bottom of the card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing.spacing1)
                            .clip(WCTheme.borderRadius.shapeFull)
                            .background(colors.textInvert.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (isApproved) 1f else progress.value)
                                .clip(WCTheme.borderRadius.shapeFull)
                                .background(if (isApproved) colors.iconSuccess else colors.textInvert)
                        )
                    }
                }
            }
        }

        if (check.value > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = spacing.spacing3, y = -spacing.spacing3)
                    .graphicsLayer {
                        scaleX = check.value
                        scaleY = check.value
                    }
                    .size(spacing.spacing11)
                    .clip(CircleShape)
                    .background(colors.iconSuccess)
                    .padding(spacing.spacing2),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Payment approved",
                    colorFilter = ColorFilter.tint(colors.textInvert)
                )
            }
        }
    }
}
