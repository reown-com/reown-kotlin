package com.reown.sample.pos.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorScreen(
    message: String,
    onReturnToStart: () -> Unit,
) {
    val errorRed = Color(0xFFD32F2F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = errorRed,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Error message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = errorRed,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(32.dp))

        // Try Again button pinned bottom
        Button(
            onClick = onReturnToStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = errorRed)
        ) {
            Text(
                text = "Try Again",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}