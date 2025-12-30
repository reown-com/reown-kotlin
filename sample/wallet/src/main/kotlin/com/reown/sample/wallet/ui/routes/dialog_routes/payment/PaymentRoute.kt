package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.Text
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
            is PaymentUiState.Options -> {
                PaymentOptionsContent(
                    paymentInfo = state.paymentInfo,
                    options = state.options,
                    onOptionSelected = { optionId ->
                        viewModel.processPayment(optionId)
                    }
                )
            }
            is PaymentUiState.Processing -> {
                ProcessingContent(message = state.message)
            }
            is PaymentUiState.Success -> {
                SuccessContent(
                    message = state.message,
                    onDone = {
                        viewModel.cancel()
                        navController.popBackStack(Route.Connections.path, inclusive = false)
                        Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            is PaymentUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = {
                        viewModel.loadPaymentOptions(paymentLink)
                    },
                    onCancel = {
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
private fun PaymentOptionsContent(
    paymentInfo: Pay.PaymentInfo?,
    options: List<Pay.PaymentOption>,
    onOptionSelected: (String) -> Unit
) {
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var isOptionsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Payment Info Header
        if (paymentInfo != null) {
            PaymentInfoHeader(paymentInfo = paymentInfo)
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Fallback header when no payment info
            Text(
                text = "WalletConnect Pay",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Expandable Payment Options Section
        ExpandablePaymentOptions(
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
        Button(
            onClick = { selectedOptionId?.let { onOptionSelected(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedOptionId != null,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF),
                disabledBackgroundColor = Color(0xFF1A4A7A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Pay", color = Color.White)
        }
    }
}

@Composable
private fun PaymentInfoHeader(paymentInfo: Pay.PaymentInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Merchant Icon
        paymentInfo.merchant.iconUrl?.let { iconUrl ->
            AsyncImage(
                model = iconUrl,
                contentDescription = "Merchant icon",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3A))
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Merchant Name
        Text(
            text = paymentInfo.merchant.name,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount
        val formattedAmount = formatAmount(
            value = paymentInfo.amount.value,
            decimals = paymentInfo.amount.display?.decimals ?: 2,
            symbol = paymentInfo.amount.display?.assetSymbol ?: paymentInfo.amount.unit
        )
        Text(
            text = formattedAmount,
            style = TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Expiration
        val expiresText = formatExpiration(paymentInfo.expiresAt)
        Text(
            text = expiresText,
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.Gray
            )
        )
    }
}

@Composable
private fun ExpandablePaymentOptions(
    options: List<Pay.PaymentOption>,
    selectedOptionId: String?,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A))
    ) {
        // Header row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedOption = options.find { it.id == selectedOptionId }
            if (selectedOption != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    selectedOption.amount.display?.iconUrl?.let { iconUrl ->
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "${selectedOption.amount.display?.assetName ?: "Token"} on ${selectedOption.amount.display?.networkName ?: "Network"}",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            } else {
                Text(
                    text = "Select payment method",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Gray
            )
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
                    PaymentOptionCard(
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
private fun PaymentOptionCard(
    option: Pay.PaymentOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF3396FF) else Color(0xFF3A3A3A)
    val backgroundColor = if (isSelected) Color(0xFF1A2A3A) else Color(0xFF2A2A2A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    option.amount.display?.let { display ->
                        Text(
                            text = "${display.assetName} on ${display.networkName ?: "Unknown"}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3396FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingContent(message: String) {
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
            text = message,
            style = TextStyle(fontSize = 16.sp, color = Color.White),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent(
    message: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Payment Successful",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = TextStyle(fontSize = 14.sp, color = Color.Gray),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3396FF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", color = Color.White)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Error",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Payment Failed",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = TextStyle(fontSize = 14.sp, color = Color.Gray),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = Color.White)
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3396FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}

