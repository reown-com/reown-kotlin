package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.RefundUiState
import com.walletconnect.sample.pos.ui.theme.WCBorderRadius
import com.walletconnect.sample.pos.ui.theme.WCTheme

private val ButtonShape = RoundedCornerShape(WCBorderRadius.radius4)

@Composable
fun RefundConfirmSheetContent(
    transaction: Pos.Transaction,
    state: RefundUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val isSubmitting = state is RefundUiState.Submitting
    val error = (state as? RefundUiState.Error)?.refundError

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        BottomSheetHeader(title = "Refund this payment?", onDismiss = onCancel)

        Spacer(Modifier.height(WCTheme.spacing.spacing6))

        // Amount + reference summary card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WCTheme.colors.foregroundPrimary, ButtonShape)
                .padding(WCTheme.spacing.spacing5)
        ) {
            val amountText = transaction.formatFiatAmount() ?: "—"
            Text(
                text = amountText,
                style = WCTheme.typography.h5Medium,
                color = WCTheme.colors.textPrimary
            )
            transaction.referenceId?.let { reference ->
                Spacer(Modifier.height(WCTheme.spacing.spacing1))
                Text(
                    text = "Reference: $reference",
                    style = WCTheme.typography.bodyMdRegular,
                    color = WCTheme.colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = "This will mark the payment as refunded. Funds are not returned to the buyer wallet automatically.",
            style = WCTheme.typography.bodyMdRegular,
            color = WCTheme.colors.textSecondary,
        )

        if (error != null) {
            Spacer(Modifier.height(WCTheme.spacing.spacing3))
            Text(
                text = error.message.ifBlank { "Refund failed" },
                style = WCTheme.typography.bodyMdMedium,
                color = WCTheme.colors.textError,
                textAlign = TextAlign.Start
            )
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))

        // Primary action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(ButtonShape)
                .background(WCTheme.colors.bgAccentPrimary)
                .clickable(enabled = !isSubmitting, onClick = onConfirm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = WCTheme.colors.textInvert,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(WCTheme.spacing.spacing2))
                Text(
                    text = "Submitting…",
                    style = WCTheme.typography.bodyLgMedium,
                    color = WCTheme.colors.textInvert
                )
            } else {
                Text(
                    text = if (error != null) "Try again" else "Confirm refund",
                    style = WCTheme.typography.bodyLgMedium,
                    color = WCTheme.colors.textInvert
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing2))

        // Secondary action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(ButtonShape)
                .border(1.dp, WCTheme.colors.borderSecondary, ButtonShape)
                .clickable(enabled = !isSubmitting, onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancel",
                style = WCTheme.typography.bodyLgMedium,
                color = WCTheme.colors.textPrimary
            )
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing4))
    }
}
