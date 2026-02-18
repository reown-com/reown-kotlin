package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.reown.sample.wallet.ui.common.SemiTransparentDialog
import com.reown.sample.wallet.ui.routes.Route
import com.reown.walletkit.client.Wallet
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentRoute(
    navController: NavHostController,
    paymentLink: String,
    onPaymentSuccess: () -> Unit = {},
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(paymentLink) {
        viewModel.setPaymentLink(paymentLink)
    }

    // WebView needs fullscreen, so handle it outside SemiTransparentDialog
    when (val state = uiState) {
        is PaymentUiState.WebViewDataCollection -> {
            WebViewDataCollectionContent(
                url = state.url,
                paymentInfo = state.paymentInfo,
                onComplete = { viewModel.onICWebViewComplete() },
                onError = { error ->
                    viewModel.onICWebViewError(error)
                },
                onClose = {
                    viewModel.goBackToOptions()
                }
            )
        }
        else -> {
            SemiTransparentDialog {
                when (state) {
                    is PaymentUiState.Loading -> {
                        LoadingContent()
                    }
                    is PaymentUiState.Options -> {
                        PaymentOptionsContent(
                            paymentInfo = state.paymentInfo,
                            options = state.options,
                            onOptionSelected = { optionId ->
                                viewModel.onOptionSelected(optionId)
                            },
                            onWhyInfoRequired = { viewModel.showWhyInfoRequired() },
                            onClose = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    is PaymentUiState.CollectingData -> {
                        CollectDataContent(
                            currentStepIndex = state.currentStepIndex,
                            totalSteps = state.totalSteps,
                            currentField = state.currentField,
                            currentValue = state.currentValue,
                            onValueSubmit = { fieldId, value ->
                                viewModel.submitFieldValue(fieldId, value)
                            },
                            onClose = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    is PaymentUiState.Summary -> {
                        SummaryContent(
                            paymentInfo = state.paymentInfo,
                            selectedOption = state.selectedOption,
                            onConfirm = { viewModel.confirmFromSummary() },
                            onClose = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    is PaymentUiState.WhyInfoRequired -> {
                        WhyInfoRequiredContent(
                            onBack = { viewModel.dismissWhyInfoRequired() },
                            onClose = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    is PaymentUiState.Processing -> {
                        ProcessingContent(
                            message = state.message,
                            onClose = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    is PaymentUiState.Success -> {
                        SuccessContent(
                            paymentInfo = state.paymentInfo,
                            onDone = {
                                viewModel.cancel()
                                onPaymentSuccess()
                                dismissPaymentDialog(navController)
                                Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                            },
                            onClose = {
                                viewModel.cancel()
                                onPaymentSuccess()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    is PaymentUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            },
                            onClose = {
                                viewModel.cancel()
                                dismissPaymentDialog(navController)
                            }
                        )
                    }
                    // WebViewDataCollection is handled outside SemiTransparentDialog
                    is PaymentUiState.WebViewDataCollection -> { /* handled above */ }
                }
            }
        }
    }
}

private fun dismissPaymentDialog(navController: NavHostController) {
    if (!navController.popBackStack(Route.Connections.path, inclusive = false)) {
        navController.popBackStack()
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF3396FF)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading payment options...",
            style = TextStyle(fontSize = 16.sp, color = Color.White)
        )
    }
}

@Composable
private fun PaymentOptionsContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    options: List<Wallet.Model.PaymentOption>,
    onOptionSelected: (String) -> Unit,
    onWhyInfoRequired: () -> Unit,
    onClose: () -> Unit
) {
    var selectedOptionId by remember { mutableStateOf<String?>(options.firstOrNull()?.id) }
    val anyOptionHasCollectData = options.any { it.collectData != null }
    val selectedOption = options.find { it.id == selectedOptionId }
    val selectedHasCollectData = selectedOption?.collectData != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        // Header: "Why info required?" pill (left) + X close (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (anyOptionHasCollectData) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0F0))
                        .clickable { onWhyInfoRequired() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Why info required?",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Merchant icon and payment title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantIcon(paymentInfo = paymentInfo, size = 64.dp)

            Spacer(modifier = Modifier.height(16.dp))

            PaymentTitle(paymentInfo = paymentInfo, fontSize = 18)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Flat list of option cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                PaymentOptionCard(
                    option = option,
                    isSelected = selectedOptionId == option.id,
                    onClick = { selectedOptionId = option.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom button: "Pay" or "Continue"
        val buttonText = if (selectedHasCollectData) {
            "Continue"
        } else {
            val buttonAmount = paymentInfo?.let {
                formatDisplayAmount(
                    value = it.amount.value,
                    decimals = it.amount.display?.decimals ?: 2,
                    symbol = it.amount.display?.assetSymbol ?: it.amount.unit
                )
            } ?: ""
            "Pay $buttonAmount"
        }

        Button(
            onClick = { selectedOptionId?.let { onOptionSelected(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedOptionId != null,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF),
                disabledBackgroundColor = Color(0xFFB3D9FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = buttonText,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun PaymentOptionCard(
    option: Wallet.Model.PaymentOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFEBF4FF) else Color(0xFFF5F5F5)
    val borderColor = if (isSelected) Color(0xFF3396FF) else Color.Transparent
    val borderWidth = if (isSelected) 1.5.dp else 0.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Asset icon with network badge
            option.amount.display?.iconUrl?.let { iconUrl ->
                TokenIconWithNetwork(
                    tokenIconUrl = iconUrl,
                    networkIconUrl = option.amount.display?.networkIconUrl,
                    tokenIconSize = 36.dp,
                    networkIconSize = 16.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Token amount only (no network text name)
            val display = option.amount.display
            val tokenAmount = formatTokenAmount(
                value = option.amount.value,
                decimals = display?.decimals ?: 18,
                symbol = display?.assetSymbol ?: "Token"
            )

            Text(
                text = tokenAmount,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
        }

        // "Info required" badge if option has collectData
        if (option.collectData != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8E8E8))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Info required",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF888888)
                    )
                )
            }
        }
    }
}

/**
 * Displays a token icon with a network icon badge in the bottom-right corner.
 */
@Composable
private fun TokenIconWithNetwork(
    tokenIconUrl: String,
    networkIconUrl: String?,
    tokenIconSize: Dp,
    networkIconSize: Dp
) {
    Box {
        AsyncImage(
            model = tokenIconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(tokenIconSize)
                .clip(CircleShape)
        )
        networkIconUrl?.let { networkUrl ->
            AsyncImage(
                model = networkUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(networkIconSize)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun MerchantIcon(paymentInfo: Wallet.Model.PaymentInfo?, size: Dp) {
    paymentInfo?.merchant?.iconUrl?.let { iconUrl ->
        AsyncImage(
            model = iconUrl,
            contentDescription = "Merchant icon",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(Color.Black)
        )
    } ?: run {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = paymentInfo?.merchant?.name?.take(1)?.uppercase() ?: "P",
                style = TextStyle(
                    fontSize = (size.value * 0.44f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun PaymentTitle(paymentInfo: Wallet.Model.PaymentInfo?, fontSize: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val merchantName = paymentInfo?.merchant?.name ?: "Merchant"
        val displayAmount = paymentInfo?.let {
            formatDisplayAmount(
                value = it.amount.value,
                decimals = it.amount.display?.decimals ?: 2,
                symbol = it.amount.display?.assetSymbol ?: it.amount.unit
            )
        } ?: ""

        Text(
            text = "Pay $displayAmount to $merchantName",
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        )

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size((fontSize - 2).dp)
                .clip(CircleShape)
                .background(Color(0xFF3396FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified",
                tint = Color.White,
                modifier = Modifier.size((fontSize - 8).dp)
            )
        }
    }
}

@Composable
private fun SummaryContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    selectedOption: Wallet.Model.PaymentOption,
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        // Close button at top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Merchant icon and payment title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantIcon(paymentInfo = paymentInfo, size = 64.dp)
            Spacer(modifier = Modifier.height(16.dp))
            PaymentTitle(paymentInfo = paymentInfo, fontSize = 18)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Pay with" row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pay with",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFF666666)
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val display = selectedOption.amount.display
                val tokenAmount = formatTokenAmount(
                    value = selectedOption.amount.value,
                    decimals = display?.decimals ?: 18,
                    symbol = display?.assetSymbol ?: "Token"
                )

                Text(
                    text = tokenAmount,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                display?.iconUrl?.let { iconUrl ->
                    TokenIconWithNetwork(
                        tokenIconUrl = iconUrl,
                        networkIconUrl = display.networkIconUrl,
                        tokenIconSize = 24.dp,
                        networkIconSize = 12.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm button
        val buttonAmount = paymentInfo?.let {
            formatDisplayAmount(
                value = it.amount.value,
                decimals = it.amount.display?.decimals ?: 2,
                symbol = it.amount.display?.assetSymbol ?: it.amount.unit
            )
        } ?: ""

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Pay $buttonAmount",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun WhyInfoRequiredContent(
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        // Header: back arrow (left) + X close (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = 90f)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Why we need your information?",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For regulatory compliance, we collect basic information on your first payment: full name, date of birth, and place of birth.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 22.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "This information is tied to your wallet address and this specific network. If you use the same wallet on this network again, you won't need to provide it again.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                lineHeight = 22.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Got it!",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

/**
 * Format amount with proper decimals and symbol.
 */
private fun formatAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val safeDecimals = decimals.coerceIn(0, 18)
        val divisor = BigDecimal.TEN.pow(safeDecimals)
        val formattedValue = rawValue.divide(divisor, safeDecimals.coerceAtMost(2), RoundingMode.HALF_UP)
        "$formattedValue $symbol"
    } catch (e: Exception) {
        "$value $symbol"
    }
}

/**
 * Format display amount with the appropriate currency symbol.
 */
private fun formatDisplayAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val safeDecimals = decimals.coerceIn(0, 18)
        val divisor = BigDecimal.TEN.pow(safeDecimals)
        val formattedValue = rawValue.divide(divisor, 2, RoundingMode.HALF_UP)
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formatted = numberFormat.format(formattedValue)
        val currencySymbol = getCurrencySymbol(symbol)
        "$currencySymbol$formatted"
    } catch (e: Exception) {
        val currencySymbol = getCurrencySymbol(symbol)
        "$currencySymbol$value"
    }
}

/**
 * Get currency symbol for a given currency code.
 */
private fun getCurrencySymbol(currencyCode: String): String {
    return when (currencyCode.uppercase()) {
        "USD" -> "$"
        "EUR" -> "\u20AC"
        "GBP" -> "\u00A3"
        "JPY" -> "\u00A5"
        "CNY" -> "\u00A5"
        "KRW" -> "\u20A9"
        "INR" -> "\u20B9"
        "RUB" -> "\u20BD"
        "BRL" -> "R$"
        "CHF" -> "CHF "
        "CAD" -> "CA$"
        "AUD" -> "A$"
        else -> "$currencyCode "
    }
}

/**
 * Format token amount with symbol.
 */
private fun formatTokenAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val safeDecimals = decimals.coerceIn(0, 18)
        val divisor = BigDecimal.TEN.pow(safeDecimals)
        val formattedValue = rawValue.divide(divisor, 4, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        val formatted = java.text.NumberFormat.getNumberInstance(Locale.US).format(BigDecimal(formattedValue))
        "$formatted $symbol"
    } catch (e: Exception) {
        "$value $symbol"
    }
}

/**
 * Format expiration timestamp to human-readable text.
 */
private fun formatExpiration(expiresAt: Long): String {
    val now = System.currentTimeMillis() / 1000
    val remainingSeconds = expiresAt - now
    
    return if (remainingSeconds <= 0) {
        "Expired"
    } else if (remainingSeconds < 60) {
        "Expires in ${remainingSeconds}s"
    } else if (remainingSeconds < 3600) {
        val minutes = remainingSeconds / 60
        "Expires in ${minutes}m"
    } else {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        "Expires ${dateFormat.format(Date(expiresAt * 1000))}"
    }
}


@Composable
private fun ProcessingContent(
    message: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button at top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Animated Reown logo
        AnimatedReownLogo()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Confirming your payment...",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AnimatedReownLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")
    
    // Create staggered pulse animations for each block
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )
    
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale3"
    )
    
    val scale4 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale4"
    )
    
    // Logo colors
    val grayColor = Color(0xFF9EA0A6)
    val blueColor = Color(0xFF3396FF)
    val lightGrayColor = Color(0xFFD4D5D9)
    val darkColor = Color(0xFF2D3131)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: gray rectangle + blue rounded square
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gray horizontal rectangle
            Box(
                modifier = Modifier
                    .scale(scale1)
                    .width(40.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(grayColor)
            )
            
            // Blue rounded square
            Box(
                modifier = Modifier
                    .scale(scale2)
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(blueColor)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Bottom row: light gray square + dark circle
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Light gray rounded square
            Box(
                modifier = Modifier
                    .scale(scale3)
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(lightGrayColor)
            )
            
            // Dark circle
            Box(
                modifier = Modifier
                    .scale(scale4)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(darkColor)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    onDone: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button at top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Green checkmark circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Success message with payment details
        val displayAmount = paymentInfo?.let {
            formatDisplayAmount(
                value = it.amount.value,
                decimals = it.amount.display?.decimals ?: 2,
                symbol = it.amount.display?.assetSymbol ?: it.amount.unit
            )
        } ?: ""
        val merchantName = paymentInfo?.merchant?.name ?: "Merchant"

        Text(
            text = "You've paid $displayAmount to $merchantName",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // Got it button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Got it!",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button at top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Red/orange circle with exclamation mark
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFD85140)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error message
        Text(
            text = message,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Try again button
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Try again",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

// ==================== Information Capture Components ====================

@Composable
private fun CollectDataContent(
    currentStepIndex: Int,
    totalSteps: Int,
    currentField: Wallet.Model.CollectDataField,
    currentValue: String,
    onValueSubmit: (String, String) -> Unit,
    onClose: () -> Unit
) {
    var inputValue by remember(currentField.id) { mutableStateOf(currentValue) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        // Header with back, progress, and close
        CollectDataHeader(
            currentStep = currentStepIndex,
            totalSteps = totalSteps,
            onClose = onClose
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Question text
        Text(
            text = currentField.name,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Input field based on type
        when (currentField.fieldType) {
            Wallet.Model.CollectDataFieldType.TEXT -> {
                TextFieldInput(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    placeholder = currentField.name
                )
            }
            Wallet.Model.CollectDataFieldType.DATE -> {
                DatePickerInput(
                    value = inputValue,
                    onValueChange = { inputValue = it }
                )
            }

            Wallet.Model.CollectDataFieldType.CHECKBOX -> {

            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Continue button
        Button(
            onClick = { onValueSubmit(currentField.id, inputValue) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = inputValue.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF),
                disabledBackgroundColor = Color(0xFFB3D9FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun CollectDataHeader(
    currentStep: Int,
    totalSteps: Int,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Empty spacer for balance
        Spacer(modifier = Modifier.size(48.dp))
        
        // Progress indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalSteps) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i <= currentStep) Color(0xFF3396FF) else Color(0xFFE0E0E0)
                        )
                )
            }
        }
        
        // Close button
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun TextFieldInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = Color.Gray
            )
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.Black,
            backgroundColor = Color(0xFFF5F5F5),
            focusedBorderColor = Color(0xFF3396FF),
            unfocusedBorderColor = Color.Transparent,
            cursorColor = Color(0xFF3396FF)
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun DatePickerInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    // Default values
    val defaultYear = "1990"
    val defaultMonth = "01"
    val defaultDay = "01"
    
    // Parse existing value (YYYY-MM-DD format) or use defaults
    val initialYear: String
    val initialMonth: String
    val initialDay: String
    
    if (value.isNotBlank() && value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
        val parts = value.split("-")
        initialYear = parts[0]
        initialMonth = parts[1]
        initialDay = parts[2]
    } else {
        initialYear = defaultYear
        initialMonth = defaultMonth
        initialDay = defaultDay
    }
    
    var selectedYear by remember(value) { mutableStateOf(initialYear) }
    var selectedMonth by remember(value) { mutableStateOf(initialMonth) }
    var selectedDay by remember(value) { mutableStateOf(initialDay) }
    
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val days = (1..31).map { it.toString().padStart(2, '0') }
    val years = (1920..2025).map { it.toString() }.reversed()
    
    // Update combined value when any selection changes (ISO 8601 format: YYYY-MM-DD)
    LaunchedEffect(selectedYear, selectedMonth, selectedDay) {
        val formattedDate = "${selectedYear}-${selectedMonth}-${selectedDay}"
        onValueChange(formattedDate)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month picker
        DateWheelPicker(
            pickerItems = months,
            selectedItem = months.getOrNull((selectedMonth.toIntOrNull() ?: 1) - 1) ?: "January",
            onItemSelected = { month ->
                selectedMonth = (months.indexOf(month) + 1).toString().padStart(2, '0')
            },
            modifier = Modifier.weight(1.5f)
        )

        // Day picker
        DateWheelPicker(
            pickerItems = days,
            selectedItem = selectedDay.takeIf { it in days } ?: "01",
            onItemSelected = { selectedDay = it },
            modifier = Modifier.weight(0.8f)
        )

        // Year picker
        DateWheelPicker(
            pickerItems = years,
            selectedItem = selectedYear.takeIf { it in years } ?: "2000",
            onItemSelected = { selectedYear = it },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateWheelPicker(
    pickerItems: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = pickerItems.indexOf(selectedItem).coerceAtLeast(0)
    val itemHeight = 40.dp
    val visibleItems = 5
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // The center picker item index equals firstVisibleItemIndex because we have
    // (visibleItems / 2) padding spacers at the top that offset the center
    val centerPickerIndex = listState.firstVisibleItemIndex

    // Update selection when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val clampedIndex = centerPickerIndex.coerceIn(0, pickerItems.size - 1)
            if (pickerItems.getOrNull(clampedIndex) != selectedItem) {
                onItemSelected(pickerItems[clampedIndex])
            }
        }
    }

    // Scroll to selected item when it changes externally
    LaunchedEffect(selectedItem) {
        val targetIndex = pickerItems.indexOf(selectedItem).coerceAtLeast(0)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.height(itemHeight * visibleItems),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            // Padding items at top to allow first item to be centered
            items(List(visibleItems / 2) { it }) {
                Spacer(modifier = Modifier.height(itemHeight))
            }

            itemsIndexed(pickerItems) { index, item ->
                // Item is visually centered if its index matches centerPickerIndex
                val isVisuallySelected = index == centerPickerIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = TextStyle(
                            fontSize = if (isVisuallySelected) 20.sp else 16.sp,
                            fontWeight = if (isVisuallySelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isVisuallySelected) Color.Black else Color(0xFFAAAAAA)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Padding items at bottom to allow last item to be centered
            items(List(visibleItems / 2) { it }) {
                Spacer(modifier = Modifier.height(itemHeight))
            }
        }

        // Selection highlight overlay
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .background(Color(0x10000000), RoundedCornerShape(8.dp))
        )
    }
}

// ==================== WebView Information Capture Components ====================

/**
 * JavaScript interface for WebView → Wallet communication.
 * WebView calls: window.AndroidWallet.onDataCollectionComplete(jsonString)
 */
class AndroidWalletJsInterface(
    private val onComplete: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onDataCollectionComplete(jsonData: String) {
        Log.d("AndroidWalletJS", "=== WebView -> Wallet Communication ===")
        Log.d("AndroidWalletJS", "Raw message received: $jsonData")
        try {
            val json = JSONObject(jsonData)
            val type = json.optString("type")
            val success = json.optBoolean("success", false)
            Log.d("AndroidWalletJS", "Parsed message - type: $type, success: $success")

            when {
                type == "IC_COMPLETE" && success -> {
                    Log.d("AndroidWalletJS", "SUCCESS: Information capture completed successfully")
                    onComplete()
                }
                type == "IC_ERROR" -> {
                    val error = json.optString("error", "Unknown error")
                    Log.e("AndroidWalletJS", "ERROR: Information capture failed - $error")
                    onError(error)
                }
                else -> {
                    Log.w("AndroidWalletJS", "WARNING: Unknown message type received: $type")
                    onError("Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidWalletJS", "ERROR: Failed to parse WebView message", e)
            onError("Failed to parse message: ${e.message}")
        }
        Log.d("AndroidWalletJS", "=== End WebView Communication ===")
    }
}

@Composable
private fun WebViewDataCollectionContent(
    url: String,
    paymentInfo: Wallet.Model.PaymentInfo?,
    onComplete: () -> Unit,
    onError: (String) -> Unit,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnError by rememberUpdatedState(onError)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Error display
            loadError?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFD85140),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { loadError = null },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3396FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
                return@Column
            }

            // WebView wrapped in FrameLayout to fix Compose rendering issues
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    // Wrap WebView in FrameLayout to isolate from Compose rendering
                    android.widget.FrameLayout(ctx).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )

                        val webView = WebView(ctx).apply {
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )

                            // Enable WebView debugging - inspect via chrome://inspect on desktop
                            WebView.setWebContentsDebuggingEnabled(true)

                            // Set dark background to match the web content theme
                            setBackgroundColor(0xFF141414.toInt())

                            // Fix for text input issues in Compose - disable nested scrolling
                            isNestedScrollingEnabled = false
                            overScrollMode = android.view.View.OVER_SCROLL_NEVER

                            // Ensure proper focus handling
                            isFocusable = true
                            isFocusableInTouchMode = true

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }

                            // Add JS interface for IC completion
                            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                            addJavascriptInterface(
                                AndroidWalletJsInterface(
                                    onComplete = {
                                        Log.d("PaymentWebView", "IC_COMPLETE received - WebView data collection successful")
                                        mainHandler.post {
                                            Log.d("PaymentWebView", "Proceeding to payment options")
                                            currentOnComplete()
                                        }
                                    },
                                    onError = { error ->
                                        Log.e("PaymentWebView", "IC_ERROR received - WebView error: $error")
                                        mainHandler.post {
                                            Log.e("PaymentWebView", "Showing error to user: $error")
                                            currentOnError(error)
                                        }
                                    }
                                ),
                                "AndroidWallet"
                            )

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val requestUrl = request?.url?.toString() ?: return false
                                    val originalHost = Uri.parse(url).host

                                    // If the URL is from a different host, open in external browser
                                    if (Uri.parse(requestUrl).host != originalHost) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            Log.e("PaymentWebView", "Failed to open external URL: $requestUrl", e)
                                        }
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = error?.description?.toString() ?: "Failed to load"
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.cancel()
                                    loadError = "SSL certificate error"
                                }
                            }

                            Log.d("PaymentWebView", "Loading URL: $url")
                            loadUrl(url)
                        }

                        addView(webView)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Centered loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141414)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = Color(0xFF3396FF)
                )
            }
        }

        // Floating close button overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
