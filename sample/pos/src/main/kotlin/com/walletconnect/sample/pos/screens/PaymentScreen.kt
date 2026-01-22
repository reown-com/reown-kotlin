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
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.PosEvent
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import coil3.compose.AsyncImage
import com.walletconnect.sample.pos.R

// Brand color
private val BrandColor = Color(0xFF0988F0)

private enum class StepState { Inactive, InProgress, Done }

private sealed interface PaymentUiState {
    data object WaitingForScan : PaymentUiState
    data class Success(val paymentId: String, val info: Pos.PaymentInfo?) : PaymentUiState
}

@Composable
fun PaymentScreen(
    viewModel: POSViewModel,
    qrUrl: String,
    onReturnToStart: () -> Unit,
    navigateToErrorScreen: (error: String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    uiState = PaymentUiState.Success(paymentId = event.paymentId, info = event.info)
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
            .background(Color.White)
    ) {
        // Header
        PosHeader()

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
                    confirmingState = confirmingStep,
                    onCancel = {
                        viewModel.cancelPayment()
                        onReturnToStart()
                    }
                )

                is PaymentUiState.Success -> SuccessContent(
                    info = state.info,
                    onReturnToStart = onReturnToStart
                )
            }
        }

        // Bottom padding for navigation bars
        Spacer(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(16.dp)
        )
    }
}

@Composable
private fun ScanToPayContent(
    qrUrl: String,
    displayAmount: String,
    scanState: StepState,
    processingState: StepState,
    confirmingState: StepState,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Close button in top right
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Main content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Scan to Pay",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(Modifier.height(16.dp))

            // QR Code (smaller)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF5F5F5),
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrImage(data = qrUrl, size = 170.dp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Amount display (matches QR code width)
            if (displayAmount.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandColor.copy(alpha = 0.1f),
                    modifier = Modifier.width(194.dp) // 170dp QR + 24dp padding
                ) {
                    Text(
                        displayAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = BrandColor,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
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

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusRow(
    state: StepState,
    inProgressText: String,
    doneText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state) {
            StepState.Inactive -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFFE0E0E0), shape = CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    inProgressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBBBBBB)
                )
            }

            StepState.InProgress -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = BrandColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    inProgressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            StepState.Done -> {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BrandColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    doneText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    info: Pos.PaymentInfo?,
    onReturnToStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(BrandColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = BrandColor,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Payment Successful",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        )

        Spacer(Modifier.height(32.dp))

        // Payment Info Card
        if (info != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8F9FA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Token icon with network badge
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val iconUrl = info.iconUrl
                        if (iconUrl != null) {
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = info.assetSymbol,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(BrandColor.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    info.assetSymbol?.take(2) ?: "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColor
                                )
                            }
                        }

                        // Network icon badge
                        val networkIconUrl = info.networkIconUrl
                        if (networkIconUrl != null) {
                            AsyncImage(
                                model = networkIconUrl,
                                contentDescription = info.networkName,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Amount
                    val formattedAmount = info.formatAmount()
                    if (formattedAmount.isNotBlank()) {
                        Text(
                            formattedAmount,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Token name & Network
                    val assetName = info.assetName
                    val networkName = info.networkName
                    val subtitle = buildString {
                        if (assetName != null) append(assetName)
                        if (networkName != null) {
                            if (assetName != null) append(" on ")
                            append(networkName)
                        }
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF6B7280)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Divider
                    HorizontalDivider(
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Transaction Hash - clickable to copy
                    val clipboardManager = LocalClipboardManager.current
                    var showCopied by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(info.txHash))
                                showCopied = true
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                        Text(
                            if (showCopied) "Copied!" else truncateTxHash(info.txHash),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (showCopied) BrandColor else Color(0xFF374151)
                        )
                    }

                    // Reset copied state after delay
                    LaunchedEffect(showCopied) {
                        if (showCopied) {
                            kotlinx.coroutines.delay(2000)
                            showCopied = false
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onReturnToStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandColor)
        ) {
            Text(
                "New Payment",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun truncateTxHash(txHash: String): String {
    return if (txHash.length > 16) {
        "${txHash.take(8)}...${txHash.takeLast(6)}"
    } else {
        txHash
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
