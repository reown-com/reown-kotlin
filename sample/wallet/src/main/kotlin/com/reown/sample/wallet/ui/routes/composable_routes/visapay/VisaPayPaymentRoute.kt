package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scanner_options.ModalCloseButton

/** Bottom sheet shown after a QR scan while a VisaPay card is active: a mocked card payment. */
@Composable
fun VisaPayPaymentRoute(
    navController: NavController,
    viewModel: VisaPayPaymentViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val view = LocalView.current

    // Beep + haptic as soon as a POS tap opens the sheet, chime when the payment is approved.
    LaunchedEffect(Unit) {
        if (state.request.isContactless) {
            VisaPaySounds.playTap()
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    LaunchedEffect(state.isApproved) {
        if (state.isApproved) {
            VisaPaySounds.playApproved()
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.bgPrimary,
                shape = RoundedCornerShape(topStart = WCTheme.borderRadius.radius8, topEnd = WCTheme.borderRadius.radius8)
            )
            .navigationBarsPadding()
            .padding(spacing.spacing5),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing5)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MerchantHeader(state.request, modifier = Modifier.weight(1f))
            if (state.isApproved) ModalCloseButton(onClick = { navController.popBackStack() })
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing4)
        ) {
            ProcessingRing(isApproved = state.isApproved, isContactless = state.request.isContactless)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                Text(
                    text = state.request.fiatLabel ?: formatUsdc(state.request.amount),
                    style = WCTheme.typography.h3Regular.copy(color = colors.textPrimary)
                )
                if (state.request.fiatLabel != null) {
                    Text(
                        text = "Paying ${formatUsdc(state.request.amount)}",
                        style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary)
                    )
                }
            }
            AnimatedContent(
                targetState = state.stage,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "visapay_payment_stage"
            ) { stage ->
                Text(
                    text = if (stage == VisaPayPaymentStage.APPROVED) "Payment approved" else "${stage.title}…",
                    textAlign = TextAlign.Center,
                    style = WCTheme.typography.bodyLgMedium.copy(
                        color = if (stage == VisaPayPaymentStage.APPROVED) colors.textSuccess else colors.textTertiary
                    )
                )
            }
        }

        AnimatedContent(
            targetState = state.isApproved,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "visapay_payment_body"
        ) { approved ->
            if (approved) {
                Receipt(state)
            } else {
                StageTimeline(state.stage, state.card)
            }
        }

        if (state.isApproved) {
            VisaPayPrimaryButton("Done", onClick = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MerchantHeader(request: VisaPayPaymentRequest, modifier: Modifier = Modifier) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
    ) {
        Box(
            modifier = Modifier
                .size(spacing.spacing11)
                .clip(CircleShape)
                .background(colors.foregroundAccentPrimary10Solid),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = request.merchant.take(1),
                style = WCTheme.typography.h6Medium.copy(color = colors.textAccentPrimary)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing05)) {
            Text(text = request.merchant, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            Text(text = "${request.category} · Card payment", style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
            if (request.isContactless) VisaPayBadge("Contactless · Tap to pay", VisaPayBadgeTone.ACCENT)
        }
    }
}

/** Ring that fills over the whole processing time, then turns into a success check. */
@Composable
private fun ProcessingRing(isApproved: Boolean, isContactless: Boolean) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val ringSize = spacing.spacing13 + spacing.spacing10
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = VisaPayPaymentViewModel.PROCESSING_DURATION_MS, easing = LinearEasing))
    }

    AnimatedContent(
        targetState = isApproved,
        transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.8f)) togetherWith fadeOut(tween(150)) },
        label = "visapay_payment_ring"
    ) { approved ->
        Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            if (approved) {
                Box(
                    modifier = Modifier
                        .size(ringSize)
                        .clip(CircleShape)
                        .background(colors.bgSuccess),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "Payment approved",
                        modifier = Modifier.size(spacing.spacing11),
                        colorFilter = ColorFilter.tint(colors.iconSuccess)
                    )
                }
            } else {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(ringSize),
                    color = colors.foregroundSecondary,
                    strokeWidth = spacing.spacing1 + spacing.spacing05
                )
                CircularProgressIndicator(
                    progress = progress.value,
                    modifier = Modifier.size(ringSize),
                    color = colors.bgAccentPrimary,
                    strokeWidth = spacing.spacing1 + spacing.spacing05
                )
                Box(
                    modifier = Modifier
                        .size(spacing.spacing13)
                        .clip(CircleShape)
                        .background(colors.foregroundAccentPrimary10Solid),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(if (isContactless) R.drawable.ic_nfc_contactless else R.drawable.ic_card),
                        contentDescription = null,
                        modifier = Modifier.size(spacing.spacing8),
                        colorFilter = ColorFilter.tint(colors.iconAccentPrimary)
                    )
                }
            }
        }
    }
}

@Composable
private fun StageTimeline(current: VisaPayPaymentStage, card: VisaPayCard?) {
    val wallet = card?.fundingWallet.orEmpty()
    val network = card?.network?.label ?: VisaPayNetwork.BASE.label
    fun stateOf(stage: VisaPayPaymentStage): TimelineState = when {
        current.ordinal > stage.ordinal -> TimelineState.DONE
        current == stage -> TimelineState.ACTIVE
        else -> TimelineState.TODO
    }

    VisaPaySurface {
        TimelineRow(stateOf(VisaPayPaymentStage.READING_REQUEST), VisaPayPaymentStage.READING_REQUEST.title, "Merchant and amount confirmed")
        TimelineRow(
            stateOf(VisaPayPaymentStage.AUTHORIZING),
            VisaPayPaymentStage.AUTHORIZING.title,
            "Card ending ${card?.lastDigits ?: VisaPayMocks.CARD_LAST_DIGITS}"
        )
        TimelineRow(
            stateOf(VisaPayPaymentStage.PULLING_FUNDS),
            VisaPayPaymentStage.PULLING_FUNDS.title,
            "From ${wallet.take(6)}...${wallet.takeLast(4)} on $network"
        )
        TimelineRow(stateOf(VisaPayPaymentStage.SETTLING), VisaPayPaymentStage.SETTLING.title, "Usually instant")
    }
}

@Composable
private fun Receipt(state: VisaPayPaymentState) {
    val colors = WCTheme.colors
    val card = state.card
    val wallet = card?.fundingWallet.orEmpty()
    VisaPaySurface {
        ReceiptRow("Merchant", state.request.merchant)
        Divider(color = colors.borderPrimary)
        ReceiptRow("Amount", formatUsdc(state.request.amount))
        state.request.reference?.let { reference ->
            Divider(color = colors.borderPrimary)
            ReceiptRow("Order", reference, monospace = true)
        }
        Divider(color = colors.borderPrimary)
        ReceiptRow("Card", "•••• ${card?.lastDigits ?: VisaPayMocks.CARD_LAST_DIGITS}")
        Divider(color = colors.borderPrimary)
        ReceiptRow("Paid from", "${wallet.take(6)}...${wallet.takeLast(4)} · ${card?.network?.label.orEmpty()}", monospace = true)
        if (card != null) {
            Divider(color = colors.borderPrimary)
            ReceiptRow("Left to spend today", formatUsdc(card.leftToSpend))
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, monospace: Boolean = false) {
    val colors = WCTheme.colors
    val valueStyle = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WCTheme.spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary)
        )
        Text(text = value, style = if (monospace) valueStyle.copy(fontFamily = FontFamily.Monospace) else valueStyle)
    }
}
