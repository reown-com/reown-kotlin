package com.reown.sample.pos.screens

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.reown.sample.pos.POSViewModel
import com.reown.sample.pos.PosEvent
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.reown.sample.pos.R


private enum class StepState { Inactive, InProgress, Done }

private sealed interface PaymentUiState {
    data object ScanToPay : PaymentUiState
    data object Success : PaymentUiState
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
    val context = LocalContext.current

    // Main UI state: show QR timeline until success
    var uiState by remember { mutableStateOf<PaymentUiState>(PaymentUiState.ScanToPay) }

    // --- Timeline step states ---
    var walletConnect by remember { mutableStateOf(StepState.InProgress) }   // starts connecting
    var sendTx by remember { mutableStateOf(StepState.Inactive) }      // waits for PaymentRequested
    var confirming by remember { mutableStateOf(StepState.Inactive) }      // waits for Broadcasted
    var checking by remember { mutableStateOf(StepState.Inactive) }      // waits for Broadcasted

    LaunchedEffect(Unit) {
        viewModel.posEventsFlow.collectLatest { e ->
            when (e) {
                PosEvent.Connected -> {
                    Log.d("POS", "Wallet connected")
                    walletConnect = StepState.Done
                    sendTx = StepState.InProgress
                }

                PosEvent.ConnectedRejected -> navigateToErrorScreen("Connection rejected by a user")

                // User is being asked to approve → start "Sending transaction…"
                PosEvent.PaymentRequested -> {
                    sendTx = StepState.Done
                    confirming = StepState.InProgress
                }

                // Broadcasted → mark "Transaction sent", start confirming & checking
                PosEvent.PaymentBroadcasted -> {
                    confirming = StepState.Done
                    checking = StepState.InProgress
                }

                // Final success → mark both in-progress steps as done and show Success UI
                is PosEvent.PaymentSuccessful -> {
                    checking = StepState.Done
                    uiState = PaymentUiState.Success
                }

                is PosEvent.PaymentRejected -> navigateToErrorScreen("Payment rejected: ${e.error}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
            Text("DTC Pay", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Crypto Payment Terminal", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.95f))
        }

        // Content
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState) {
                PaymentUiState.ScanToPay ->
                    ScanToPayContent(
                        qrUrl = qrUrl,
                        // show amount/token/network if you keep those on the VM
                        amount = viewModel.amount,
                        token = viewModel.token,
                        network = viewModel.network,
                        walletState = walletConnect,
                        sendState = sendTx,
                        confirmState = confirming,
                        checkState = checking
                    )

                PaymentUiState.Success -> SuccessContent { onReturnToStart() }
            }
        }

        // Footer pinned bottom
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Powered by DTC Pay", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


/* ---------- State UIs ---------- */

// 1) QR visible + details; per your note: no “Start Payment” button here.
@Composable
private fun ScanToPayContent(
    qrUrl: String,
    amount: String?,
    token: String?,
    network: String?,
    walletState: StepState,
    sendState: StepState,
    confirmState: StepState,
    checkState: StepState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Scan to Pay", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "Step 5: Customer scans QR",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // QR box
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
            ) { QrImage(data = qrUrl, size = 220.dp) }
        }

        Spacer(Modifier.height(16.dp))

        // --------- Timeline (always visible) ---------
        StatusRow(
            state = walletState,
            inProgressText = "Scan QR, waiting for wallet connection…",
            doneText = "Wallet connected"
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            state = sendState,
            inProgressText = "Sending transaction…",
            doneText = "Transaction sent"
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            state = confirmState,
            inProgressText = "Confirming transaction…",
            doneText = "Transaction confirmed"
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            state = checkState,
            inProgressText = "Checking the transaction status…",
            doneText = "Transaction confirmed"
        )

        Spacer(Modifier.height(20.dp))

        // Details card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!amount.isNullOrBlank()) {
                    Text("$${amount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                }
                val line2 = buildString {
                    append(token ?: "Token")
                    if (!network.isNullOrBlank()) append(" on $network")
                }
                Text(line2, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text("Network fee: $0.05", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

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
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (state) {
            StepState.Inactive -> {
                // subtle dot for steps not started yet
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, shape = CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(inProgressText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            StepState.InProgress -> {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = green)
                Spacer(Modifier.width(8.dp))
                Text(inProgressText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            StepState.Done -> {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = green, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(doneText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// 4) Success (no extra details)
@Composable
private fun SuccessContent(onReturnToStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val brandGreen = Color(0xFF0A8F5B)
        // A simple success glyph; swap for an icon if you have one
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = Color(0xFF0A8F5B),
            modifier = Modifier.size(72.dp)
        )

        Text(
            "Payment Successful!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF0A8F5B)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onReturnToStart() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = brandGreen)
        ) {
            Text("Start Again", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun QrImage(
    data: String,
    size: Dp = 220.dp
) {
    // Generate once per 'data'
    val bmp by remember(data) {
        mutableStateOf(generateQrBitmap(data, sizePx = (size.value * Resources.getSystem().displayMetrics.density).toInt()))
    }
    if (bmp != null) {
        Image(
            bitmap = bmp!!.asImageBitmap(),
            contentDescription = "QR",
            modifier = Modifier.size(size)
        )
    }
}

private fun generateQrBitmap(data: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1  // small quiet zone
        )
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