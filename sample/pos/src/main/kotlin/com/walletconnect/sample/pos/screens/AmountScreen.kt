package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.walletconnect.sample.pos.POSViewModel

// Brand color
private val BrandColor = Color(0xFF0988F0)

@Composable
fun AmountScreen(
    viewModel: POSViewModel,
    modifier: Modifier = Modifier
) {
    var amountDisplay by rememberSaveable { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    // Convert display amount (dollars) to minor units (cents)
    fun getAmountInCents(): String {
        val dollars = amountDisplay.toDoubleOrNull() ?: 0.0
        return (dollars * 100).toLong().toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        // Header
        PosHeader()

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "Enter Amount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Enter the payment amount in USD",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Amount input card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF5F5F5),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = amountDisplay,
                        onValueChange = { new ->
                            // Filter to allow only numbers and one decimal point
                            val filtered = new
                                .replace(Regex("[^0-9.]"), "")
                                .let { s ->
                                    val firstDot = s.indexOf('.')
                                    if (firstDot == -1) s else
                                        s.substring(0, firstDot + 1) + s.substring(firstDot + 1).replace(".", "")
                                }
                            // Limit decimal places to 2
                            val parts = filtered.split(".")
                            amountDisplay = if (parts.size == 2 && parts[1].length > 2) {
                                "${parts[0]}.${parts[1].take(2)}"
                            } else {
                                filtered
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        ),
                        leadingIcon = {
                            Text(
                                "$",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        },
                        placeholder = {
                            Text(
                                "0.00",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFBBBBBB)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "USD",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Start Payment button
            Button(
                onClick = {
                    val amountInCents = getAmountInCents()
                    if (amountInCents.toLongOrNull() ?: 0L > 0) {
                        viewModel.createPayment(amountInCents, "USD")
                    }
                },
                enabled = amountDisplay.isNotBlank() &&
                        (amountDisplay.toDoubleOrNull() ?: 0.0) > 0 &&
                        !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandColor,
                    disabledContainerColor = BrandColor.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Start Payment",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Bottom padding for navigation bars
        Spacer(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(16.dp)
        )
    }
}
