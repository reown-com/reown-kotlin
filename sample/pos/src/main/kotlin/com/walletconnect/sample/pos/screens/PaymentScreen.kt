package com.walletconnect.sample.pos.screens

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.PosEvent
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.walletconnect.sample.pos.R

private enum class StepState { Inactive, InProgress, Done }

private sealed interface PaymentUiState {
    data object WaitingForScan : PaymentUiState
    data class Success(val paymentId: String) : PaymentUiState
}

@Composable
fun PaymentScreen(
    viewModel: POSViewModel,
    qrUrl: String,
    onReturnToStart: () -> Unit,
    navigateToErrorScreen: (error: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val brandGreen = Color(0xFF0A8F5B)

    var uiState by remember { mutableStateOf<PaymentUiState>(PaymentUiState.WaitingForScan) }

    // Timeline step states
    var scanStep by remember { mutableStateOf(StepState.InProgress) }
    var processingStep by remember { mutableStateOf(StepState.Inactive) }
    var confirmingStep by remember { mutableStateOf(StepState.Inactive) }

    LaunchedEffect(Unit) {
        viewModel.posEventsFlow.collectLatest { event ->
            when (event) {
                PosEvent.PaymentRequested -> {
                    scanStep = StepState.Done
                    processingStep = StepState.InProgress
                }

                PosEvent.PaymentProcessing -> {
                    processingStep = StepState.Done
                    confirmingStep = StepState.InProgress
                }

                is PosEvent.PaymentSuccess -> {
                    confirmingStep = StepState.Done
                    uiState = PaymentUiState.Success(paymentId = event.paymentId)
                }

                is PosEvent.PaymentError -> {
                    navigateToErrorScreen(event.error)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brandGreen)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "WalletConnect Pay",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "POS Sample App",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.95f)
            )
        }

        // Content
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = uiState) {
                PaymentUiState.WaitingForScan -> ScanToPayContent(
                    qrUrl = qrUrl,
                    displayAmount = viewModel.getDisplayAmount(),
                    scanState = scanStep,
                    processingState = processingStep,
                    confirmingState = confirmingStep
                )

                is PaymentUiState.Success -> SuccessContent(
                    paymentId = state.paymentId,
                    onReturnToStart = onReturnToStart
                )
            }
        }

        // Footer
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState is PaymentUiState.WaitingForScan) {
                    Button(
                        onClick = {
                            viewModel.cancelPayment()
                            onReturnToStart()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Cancel Payment")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "Powered by WalletConnect",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanToPayContent(
    qrUrl: String,
    displayAmount: String,
    scanState: StepState,
    processingState: StepState,
    confirmingState: StepState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            "Scan to Pay",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Customer scans QR code with wallet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // QR Code
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            tonalElevation = 1.dp
        ) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 220.dp, minHeight = 220.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                QrImage(data = qrUrl, size = 220.dp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Amount display
        if (displayAmount.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    displayAmount,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // Timeline
        StatusRow(
            state = scanState,
            inProgressText = "Waiting for wallet scan…",
            doneText = "Payment initiated"
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            state = processingState,
            inProgressText = "Processing payment…",
            doneText = "Payment processing"
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            state = confirmingState,
            inProgressText = "Confirming transaction…",
            doneText = "Payment confirmed"
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusRow(
    state: StepState,
    inProgressText: String,
    doneText: String
) {
    val green = Color(0xFF0A8F5B)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state) {
            StepState.Inactive -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, shape = CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    inProgressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            StepState.InProgress -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = green
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    inProgressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StepState.Done -> {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    doneText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    paymentId: String,
    onReturnToStart: () -> Unit
) {
    val brandGreen = Color(0xFF0A8F5B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = brandGreen,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Payment Successful!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = brandGreen
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Payment ID",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            paymentId,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onReturnToStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = brandGreen)
        ) {
            Text(
                "New Payment",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun QrImage(
    data: String,
    size: Dp = 220.dp
) {
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
