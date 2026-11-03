package com.walletconnect.sample.pos.visapay

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walletconnect.sample.pos.BuildConfig
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.ui.theme.WCTheme

private const val RING_COUNT = 3
private const val RING_DURATION_MS = 2_400
private const val RING_MAX_SCALE = 2.1f

/**
 * VisaPay tap-to-pay screen. The terminal serves the payment link over NFC (HCE) and waits
 * for the Kotlin sample wallet to tap. After the tap everything is mocked: a 5 second
 * processing animation followed by an approved state, with a beep on tap and a chime on approval.
 */
@Composable
internal fun VisaPayTapScreen(
    onCancel: () -> Unit,
    onNewSale: () -> Unit,
    onDone: () -> Unit,
    viewModel: VisaPayTapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val view = LocalView.current

    LaunchedEffect(state.stage) {
        when (state.stage) {
            VisaPayTapStage.READING_WALLET -> {
                VisaPayTapSounds.playTap()
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            VisaPayTapStage.APPROVED -> VisaPayTapSounds.playApproved()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.stage == VisaPayTapStage.WAITING_FOR_TAP) {
            PosHeader(onBack = onCancel)
        } else {
            Spacer(Modifier.height(WCTheme.spacing.spacing13))
        }

        AnimatedContent(
            targetState = phaseOf(state.stage),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "visapay_tap_phase"
        ) { phase ->
            when (phase) {
                TapPhase.WAITING -> WaitingForTap(state, onSimulateTap = viewModel::simulateTap)
                TapPhase.PROCESSING -> Processing(state)
                TapPhase.APPROVED -> Approved(state)
            }
        }

        Footer(stage = state.stage, onCancel = onCancel, onNewSale = onNewSale, onDone = onDone)
    }
}

private enum class TapPhase { WAITING, PROCESSING, APPROVED }

private fun phaseOf(stage: VisaPayTapStage): TapPhase = when {
    stage == VisaPayTapStage.WAITING_FOR_TAP -> TapPhase.WAITING
    stage == VisaPayTapStage.APPROVED -> TapPhase.APPROVED
    else -> TapPhase.PROCESSING
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaitingForTap(state: VisaPayTapState, onSimulateTap: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = VisaPayTapLink.MERCHANT_NAME, style = WCTheme.typography.bodyLgRegular, color = colors.textTertiary)
        Spacer(Modifier.height(spacing.spacing2))
        Text(text = state.displayAmount, style = WCTheme.typography.h1Regular, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.spacing12))

        // Debug builds can long-press the animation to simulate a tap on devices without NFC.
        val simulateModifier = if (BuildConfig.DEBUG) {
            Modifier.combinedClickable(onClick = {}, onLongClick = onSimulateTap)
        } else {
            Modifier
        }
        ContactlessPulse(modifier = simulateModifier)

        Spacer(Modifier.height(spacing.spacing12))
        Text(text = "Tap to pay", style = WCTheme.typography.h4Regular, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.spacing2))
        Text(
            text = if (state.isNfcAvailable) {
                "Hold the phone near the terminal"
            } else {
                "NFC is off or unavailable on this terminal"
            },
            style = WCTheme.typography.bodyLgRegular,
            color = if (state.isNfcAvailable) colors.textTertiary else colors.textError,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(spacing.spacing5))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            Pill("VisaPay")
            Pill("USDC")
            Pill("Contactless")
        }
    }
}

/** Contactless icon with rings that ripple outwards, like a card terminal waiting for a tap. */
@Composable
private fun ContactlessPulse(modifier: Modifier = Modifier) {
    val colors = WCTheme.colors
    val iconSize = WCTheme.spacing.spacing13 + WCTheme.spacing.spacing10
    val transition = rememberInfiniteTransition(label = "contactless_pulse")
    Box(modifier = modifier.size(iconSize * RING_MAX_SCALE), contentAlignment = Alignment.Center) {
        repeat(RING_COUNT) { index ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(RING_DURATION_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * RING_DURATION_MS / RING_COUNT)
                ),
                label = "ring_$index"
            )
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .scale(1f + progress * (RING_MAX_SCALE - 1f))
                    .alpha(1f - progress)
                    .border(2.dp, colors.borderAccentPrimary, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(colors.foregroundAccentPrimary10Solid),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nfc_contactless),
                contentDescription = "Contactless",
                tint = colors.iconAccentPrimary,
                modifier = Modifier.size(WCTheme.spacing.spacing13)
            )
        }
    }
}

@Composable
private fun Processing(state: VisaPayTapState) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val ringSize = WCTheme.spacing.spacing13 * 2 + WCTheme.spacing.spacing8
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(VisaPayTapStage.processingDurationMs, easing = LinearEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.size(ringSize),
                color = colors.foregroundSecondary,
                strokeWidth = spacing.spacing2
            )
            CircularProgressIndicator(
                progress = progress.value,
                modifier = Modifier.size(ringSize),
                color = colors.bgAccentPrimary,
                strokeWidth = spacing.spacing2
            )
            Text(text = state.displayAmount, style = WCTheme.typography.h4Regular, color = colors.textPrimary)
        }
        Spacer(Modifier.height(spacing.spacing8))
        AnimatedContent(
            targetState = state.stage,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "visapay_tap_stage"
        ) { stage ->
            Text(text = "${stage.title}…", style = WCTheme.typography.h6Regular, color = colors.textPrimary)
        }
        Spacer(Modifier.height(spacing.spacing6))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
            VisaPayTapStage.processingStages.forEach { stage ->
                StageRow(stage = stage, current = state.stage)
            }
        }
    }
}

@Composable
private fun StageRow(stage: VisaPayTapStage, current: VisaPayTapStage) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val isDone = current.ordinal > stage.ordinal
    val isActive = current == stage
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
        Box(
            modifier = Modifier
                .size(spacing.spacing6)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> colors.bgSuccess
                        isActive -> colors.foregroundAccentPrimary10
                        else -> colors.foregroundSecondary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDone -> Icon(
                    painter = painterResource(R.drawable.ic_check_plain),
                    contentDescription = null,
                    tint = colors.iconSuccess,
                    modifier = Modifier.size(spacing.spacing4)
                )

                isActive -> Box(
                    modifier = Modifier
                        .size(spacing.spacing2)
                        .clip(CircleShape)
                        .background(colors.bgAccentPrimary)
                )
            }
        }
        Text(
            text = stage.title,
            style = WCTheme.typography.bodyLgRegular,
            color = if (isDone || isActive) colors.textPrimary else colors.textTertiary
        )
    }
}

@Composable
private fun Approved(state: VisaPayTapState) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val badgeSize = WCTheme.spacing.spacing13 * 2
    val checkScale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        checkScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .scale(checkScale.value)
                .clip(CircleShape)
                .background(colors.bgSuccess),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_plain),
                contentDescription = "Approved",
                tint = colors.iconSuccess,
                modifier = Modifier.size(spacing.spacing13)
            )
        }
        Spacer(Modifier.height(spacing.spacing8))
        Text(text = "Approved", style = WCTheme.typography.h3Regular, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.spacing2))
        Text(text = state.displayAmount, style = WCTheme.typography.h5Regular, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.spacing3))
        Text(
            text = "Paid with VisaPay · Contactless\n${state.reference}",
            style = WCTheme.typography.bodyLgRegular,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Footer(stage: VisaPayTapStage, onCancel: () -> Unit, onNewSale: () -> Unit, onDone: () -> Unit) {
    val spacing = WCTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.spacing5, vertical = spacing.spacing5),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3)
    ) {
        when (stage) {
            VisaPayTapStage.WAITING_FOR_TAP -> SecondaryButton("Cancel", onCancel)
            VisaPayTapStage.APPROVED -> {
                PrimaryButton("New sale", onNewSale)
                SecondaryButton("Done", onDone)
            }

            else -> Spacer(Modifier.height(spacing.spacing12))
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WCTheme.spacing.spacing12)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(WCTheme.colors.bgAccentPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = WCTheme.typography.bodyLgMedium, color = WCTheme.colors.textInvert)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WCTheme.spacing.spacing12)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = WCTheme.typography.bodyLgMedium, color = WCTheme.colors.textPrimary)
    }
}

@Composable
private fun Pill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(WCTheme.borderRadius.shapeFull)
            .background(WCTheme.colors.foregroundPrimary)
            .padding(horizontal = WCTheme.spacing.spacing3, vertical = WCTheme.spacing.spacing1),
        style = WCTheme.typography.bodySmMedium,
        color = WCTheme.colors.textTertiary
    )
}
