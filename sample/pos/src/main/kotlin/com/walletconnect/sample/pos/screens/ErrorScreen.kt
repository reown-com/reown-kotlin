package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.ErrorCodes
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.components.PosHeader

@Composable
fun ErrorScreen(
    errorCode: String,
    onNewPayment: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    val isInitError = errorCode == ErrorCodes.INIT_FAILED
    val (title, subtitle) = getErrorMessages(errorCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PosHeader(onBack = if (isInitError) onClose ?: {} else onNewPayment)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = WCTheme.spacing.spacing5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning_circle),
                contentDescription = "Warning",
                tint = WCTheme.colors.bgAccentPrimary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing4))

            Text(
                text = title,
                style = WCTheme.typography.h6Regular,
                color = WCTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            Text(
                text = subtitle,
                style = WCTheme.typography.bodyLgRegular,
                color = WCTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WCTheme.spacing.spacing5)
                .height(48.dp)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.bgAccentPrimary)
                .clickable(onClick = if (isInitError) onClose ?: {} else onNewPayment),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isInitError) "Close" else "New payment",
                    style = WCTheme.typography.bodyLgRegular,
                    color = WCTheme.colors.textInvert
                )
                if (!isInitError) {
                    Spacer(Modifier.width(WCTheme.spacing.spacing2))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = WCTheme.colors.textInvert,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

private fun getErrorMessages(errorCode: String): Pair<String, String> {
    return when (errorCode) {
        "expired" -> "Your payment has expired" to "This payment request has expired. Please generate a new payment and try again."
        "cancelled" -> "Payment cancelled" to "Payment was cancelled. You can start a new payment anytime."
        "create_failed" -> "Payment can't be completed" to "We're unable to complete this payment at this time. Please generate a new payment and try again."
        "not_found" -> "Payment not found" to "The payment could not be found. Please try creating a new one."
        "invalid_request" -> "Invalid request" to "The payment request was invalid. Please try again with a valid amount."
        ErrorCodes.INIT_FAILED -> "Initialization failed" to "POS SDK could not be initialized. Check that MERCHANT_API_KEY and MERCHANT_ID are configured correctly."
        else -> "Payment can't be completed" to "We're unable to complete this payment at this time. Please generate a new payment and try again."
    }
}
