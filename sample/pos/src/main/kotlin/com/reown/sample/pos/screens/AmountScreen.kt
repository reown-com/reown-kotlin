package com.reown.sample.pos.screens

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
import com.reown.sample.pos.POSViewModel

@Composable
fun AmountScreen(
    viewModel: POSViewModel,
    modifier: Modifier = Modifier
) {
    val brandGreen = Color(0xFF0A8F5B)
    var amount by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // --- Green header like screen #1 ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brandGreen)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DTC Pay",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Crypto Payment Terminal",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.95f)
            )
        }

        // --- Main content ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "Enter Payment Amount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Step 2: Enter the amount to charge",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Amount "display" card with a big centered TextField
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big amount input. System keyboard opens automatically on tap.
                    TextField(
                        value = amount,
                        onValueChange = { new ->
                            // Simple numeric filter (digits + at most one '.')
                            val filtered = new
                                .replace(Regex("[^0-9.]"), "")
                                .let { s ->
                                    val firstDot = s.indexOf('.')
                                    if (firstDot == -1) s else
                                        s.substring(0, firstDot + 1) + s.substring(firstDot + 1).replace(".", "")
                                }
                            amount = filtered
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        ),
                        leadingIcon = {
                            Text(
                                "$",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        },
                        placeholder = { Text("0", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Manual token selection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // CTA
            Button(
                onClick = { viewModel.createPaymentIntent("eip155:137", "Polygon", amount.trim()) },
                enabled = amount.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandGreen)
            ) {
                Text(
                    "Start Payment",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // --- Footer strip ---
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Powered by DTC Pay",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
