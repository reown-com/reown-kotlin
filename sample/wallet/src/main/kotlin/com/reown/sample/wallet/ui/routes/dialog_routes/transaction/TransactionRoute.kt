package com.reown.sample.wallet.ui.routes.dialog_routes.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavHostController
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.ui.common.SemiTransparentDialog

@Composable
fun TransactionRoute(navController: NavHostController) {
    var selectedCoin by remember { mutableStateOf("USDC") }
    var amountToSend by remember { mutableStateOf("1") }
    var address by remember { mutableStateOf("") }

    SemiTransparentDialog {
        Spacer(modifier = Modifier.height(16.dp))
        AddressCard()
        Spacer(modifier = Modifier.height(16.dp))
        BalanceCard()
        Spacer(modifier = Modifier.height(16.dp))
        TransactionCard(
            selectedCoin = selectedCoin,
            onCoinSelected = { selectedCoin = it },
            amountToSend = amountToSend,
            onAmountChanged = { amountToSend = it },
            recipient = address,
            onRecipientChanged = { address = it }
        )
        SendButton()
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
fun BalanceCard() {
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
                    text = "28,999864 USDC",
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
                    text = "0 USDT",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    selectedCoin: String,
    onCoinSelected: (String) -> Unit,
    amountToSend: String,
    onAmountChanged: (String) -> Unit,
    recipient: String,
    onRecipientChanged: (String) -> Unit
) {
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
                    text = "USDC",
                    selected = selectedCoin == "USDC",
                    onClick = { onCoinSelected("USDC") },
                    modifier = Modifier.weight(1f)
                )
                CoinSelectionButton(
                    text = "USDT",
                    selected = selectedCoin == "USDT",
                    onClick = { onCoinSelected("USDT") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recipient",
                color = Color.Gray,
                fontSize = 16.sp
            )
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
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Black,
                    focusedContainerColor = Color.Black,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )

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
                    var selectedOption by remember { mutableStateOf("Base") }

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
                            text = selectedOption,
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
                        listOf("Base", "OP", "Arb").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedOption = option
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
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
fun SendButton() {
    Button(
        onClick = { /* Handle send click */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3),
                            Color(0xFF9C27B0)
                        )
                    )
                )
        ) {
            Text(
                text = "Send",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}