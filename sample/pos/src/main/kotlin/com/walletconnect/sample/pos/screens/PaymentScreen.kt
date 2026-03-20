package com.walletconnect.sample.pos.screens

import android.nfc.NfcAdapter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCTheme
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.PosEvent
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.StyledQrCode
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
    onBack: () -> Unit,
    onReturnToStart: () -> Unit,
    onNavigateToAmount: () -> Unit,
    navigateToErrorScreen: (error: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf<PaymentUiState>(PaymentUiState.WaitingForScan) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    val displayAmount by viewModel.displayAmount.collectAsState()

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

    // Countdown timer — guard against navigating to error if already processing/succeeded
    LaunchedEffect(expiresAt) {
        if (expiresAt <= 0L) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis() / 1000
            val remaining = expiresAt - now
            if (remaining <= 0) {
                if (uiState is PaymentUiState.WaitingForScan) {
                    navigateToErrorScreen("expired")
                }
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

        PosHeader(onBack = {
            viewModel.cancelPayment()
            onBack()
        })

        when (uiState) {
            PaymentUiState.WaitingForScan -> {
                ScanContent(
                    qrUrl = qrUrl,
                    displayAmount = displayAmount,
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
    val context = LocalContext.current
    val hasNfc = remember {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        nfcAdapter != null && nfcAdapter.isEnabled
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasNfc) {
            Spacer(Modifier.height(WCTheme.spacing.spacing8))
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (hasNfc) {

            // NFC contactless icon
            Image(
                painter = painterResource(R.drawable.ic_nfc_contactless),
                contentDescription = "NFC contactless",
                modifier = Modifier.size(60.dp)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing5))

            Text(
                text = "Open your wallet app and tap",
                style = WCTheme.typography.bodyXlRegular,
                color = WCTheme.colors.textSecondary
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing5))
        } else {
            Text(
                text = "Scan to pay",
                style = WCTheme.typography.bodyLgRegular,
                color = WCTheme.colors.textTertiary
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))
        }

        // Amount with cents in tertiary color
        if (displayAmount.isNotBlank()) {
            AmountText(displayAmount = displayAmount)
            Spacer(Modifier.height(WCTheme.spacing.spacing8))
        }

        if (hasNfc) {
            // Divider with "Or scan the QR code"
            OrScanDivider()
            Spacer(Modifier.height(WCTheme.spacing.spacing8))
        }

        // QR Code
        StyledQrCode(data = qrUrl, size = 320.dp)

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

        Text(
            text = "or tap to pay",
            style = WCTheme.typography.bodyMdRegular,
            color = WCTheme.colors.textTertiary
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

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
private fun AmountText(displayAmount: String) {
    Text(
        text = displayAmount,
        style = WCTheme.typography.h1Medium,
        color = WCTheme.colors.textPrimary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun OrScanDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = WCTheme.colors.borderPrimary
        )
        Text(
            text = "Or scan the QR code",
            style = WCTheme.typography.bodyXlRegular,
            color = WCTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing2)
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = WCTheme.colors.borderPrimary
        )
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

