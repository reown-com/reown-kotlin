package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.R

// Brand color
private val BrandColor = Color(0xFF0988F0)

@Composable
fun StartPaymentScreen(
    viewModel: POSViewModel,
    merchantName: String = "Sample POS Terminal",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header - simplified with just NRF text
        PosHeader()

        // Content - centered logos and tagline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logos row: WCPay logo + X + Ingenico logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // WCPay Logo (using vector drawable)
                Image(
                    painter = painterResource(R.drawable.ic_wcpay_logo),
                    contentDescription = "WCPay",
                    modifier = Modifier.height(28.dp),
                    contentScale = ContentScale.FillHeight
                )
                
                Spacer(Modifier.width(20.dp))
                
                // X separator
                Text(
                    "x",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
                
                Spacer(Modifier.width(20.dp))
                
                // Ingenico Logo
                IngenicoLogo()
            }

            Spacer(Modifier.height(40.dp))

            // Tagline
            Text(
                "Enable crypto payments from any wallet, any asset, anywhere",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
        
        // Bottom section with button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // New Payment button
            Button(
                onClick = { viewModel.navigateToAmountScreen() },
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

            Spacer(Modifier.height(12.dp))

            // Transaction History button
            OutlinedButton(
                onClick = { viewModel.navigateToTransactionHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandColor),
                border = BorderStroke(1.dp, BrandColor)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PosHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "NRF'26 NYC",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IngenicoLogo() {
    Image(
        painter = painterResource(R.drawable.ic_ingenico_logo),
        contentDescription = "Ingenico",
        modifier = Modifier.height(28.dp),
        contentScale = ContentScale.FillHeight
    )
}
