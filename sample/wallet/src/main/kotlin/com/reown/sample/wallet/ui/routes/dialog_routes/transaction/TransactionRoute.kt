package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.ui.common.SemiTransparentDialog
import com.reown.sample.wallet.ui.routes.Route

@Composable
fun TransactionRoute(navController: NavHostController, viewModel: TransactionViewModel = viewModel()) {
    var selectedCoin by remember { mutableStateOf(StableCoin.USDC) }
    var amountToSend by remember { mutableStateOf("1") }
    var to by remember { mutableStateOf("") }
    var selectedChain by remember { mutableStateOf(Chain.BASE) }
    val uiState by viewModel.uiState.collectAsState()

    SemiTransparentDialog {
        Spacer(modifier = Modifier.height(16.dp))
        AddressCard()
        Spacer(modifier = Modifier.height(16.dp))
        BalanceCard(viewModel, selectedChain)
        Spacer(modifier = Modifier.height(16.dp))
        TransactionCard(
            selectedCoin = selectedCoin,
            onCoinSelected = { selectedCoin = it },
            amountToSend = amountToSend,
            onAmountChanged = { amountToSend = it },
            recipient = to,
            onRecipientChanged = { to = it },
            selectedChain,
            onNetworkSelected = { selectedChain = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState is UIState.Error) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = (uiState as UIState.Error).message,
                color = Color.Red,
                fontSize = 16.sp,
            )
        }
        if (uiState is UIState.NotRequired) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "Chain Abstraction is not required for this transaction.",
                color = Color.Red,
                fontSize = 16.sp
            )
        }
        SendButton(uiState = uiState) {
            viewModel.sendTransaction(selectedChain, selectedCoin, amountToSend, to, EthAccountDelegate.address)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AddressCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "My Address",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = EthAccountDelegate.address,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BalanceCard(viewModel: TransactionViewModel, selectedChain: Chain) {
    val balanceState by viewModel.balanceState.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Balance",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "USD Coin",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Text(
                    text = "${balanceState[Pair(selectedChain, StableCoin.USDC)] ?: "-.--"} USDC",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tether USD",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Text(
                    text = "${balanceState[Pair(selectedChain, StableCoin.USDT)] ?: "-.--"} USDT",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    selectedCoin: StableCoin,
    onCoinSelected: (StableCoin) -> Unit,
    amountToSend: String,
    onAmountChanged: (String) -> Unit,
    recipient: String,
    onRecipientChanged: (String) -> Unit,
    selectedNetwork: Chain,
    onNetworkSelected: (Chain) -> Unit
) {
    val savedRecipients = listOf("0x228311b83dAF3FC9a0D0a46c0B329942fc8Cb2eD")
    var isRecipientDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Transaction",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Coin",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoinSelectionButton(
                    text = StableCoin.USDC.name,
                    selected = selectedCoin == StableCoin.USDC,
                    onClick = { onCoinSelected(StableCoin.USDC) },
                    modifier = Modifier.weight(1f)
                )
                CoinSelectionButton(
                    text = StableCoin.USDT.name,
                    selected = selectedCoin == StableCoin.USDT,
                    onClick = { onCoinSelected(StableCoin.USDT) },
                    modifier = Modifier.weight(1f),
                    isEnabled = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recipient",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Box {
                TextField(
                    value = recipient,
                    placeholder = {
                        Text(
                            text = "address...",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    },
                    onValueChange = onRecipientChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                isRecipientDropdownExpanded = true
                            }
                        },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Black,
                        focusedContainerColor = Color.Black,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )

                DropdownMenu(
                    expanded = isRecipientDropdownExpanded,
                    onDismissRequest = { isRecipientDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray)
                ) {
                    savedRecipients.forEach { savedRecipient ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = savedRecipient,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            },
                            onClick = {
                                onRecipientChanged(savedRecipient)
                                isRecipientDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Amount to send",
                color = Color.Gray,
                fontSize = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextField(
                    value = amountToSend,
                    onValueChange = onAmountChanged,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Black,
                        focusedContainerColor = Color.Black,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
                Box {
                    var expanded by remember { mutableStateOf(false) }

                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .padding(start = 8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = selectedNetwork.name,
                            color = Color(0xFF2196F3),
                            fontSize = 16.sp
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(Color.DarkGray),
                        properties = PopupProperties(focusable = true)
                    ) {
                        listOf(Chain.BASE, Chain.OPTIMISM, Chain.ARBITRUM).forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.name,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    onNetworkSelected(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoinSelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color.Gray else Color.DarkGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray
        )
    }
}

@Composable
fun SendButton(uiState: UIState, onSend: () -> Unit = {}) {
    Button(
        onClick = { onSend() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3),
                            Color(0xFF9C27B0)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is UIState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }

                else -> {
                    Text(
                        text = "Send",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}