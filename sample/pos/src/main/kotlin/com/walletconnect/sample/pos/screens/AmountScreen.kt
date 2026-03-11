package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.components.BigAmountDisplay
import com.walletconnect.sample.pos.components.NumericKeyboard
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.model.formatAmountWithSymbol

@Composable
fun AmountScreen(
    viewModel: POSViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountDisplay by rememberSaveable { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    fun getAmountInCents(): String {
        val dollars = amountDisplay.toDoubleOrNull() ?: 0.0
        return (dollars * 100).toLong().toString()
    }

    val isValid = amountDisplay.isNotBlank() && (amountDisplay.toDoubleOrNull() ?: 0.0) > 0 && !isLoading
    val chargeText = if (isValid) {
        val dollars = amountDisplay.toDoubleOrNull() ?: 0.0
        "Charge ${formatAmountWithSymbol(String.format("%.2f", dollars), currency)}"
    } else {
        "Enter amount"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PosHeader(onBack = onBack)

        // Amount display area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = WCTheme.spacing.spacing5),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            BigAmountDisplay(
                currencySymbol = currency.symbol,
                amount = amountDisplay,
                symbolPosition = currency.symbolPosition
            )

            Spacer(Modifier.weight(1f))
        }

        // Numeric keyboard
        NumericKeyboard(
            modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5),
            onDigit = { digit ->
                val newAmount = amountDisplay + digit
                // Limit decimal places to 2
                val parts = newAmount.split(".")
                if (parts.size == 2 && parts[1].length > 2) return@NumericKeyboard
                // Limit total length
                if (newAmount.replace(".", "").length > 8) return@NumericKeyboard
                amountDisplay = newAmount
            },
            onDecimal = {
                if (!amountDisplay.contains(".")) {
                    amountDisplay = if (amountDisplay.isEmpty()) "0." else "$amountDisplay."
                }
            },
            onBackspace = {
                if (amountDisplay.isNotEmpty()) {
                    amountDisplay = amountDisplay.dropLast(1)
                }
            }
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing4))

        // Charge button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WCTheme.spacing.spacing5)
                .height(WCTheme.spacing.spacing12)
                .alpha(if (isValid) 1f else 0.6f)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.bgAccentPrimary)
                .then(
                    if (isValid) Modifier.clickable {
                        viewModel.createPayment(getAmountInCents())
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = WCTheme.colors.textInvert,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = chargeText,
                    style = WCTheme.typography.bodyLgMedium,
                    color = WCTheme.colors.textInvert
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing4))
    }
}
