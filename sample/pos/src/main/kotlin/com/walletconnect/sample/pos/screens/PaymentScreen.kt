package com.walletconnect.sample.pos.screens

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.PosEvent
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.components.WalletConnectLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private sealed interface PaymentUiState {
    data object WaitingForScan : PaymentUiState
    data object Processing : PaymentUiState
}

@Composable
fun PaymentScreen(
    viewModel: POSViewModel,
    qrUrl: String,
    expiresAt: Long,
    onReturnToStart: () -> Unit,
    onNavigateToAmount: () -> Unit,
    navigateToErrorScreen: (error: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf<PaymentUiState>(PaymentUiState.WaitingForScan) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }

    // Listen for payment events
    LaunchedEffect(Unit) {
        viewModel.posEventsFlow.collectLatest { event ->
            when (event) {
                PosEvent.PaymentRequested -> {
                    // requires_action - wallet scanned but still on QR screen
                }
                PosEvent.PaymentProcessing -> {
                    uiState = PaymentUiState.Processing
                }
                is PosEvent.PaymentSuccess -> {
                    // Navigation handled by ViewModel nav events
                }
                is PosEvent.PaymentError -> {
                    navigateToErrorScreen(event.error)
                }
            }
        }
    }

    // Countdown timer
    LaunchedEffect(expiresAt) {
        if (expiresAt <= 0L) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis() / 1000
            val remaining = expiresAt - now
            if (remaining <= 0) {
                navigateToErrorScreen("expired")
                break
            }
            remainingSeconds = remaining
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val onCancel = {
            viewModel.cancelPayment()
            onReturnToStart()
        }

        PosHeader(onBack = onCancel)

        when (uiState) {
            PaymentUiState.WaitingForScan -> {
                ScanContent(
                    qrUrl = qrUrl,
                    displayAmount = viewModel.getDisplayAmount(),
                    remainingSeconds = remainingSeconds,
                    onCancel = onCancel
                )
            }
            PaymentUiState.Processing -> {
                ProcessingContent(
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun ScanContent(
    qrUrl: String,
    displayAmount: String,
    remainingSeconds: Long,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "Scan to pay",
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textTertiary
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing2))

        if (displayAmount.isNotBlank()) {
            Text(
                text = displayAmount,
                style = WCTheme.typography.h3Regular,
                color = WCTheme.colors.textPrimary
            )
            Spacer(Modifier.height(WCTheme.spacing.spacing5))
        }

        // QR Code
        QrImage(data = qrUrl, size = 240.dp)

        Spacer(Modifier.height(WCTheme.spacing.spacing4))

        // Expiration countdown
        if (remainingSeconds > 0) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timeText = "${minutes}:${String.format("%02d", seconds)}s"
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = WCTheme.colors.textSecondary)) {
                        append("Payment expires in ")
                    }
                    withStyle(SpanStyle(color = WCTheme.colors.textAccentPrimary)) {
                        append(timeText)
                    }
                },
                style = WCTheme.typography.bodyLgRegular
            )
        }

        Spacer(Modifier.weight(1f))

        CloseButton(onClick = onCancel)

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun ProcessingContent(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        WalletConnectLoader(size = 140.dp)

        Spacer(Modifier.height(WCTheme.spacing.spacing5))

        Text(
            text = "Waiting for payment confirmation...",
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        CloseButton(onClick = onCancel)

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
fun QrImage(data: String, size: Dp = 220.dp) {
    val bmp by remember(data) {
        mutableStateOf(
            generateQrBitmap(
                data,
                sizePx = (size.value * Resources.getSystem().displayMetrics.density).toInt()
            )
        )
    }
    if (bmp != null) {
        Image(
            bitmap = bmp!!.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(size)
        )
    }
}

private fun generateQrBitmap(data: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bmp
    } catch (_: Throwable) {
        null
    }
}
