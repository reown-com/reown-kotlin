package com.reown.sample.pos.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reown.sample.pos.POSViewModel

@Composable
fun StartPaymentScreen(viewModel: POSViewModel) {
    Text("Start Screen", modifier = Modifier.padding(24.dp))
}