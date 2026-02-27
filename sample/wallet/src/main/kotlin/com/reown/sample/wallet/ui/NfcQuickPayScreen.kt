@file:JvmSynthetic

package com.reown.sample.wallet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.sample.common.ui.themedColor

private val BlueAccent = Color(0xFF3396FF)
private val SuccessGreen = Color(0xFF27AE60)
private val ErrorRed = Color(0xFFED4747)

@Composable
fun NfcQuickPayScreen(
    viewModel: NfcQuickPayViewModel,
    onDone: () -> Unit,
    onFallback: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    when (val s = state) {
        is QuickPayState.Done -> {
            onDone()
            return
        }
        is QuickPayState.FallbackToFullApp -> {
            onFallback(s.paymentUrl)
            return
        }
        else -> Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(themedColor(Color(0xFF1C1C1E), Color(0xFFFFFFFF)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val s = state) {
                is QuickPayState.Initializing,
                is QuickPayState.FetchingOptions -> LoadingContent()

                is QuickPayState.Processing -> ProcessingContent(
                    merchantName = s.merchantName,
                    amount = s.amount,
                    optionSummary = s.optionSummary
                )

                is QuickPayState.Success -> SuccessContent(
                    merchantName = s.merchantName,
                    amount = s.amount,
                    optionSummary = s.optionSummary,
                    onDone = onDone
                )

                is QuickPayState.Error -> ErrorContent(
                    message = s.message,
                    onDismiss = onDone
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Text(
        text = "WalletConnect Pay",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = themedColor(Color(0xFF8E8E93), Color(0xFF6E6E73))
    )
    Spacer(modifier = Modifier.height(16.dp))
    CircularProgressIndicator(
        modifier = Modifier.size(40.dp),
        color = BlueAccent,
        strokeWidth = 3.dp
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Connecting...",
        fontSize = 16.sp,
        color = themedColor(Color(0xFFE5E5EA), Color(0xFF1C1C1E))
    )
}

@Composable
private fun ProcessingContent(
    merchantName: String?,
    amount: String?,
    optionSummary: String?
) {
    Text(
        text = "WalletConnect Pay",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = themedColor(Color(0xFF8E8E93), Color(0xFF6E6E73))
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (merchantName != null) {
        Text(
            text = merchantName,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = themedColor(Color(0xFFFFFFFF), Color(0xFF000000))
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    if (amount != null) {
        Text(
            text = amount,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(Color(0xFFFFFFFF), Color(0xFF000000))
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    CircularProgressIndicator(
        modifier = Modifier.size(32.dp),
        color = BlueAccent,
        strokeWidth = 3.dp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Processing...",
        fontSize = 14.sp,
        color = themedColor(Color(0xFF8E8E93), Color(0xFF6E6E73))
    )

    if (optionSummary != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Paying with $optionSummary",
            fontSize = 13.sp,
            color = themedColor(Color(0xFF8E8E93), Color(0xFF6E6E73))
        )
    }
}

@Composable
private fun SuccessContent(
    merchantName: String?,
    amount: String?,
    optionSummary: String?,
    onDone: () -> Unit
) {
    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Payment Successful",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SuccessGreen
            )

            if (optionSummary != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = optionSummary,
                    fontSize = 14.sp,
                    color = themedColor(Color(0xFF8E8E93), Color(0xFF6E6E73))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(backgroundColor = BlueAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Done",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onDismiss: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Error",
        tint = ErrorRed,
        modifier = Modifier.size(48.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Payment Failed",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = ErrorRed
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        fontSize = 14.sp,
        color = themedColor(Color(0xFF8E8E93), Color(0xFF6E6E73)),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(backgroundColor = themedColor(Color(0xFF3A3A3C), Color(0xFFE5E5EA))),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Dismiss",
            color = themedColor(Color(0xFFFFFFFF), Color(0xFF000000)),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
