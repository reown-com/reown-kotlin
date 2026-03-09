package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme

@Composable
fun ErrorScreen(
    errorCode: String,
    onNewPayment: () -> Unit,
) {
    val (title, subtitle) = getErrorMessages(errorCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = WCTheme.colors.iconError,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing5))

        Text(
            text = title,
            style = WCTheme.typography.h5Regular,
            color = WCTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

        Text(
            text = subtitle,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WCTheme.spacing.spacing12)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.bgAccentPrimary)
                .clickable(onClick = onNewPayment),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "New payment",
                style = WCTheme.typography.bodyLgMedium,
                color = WCTheme.colors.textInvert
            )
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

private fun getErrorMessages(errorCode: String): Pair<String, String> {
    return when (errorCode) {
        "expired" -> "Payment expired" to "Your payment has expired. Please generate a new payment and try again."
        "cancelled" -> "Payment cancelled" to "Payment was cancelled. You can start a new payment anytime."
        "create_failed" -> "Payment failed" to "We couldn't create the payment. Please check your connection and try again."
        "not_found" -> "Payment not found" to "The payment could not be found. Please try creating a new one."
        "invalid_request" -> "Invalid request" to "The payment request was invalid. Please try again with a valid amount."
        else -> "Payment failed" to "We're unable to complete this payment at this time. Please try again."
    }
}
