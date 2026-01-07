package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.reown.sample.wallet.ui.common.SemiTransparentDialog
import com.reown.sample.wallet.ui.routes.Route
import com.walletconnect.pay.Pay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentRoute(
    navController: NavHostController,
    paymentLink: String,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(paymentLink) {
        viewModel.loadPaymentOptions(paymentLink)
    }

    SemiTransparentDialog {
        when (val state = uiState) {
            is PaymentUiState.Loading -> {
                LoadingContent()
            }
            is PaymentUiState.Intro -> {
                IntroContent(
                    paymentInfo = state.paymentInfo,
                    hasInfoCapture = state.hasInfoCapture,
                    estimatedTime = state.estimatedTime,
                    onStart = { viewModel.proceedFromIntro() },
                    onClose = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                    }
                )
            }
            is PaymentUiState.Options -> {
                PaymentOptionsContent(
                    paymentInfo = state.paymentInfo,
                    options = state.options,
                    onOptionSelected = { optionId ->
                        viewModel.processPayment(optionId)
                    },
                    onClose = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
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
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                    }
                )
            }
            is PaymentUiState.Processing -> {
                ProcessingContent(
                    message = state.message,
                    onClose = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                    }
                )
            }
            is PaymentUiState.Success -> {
                SuccessContent(
                    paymentInfo = state.paymentInfo,
                    onDone = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                        Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                    },
                    onClose = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                    }
                )
            }
            is PaymentUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = {
                        viewModel.loadPaymentOptions(paymentLink)
                    },
                    onClose = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                    }
                )
            }
        }
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
private fun IntroContent(
    paymentInfo: Pay.PaymentInfo?,
    hasInfoCapture: Boolean,
    estimatedTime: String,
    onStart: () -> Unit,
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Merchant icon
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            paymentInfo?.merchant?.iconUrl?.let { iconUrl ->
                AsyncImage(
                    model = iconUrl,
                    contentDescription = "Merchant icon",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                )
            } ?: run {
                // Placeholder icon if no merchant icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
            Text(
                        text = paymentInfo?.merchant?.name?.take(1)?.uppercase() ?: "P",
                style = TextStyle(
                            fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Payment title with verified badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val merchantName = paymentInfo?.merchant?.name ?: "Merchant"
                val formattedAmount = if (paymentInfo != null) {
                    formatAmount(
                        value = paymentInfo.amount.value,
                        decimals = paymentInfo.amount.display?.decimals ?: 2,
                        symbol = paymentInfo.amount.display?.assetSymbol ?: paymentInfo.amount.unit
                    )
                } else {
                    "Payment"
                }
                
                Text(
                    text = "Pay $formattedAmount to $merchantName",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Verified badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3396FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Steps timeline
        IntroStepsTimeline(
            hasInfoCapture = hasInfoCapture,
            estimatedTime = estimatedTime
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Let's start button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Let's start",
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
private fun IntroStepsTimeline(
    hasInfoCapture: Boolean,
    estimatedTime: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Step 1: Provide information (only if info capture is required)
        if (hasInfoCapture) {
            IntroStepItem(
                stepNumber = 1,
                totalSteps = 2,
                title = "Provide information",
                description = "A quick one-time check required for regulated payments.",
                estimatedTime = estimatedTime,
                isFirst = true,
                isLast = false
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Step 2: Confirm payment
            IntroStepItem(
                stepNumber = 2,
                totalSteps = 2,
                title = "Confirm payment",
                description = "Review the details and approve the payment.",
                estimatedTime = null,
                isFirst = false,
                isLast = true
            )
        } else {
            // Only show payment confirmation step
            IntroStepItem(
                stepNumber = 1,
                totalSteps = 1,
                title = "Confirm payment",
                description = "Review the details and approve the payment.",
                estimatedTime = null,
                isFirst = true,
                isLast = true
            )
        }
    }
}

@Composable
private fun IntroStepItem(
    stepNumber: Int,
    totalSteps: Int,
    title: String,
    description: String,
    estimatedTime: String?,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            // Connector line (top part)
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(Color(0xFFE0E0E0))
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Circle indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0))
            )
            
            // Connector line (bottom part)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Step content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                )
                
                // Estimated time badge
                estimatedTime?.let { time ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = time,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E),
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
private fun PaymentOptionsContent(
    paymentInfo: Pay.PaymentInfo?,
    options: List<Pay.PaymentOption>,
    onOptionSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedOptionId by remember { mutableStateOf<String?>(options.firstOrNull()?.id) }
    var isOptionsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        // Header with progress and close
        PaymentOptionsHeader(
            onClose = onClose
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Merchant icon and payment title
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            paymentInfo?.merchant?.iconUrl?.let { iconUrl ->
            AsyncImage(
                model = iconUrl,
                contentDescription = "Merchant icon",
                modifier = Modifier
                    .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = paymentInfo?.merchant?.name?.take(1)?.uppercase() ?: "P",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Payment title with verified badge
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Verified badge
                Box(
                    modifier = Modifier
                        .size(18.dp)
                    .clip(CircleShape)
                        .background(Color(0xFF3396FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Amount row
        paymentInfo?.let { info ->
            val displayAmount = formatDisplayAmount(
                value = info.amount.value,
                decimals = info.amount.display?.decimals ?: 2,
                symbol = info.amount.display?.assetSymbol ?: info.amount.unit
            )
            
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
                    text = "Amount",
            style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )
                Text(
                    text = displayAmount,
                    style = TextStyle(
                        fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                        color = Color.Black
            )
        )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pay with dropdown
        PayWithDropdown(
            options = options,
            selectedOptionId = selectedOptionId,
            isExpanded = isOptionsExpanded,
            onExpandToggle = { isOptionsExpanded = !isOptionsExpanded },
            onOptionSelected = { optionId ->
                selectedOptionId = optionId
                isOptionsExpanded = false
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pay button
        val buttonAmount = paymentInfo?.let {
            formatDisplayAmount(
                value = it.amount.value,
                decimals = it.amount.display?.decimals ?: 2,
                symbol = it.amount.display?.assetSymbol ?: it.amount.unit
            )
        } ?: "Pay"
        
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
private fun PaymentOptionsHeader(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Empty spacer for balance
        Spacer(modifier = Modifier.size(32.dp))
        
        // Progress indicator (2 steps, both completed)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step 1 - completed
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3396FF))
            )
            // Step 2 - current
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3396FF))
            )
        }
        
        // Close button
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
}

@Composable
private fun PayWithDropdown(
    options: List<Pay.PaymentOption>,
    selectedOptionId: String?,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        // Header row with "Pay with" label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() }
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
            
            val selectedOption = options.find { it.id == selectedOptionId }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
            if (selectedOption != null) {
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
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                    )
                }
            } else {
                Text(
                        text = "Select",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                )
            }
                
                Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(20.dp)
            )
            }
        }

        // Expandable options list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
            ) {
                options.forEach { option ->
                    PaymentOptionItem(
                        option = option,
                        isSelected = selectedOptionId == option.id,
                        onClick = { onOptionSelected(option.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionItem(
    option: Pay.PaymentOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Asset icon
                option.amount.display?.iconUrl?.let { iconUrl ->
                    AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                        .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

            // Token amount and network
            val display = option.amount.display
            val tokenAmount = formatTokenAmount(
                value = option.amount.value,
                decimals = display?.decimals ?: 18,
                symbol = display?.assetSymbol ?: "Token"
            )
            val networkName = display?.networkName ?: ""

                Column {
                        Text(
                    text = tokenAmount,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                )
                if (networkName.isNotEmpty()) {
                    Text(
                        text = "on $networkName",
                            style = TextStyle(
                                fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                            )
                        )
                    }
                }
            }

        // Radio button indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                .border(
                    width = if (isSelected) 0.dp else 2.dp,
                    color = if (isSelected) Color.Transparent else Color(0xFFE0E0E0),
                    shape = CircleShape
                )
                .background(if (isSelected) Color(0xFF3396FF) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Format amount with proper decimals and symbol.
 */
private fun formatAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val divisor = BigDecimal.TEN.pow(decimals)
        val formattedValue = rawValue.divide(divisor, decimals.coerceAtMost(2), RoundingMode.HALF_UP)
        "$formattedValue $symbol"
    } catch (e: Exception) {
        "$value $symbol"
    }
}

/**
 * Format display amount with $ prefix (for USD amounts).
 */
private fun formatDisplayAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val divisor = BigDecimal.TEN.pow(decimals)
        val formattedValue = rawValue.divide(divisor, 2, RoundingMode.HALF_UP)
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formatted = numberFormat.format(formattedValue)
        "$$formatted"
    } catch (e: Exception) {
        "$$value"
    }
}

/**
 * Format token amount with symbol.
 */
private fun formatTokenAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val divisor = BigDecimal.TEN.pow(decimals)
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
    paymentInfo: Pay.PaymentInfo?,
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
    currentField: Pay.CollectDataField,
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
            Pay.CollectDataFieldType.TEXT -> {
                TextFieldInput(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    placeholder = currentField.name
                )
            }
            Pay.CollectDataFieldType.DATE -> {
                DatePickerInput(
                    value = inputValue,
                    onValueChange = { inputValue = it }
                )
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
            items = months,
            selectedItem = months.getOrNull((selectedMonth.toIntOrNull() ?: 1) - 1) ?: "January",
            onItemSelected = { month ->
                selectedMonth = (months.indexOf(month) + 1).toString().padStart(2, '0')
            },
            modifier = Modifier.weight(1.5f)
        )
        
        // Day picker
        DateWheelPicker(
            items = days,
            selectedItem = selectedDay.takeIf { it in days } ?: "01",
            onItemSelected = { selectedDay = it },
            modifier = Modifier.weight(0.8f)
        )
        
        // Year picker
        DateWheelPicker(
            items = years,
            selectedItem = selectedYear.takeIf { it in years } ?: "2000",
            onItemSelected = { selectedYear = it },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateWheelPicker(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show a few items around the selected one
        val visibleRange = 2
        val startIndex = (selectedIndex - visibleRange).coerceAtLeast(0)
        val endIndex = (selectedIndex + visibleRange).coerceAtMost(items.size - 1)
        
        for (i in startIndex..endIndex) {
            val item = items[i]
            val isSelected = i == selectedIndex
            Text(
                text = item,
                style = TextStyle(
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.Black else Color.Gray
                ),
                modifier = Modifier
                    .clickable { onItemSelected(item) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

