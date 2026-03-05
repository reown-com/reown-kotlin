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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route
import com.reown.walletkit.client.Wallet
import org.json.JSONObject
import java.math.BigDecimal
import com.reown.sample.wallet.ui.common.WalletConnectLoader
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

    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "paymentState"
    ) { state ->
        when (state) {
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
                    message = state.message
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
                    }
                )
            }
            is PaymentUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    errorType = state.errorType,
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    },
                    onScanNewQrCode = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                        navController.navigate(Route.ScannerOptions.path)
                    }
                )
            }
        }
    }
}

private fun dismissPaymentDialog(navController: NavHostController) {
    if (!navController.popBackStack(Route.Wallets.path, inclusive = false)) {
        navController.popBackStack()
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(WCTheme.colors.bgPrimary)
            .padding(horizontal = WCTheme.spacing.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WalletConnectLoader(size = 120.dp)
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
        Text(
            text = "Preparing your payment...",
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
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
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Header: "?" icon button (left) + X close (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (anyOptionHasCollectData) {
                ModalIconButton(
                    iconRes = R.drawable.ic_question_mark,
                    contentDescription = "Why info needed",
                    onClick = onWhyInfoRequired
                )
            } else {
                Spacer(modifier = Modifier.size(38.dp))
            }

            ModalCloseButton(onClick = onClose)
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        // Merchant icon and payment title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantIcon(paymentInfo = paymentInfo, size = 64.dp)

            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

            PaymentTitle(paymentInfo = paymentInfo)
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        // Flat list of option cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
        ) {
            options.forEach { option ->
                PaymentOptionCard(
                    option = option,
                    isSelected = selectedOptionId == option.id,
                    onClick = { selectedOptionId = option.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

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

        val isEnabled = selectedOptionId != null
        PrimaryActionButton(
            text = buttonText,
            enabled = isEnabled,
            onClick = { selectedOptionId?.let { onOptionSelected(it) } }
        )
    }
}

@Composable
private fun PaymentOptionCard(
    option: Wallet.Model.PaymentOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) WCTheme.colors.foregroundAccentPrimary10 else WCTheme.colors.foregroundPrimary,
        label = "optionBg"
    )
    val borderColor = if (isSelected) WCTheme.colors.borderAccentPrimary else Color.Transparent
    val borderWidth = 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(WCTheme.borderRadius.shapeLarge)
            .border(borderWidth, borderColor, WCTheme.borderRadius.shapeLarge)
            .background(animatedBg)
            .clickable { onClick() }
            .padding(horizontal = WCTheme.spacing.spacing4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Asset icon with network badge
            option.amount.display?.iconUrl?.let { iconUrl ->
                val networkBadgeStrokeColor = if (isSelected) {
                    WCTheme.colors.foregroundAccentPrimary10Solid
                } else {
                    WCTheme.colors.foregroundPrimary
                }

                TokenIconWithNetwork(
                    tokenIconUrl = iconUrl,
                    networkIconUrl = option.amount.display?.networkIconUrl,
                    tokenIconSize = 40.dp,
                    networkIconSize = 16.dp,
                    networkIconBorderWidth = 2.dp,
                    networkIconBorderColor = networkBadgeStrokeColor,
                    useExternalNetworkBorder = true
                )
                Spacer(modifier = Modifier.width(WCTheme.spacing.spacing3))
            }

            // Token amount
            val display = option.amount.display
            val tokenAmount = formatTokenAmount(
                value = option.amount.value,
                decimals = display?.decimals ?: 18,
                symbol = display?.assetSymbol ?: "Token"
            )

            Text(
                text = tokenAmount,
                style = WCTheme.typography.bodyLgMedium.copy(color = WCTheme.colors.textPrimary)
            )
        }

        // "Info required" badge if option has collectData
        if (option.collectData != null) {
            val pillBg = if (isSelected) WCTheme.colors.bgAccentPrimary.copy(alpha = 0.9f) else WCTheme.colors.foregroundTertiary
            val pillText = if (isSelected) Color.White else WCTheme.colors.textPrimary
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(WCTheme.borderRadius.radius2))
                    .background(pillBg)
                    .padding(horizontal = WCTheme.spacing.spacing2, vertical = 6.dp)
            ) {
                Text(
                    text = "Info required",
                    style = WCTheme.typography.bodyMdMedium.copy(color = pillText)
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
    networkIconSize: Dp,
    networkIconBorderWidth: Dp = 1.dp,
    networkIconBorderColor: Color = Color.White,
    useExternalNetworkBorder: Boolean = false
) {
    Box(modifier = Modifier.size(tokenIconSize)) {
        AsyncImage(
            model = tokenIconUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
        networkIconUrl?.let { networkUrl ->
            if (useExternalNetworkBorder && networkIconBorderWidth > 0.dp) {
                val badgeSize = networkIconSize + (networkIconBorderWidth * 2)
                val badgeOffset = tokenIconSize - badgeSize
                Box(
                    modifier = Modifier
                        .size(badgeSize)
                        .offset(
                            x = badgeOffset,
                            y = badgeOffset
                        )
                        .clip(CircleShape)
                        .background(networkIconBorderColor)
                        .padding(networkIconBorderWidth)
                ) {
                    AsyncImage(
                        model = networkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            } else {
                AsyncImage(
                    model = networkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(networkIconSize)
                        .offset(
                            x = tokenIconSize - networkIconSize,
                            y = tokenIconSize - networkIconSize
                        )
                        .clip(CircleShape)
                        .border(networkIconBorderWidth, networkIconBorderColor, CircleShape)
                )
            }
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
                style = WCTheme.typography.h4Regular.copy(color = Color.White)
            )
        }
    }
}

@Composable
private fun PaymentTitle(paymentInfo: Wallet.Model.PaymentInfo?) {
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
        style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
        textAlign = TextAlign.Center
    )
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
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Close button at top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            ModalCloseButton(onClick = onClose)
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        // Merchant icon and payment title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantIcon(paymentInfo = paymentInfo, size = 64.dp)
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
            PaymentTitle(paymentInfo = paymentInfo)
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        // "Pay with" row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.foregroundPrimary)
                .padding(horizontal = WCTheme.spacing.spacing4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pay with",
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textTertiary)
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
                    style = WCTheme.typography.bodyLgMedium.copy(color = WCTheme.colors.textPrimary)
                )

                Spacer(modifier = Modifier.width(WCTheme.spacing.spacing2))

                display?.iconUrl?.let { iconUrl ->
                    TokenIconWithNetwork(
                        tokenIconUrl = iconUrl,
                        networkIconUrl = display.networkIconUrl,
                        tokenIconSize = 32.dp,
                        networkIconSize = 16.dp,
                        networkIconBorderWidth = 2.dp,
                        networkIconBorderColor = WCTheme.colors.foregroundPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        // Confirm button
        val buttonAmount = paymentInfo?.let {
            formatDisplayAmount(
                value = it.amount.value,
                decimals = it.amount.display?.decimals ?: 2,
                symbol = it.amount.display?.assetSymbol ?: it.amount.unit
            )
        } ?: ""

        PrimaryActionButton(
            text = "Pay $buttonAmount",
            onClick = onConfirm
        )
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
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: back arrow (left) + X close (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalIconButton(
                iconRes = R.drawable.ic_arrow_left,
                contentDescription = "Back",
                onClick = onBack,
                showBorder = false
            )

            ModalCloseButton(onClick = onClose)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Why we need your information?",
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = "For regulatory compliance, we collect basic information on your first payment: full name, date of birth, and place of birth.",
            style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textTertiary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing3))

        Text(
            text = "This information is tied to your wallet address and this specific network. If you use the same wallet on this network again, you won't need to provide it again.",
            style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textTertiary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryActionButton(
            text = "Got it!",
            onClick = onBack
        )
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
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WalletConnectLoader(size = 120.dp)

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = "Confirming your payment...",
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing8))

        // Green checkmark circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WCTheme.colors.iconSuccess),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_check),
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

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
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing8))

        PrimaryActionButton(
            text = "Got it!",
            onClick = onDone
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    errorType: PaymentErrorType,
    onClose: () -> Unit,
    onScanNewQrCode: () -> Unit = {}
) {
    val title = when (errorType) {
        PaymentErrorType.INSUFFICIENT_FUNDS -> "Not enough funds"
        PaymentErrorType.EXPIRED -> "Your payment has expired"
        PaymentErrorType.CANCELLED -> "This payment was cancelled"
        PaymentErrorType.NOT_FOUND -> "Payment not found"
        PaymentErrorType.GENERIC -> "Transaction failed"
    }

    val subtitle = when (errorType) {
        PaymentErrorType.INSUFFICIENT_FUNDS -> "This wallet doesn't have enough funds on the supported networks to complete the payment."
        PaymentErrorType.EXPIRED -> "Please ask the merchant to generate a new payment and try again."
        PaymentErrorType.CANCELLED -> "Please ask the merchant to generate a new payment and try again."
        PaymentErrorType.NOT_FOUND -> "This payment link is no longer valid."
        PaymentErrorType.GENERIC -> message.ifBlank { null }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button (top-right)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            ModalCloseButton(onClick = onClose)
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing7))

        // Warning icon
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_warning_circle),
            contentDescription = "Warning",
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = title,
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))
            Text(
                text = subtitle,
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textTertiary),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing8))

        when (errorType) {
            PaymentErrorType.EXPIRED, PaymentErrorType.CANCELLED, PaymentErrorType.NOT_FOUND -> {
                PrimaryActionButton(
                    text = "Scan new QR code",
                    onClick = onScanNewQrCode
                )
            }
            PaymentErrorType.INSUFFICIENT_FUNDS -> {
                PrimaryActionButton(
                    text = "Got it!",
                    onClick = onClose
                )
            }
            else -> {
                PrimaryActionButton(
                    text = "Close",
                    onClick = onClose
                )
            }
        }
    }
}

// ==================== Shared Modal Components ====================

@Composable
private fun ModalCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius3))
            .border(
                width = 1.dp,
                color = WCTheme.colors.borderSecondary,
                shape = RoundedCornerShape(WCTheme.borderRadius.radius3)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x_close),
            contentDescription = "Close",
            modifier = Modifier.size(20.dp),
            tint = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun ModalIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    showBorder: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius3))
            .then(
                if (showBorder) Modifier.border(
                    width = 1.dp,
                    color = WCTheme.colors.borderSecondary,
                    shape = RoundedCornerShape(WCTheme.borderRadius.radius3)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(
                if (enabled) WCTheme.colors.bgAccentPrimary
                else WCTheme.colors.bgAccentPrimary.copy(alpha = 0.6f)
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = WCTheme.typography.bodyLgRegular.copy(color = Color.White)
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
            .statusBarsPadding()
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
                        style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textError),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
                    PrimaryActionButton(
                        text = "Retry",
                        onClick = { loadError = null }
                    )
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
                    .background(WCTheme.colors.bgPrimary),
                contentAlignment = Alignment.Center
            ) {
                WalletConnectLoader(size = 120.dp)
            }
        }

        // Floating close button overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(WCTheme.spacing.spacing4)
        ) {
            ModalCloseButton(onClick = onClose)
        }
    }
}
