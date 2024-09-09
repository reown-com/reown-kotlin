package com.reown.sample.modal.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.appkit.ui.components.button.AccountButton
import com.reown.appkit.ui.components.button.AccountButtonType
import com.reown.appkit.ui.components.button.ConnectButton
import com.reown.appkit.ui.components.button.ConnectButtonSize
import com.reown.appkit.ui.components.button.NetworkButton
import com.reown.appkit.ui.components.button.Web3Button
import com.reown.appkit.ui.components.button.rememberAppKitState

@Composable
fun HomeScreen(navController: NavController) {
    val web3ModalState = rememberAppKitState(navController = navController)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConnectButton(state = web3ModalState, buttonSize = ConnectButtonSize.NORMAL)
        Spacer(modifier = Modifier.height(20.dp))
        ConnectButton(state = web3ModalState, buttonSize = ConnectButtonSize.SMALL)
        Spacer(modifier = Modifier.height(20.dp))
        AccountButton(web3ModalState, AccountButtonType.NORMAL)
        Spacer(modifier = Modifier.height(20.dp))
        AccountButton(web3ModalState, AccountButtonType.MIXED)
        Spacer(modifier = Modifier.height(20.dp))
        Web3Button(state = web3ModalState)
        Spacer(modifier = Modifier.height(20.dp))
        NetworkButton(state = web3ModalState)
    }
}
