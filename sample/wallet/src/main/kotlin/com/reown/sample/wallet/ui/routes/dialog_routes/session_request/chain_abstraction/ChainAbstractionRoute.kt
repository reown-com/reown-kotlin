package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.common.sendResponseDeepLink
import com.reown.sample.common.ui.theme.mismatch_color
import com.reown.sample.common.ui.theme.verified_color
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.getErrorMessage
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.common.Button
import com.reown.sample.wallet.ui.common.Buttons
import com.reown.sample.wallet.ui.common.Content
import com.reown.sample.wallet.ui.common.InnerContent
import com.reown.sample.wallet.ui.common.SemiTransparentDialog
import com.reown.sample.wallet.ui.common.blue.BlueLabelRow
import com.reown.sample.wallet.ui.common.blue.BlueLabelText
import com.reown.sample.wallet.ui.common.peer.Peer
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.common.peer.getColor
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request.SessionRequestUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi")
@Composable
fun ChainAbstractionRoute(navController: NavHostController, isError: Boolean, chainAbstractionViewModel: ChainAbstractionViewModel = viewModel()) {
    println("kobe: isError: $isError")
    //InitTransaction(from=0xc3d7420EA0d9102760c4DCf700245961FFc5Ec42, to=0xaf88d065e77c8cC2239327C5EDb3A432268e5831, value=0, gas=0, gasPrice=0, data=0xa9059cbb000000000000000000000000228311b83daf3fc9a0d0a46c0b329942fc8cb2ed000000000000000000000000000000000000000000000000000000000007a120, nonce=0, maxFeePerGas=0, maxPriorityFeePerGas=0, chainId=eip155:42161)
    //Available(fulfilmentId=2d26c36c-56ea-4fd0-86f6-b1d80e5b0bf0, checkIn=3000, transactions=[Transaction(from=0xc3d7420EA0d9102760c4DCf700245961FFc5Ec42, to=0x0b2C639c533813f4Aa9D7837CAf62653d097Ff85, value=0x00, gas=0xf9e82, gasPrice=0xf4472, data=0x095ea7b30000000000000000000000003a23f943181408eac424116af7b7790c94cb97a500000000000000000000000000000000000000000000000000000000000696af, nonce=0x16, maxFeePerGas=0, maxPriorityFeePerGas=0, chainId=eip155:10), Transaction(from=0xc3d7420EA0d9102760c4DCf700245961FFc5Ec42, to=0x3a23F943181408EAC424116Af7b7790c94Cb97a5, value=0x00, gas=0xf9e82, gasPrice=0xf4472, data=0x0000019e5da11e770000000000000000000000000b2c639c533813f4aa9d7837caf62653d097ff850000000000000000000000000000000000000000000000000000000000036d310000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000759e0000000000000000000000000000000000000000000000000000000000036465000000000000000000000000ce8cca271ebc0533920c83d39f417ed6a0abb7d000000000000000000000000000000000000000000000000000000000000001a000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000001f04aef628c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b3b000000000000000000000000000000000000000000000000000000000000a4b1000000000000000000000000c3d7420ea0d9102760c4dcf700245961ffc5ec4200000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000013000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000018000301001303000000000000000000000000000000030d4000000000000000000000000000000000000000000000000000000000000000000000000000000184ee8f0b860000000000000000000000000b2c639c533813f4aa9d7837caf62653d097ff85000000000000000000000000eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee000000000000000000000000000000000000000000000000000000000003297e0000000000000000000000000000000000000000000000000000000000001b3b00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000a8e449022e000000000000000000000000000000000000000000000000000000000003297e00000000000000000000000000000000000000000000000000003ba09fe48cae000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000012000000000000000000000001fb3cf6e48f1e7b10213e7b6d87d4c073c7fdb7b41caa3f800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000, nonce=0x17, maxFeePerGas=0, maxPriorityFeePerGas=0, chainId=eip155:10)], funding=[FundingMetadata(chainId=eip155:10, tokenContract=0x0b2C639c533813f4Aa9D7837CAf62653d097Ff85, symbol=USDC, amount=0x696af)])

    val sessionRequestUI = chainAbstractionViewModel.sessionRequestUI
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isConfirmLoading by remember { mutableStateOf(false) }
    var isCancelLoading by remember { mutableStateOf(false) }

    when (sessionRequestUI) {
        is SessionRequestUI.Content -> {
            val allowButtonColor = getColor(sessionRequestUI.peerContextUI)
            WCDelegate.currentId = sessionRequestUI.requestId

            SemiTransparentDialog {
                Spacer(modifier = Modifier.height(24.dp))
                Peer(peerUI = sessionRequestUI.peerUI, "Review transaction", sessionRequestUI.peerContextUI)
                Spacer(modifier = Modifier.height(16.dp))
                if (isError) {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(
                            text = getErrorMessage(),
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = mismatch_color
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Request(sessionRequestUI = sessionRequestUI, isError)
                Spacer(modifier = Modifier.height(16.dp))
                if (isError) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD6D6D6))
                            .height(46.dp),
                        text = "Back to App",
                        onClick = {
                            navController.popBackStack()
                        }
                    )

                } else {
                    Buttons(
                        allowButtonColor,
                        onConfirm = { confirmRequest(sessionRequestUI, navController, chainAbstractionViewModel, composableScope, context) { isConfirmLoading = it } },
                        onCancel = { cancelRequest(sessionRequestUI, navController, chainAbstractionViewModel, composableScope, context) { isCancelLoading = it } },
                        isLoadingConfirm = isConfirmLoading,
                        isLoadingCancel = isCancelLoading
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SessionRequestUI.Initial -> {
            SemiTransparentDialog {
                Spacer(modifier = Modifier.height(24.dp))
                Peer(peerUI = PeerUI.Empty, null)
                Spacer(modifier = Modifier.height(200.dp))
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 8.dp, modifier = Modifier.size(100.dp), color = Color(0xFFB8F53D))
                }
                Spacer(modifier = Modifier.height(200.dp))
                Buttons(
                    verified_color,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .blur(4.dp)
                        .padding(vertical = 8.dp),
                    isLoadingConfirm = isConfirmLoading,
                    isLoadingCancel = isCancelLoading
                )
            }

        }
    }
}

private fun cancelRequest(
    sessionRequestUI: SessionRequestUI.Content,
    navController: NavHostController,
    chainAbstractionViewModel: ChainAbstractionViewModel,
    composableScope: CoroutineScope,
    context: Context,
    toggleCancelLoader: (Boolean) -> Unit
) {
    toggleCancelLoader(true)
    if (sessionRequestUI.peerUI.linkMode) {
        navController.popBackStack(route = Route.Connections.path, inclusive = false)
    }
    try {
        chainAbstractionViewModel.reject(
            onSuccess = { uri ->
                toggleCancelLoader(false)
                composableScope.launch(Dispatchers.Main) {
                    navController.popBackStack(route = Route.Connections.path, inclusive = false)
                }
                if (uri != null && uri.toString().isNotEmpty()) {
                    context.sendResponseDeepLink(uri)
                } else {
                    composableScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Go back to your browser", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                toggleCancelLoader(false)
                showError(navController, error, composableScope, context)
            })
    } catch (e: Throwable) {
        showError(navController, e, composableScope, context)
    }
}

private fun confirmRequest(
    sessionRequestUI: SessionRequestUI.Content,
    navController: NavHostController,
    chainAbstractionViewModel: ChainAbstractionViewModel,
    composableScope: CoroutineScope,
    context: Context,
    toggleConfirmLoader: (Boolean) -> Unit
) {
    toggleConfirmLoader(true)
    if (sessionRequestUI.peerUI.linkMode) {
        navController.popBackStack(route = Route.Connections.path, inclusive = false)
    }
    try {
        chainAbstractionViewModel.approve(
            onSuccess = { uri ->
                toggleConfirmLoader(false)
                composableScope.launch(Dispatchers.Main) {
                    navController.popBackStack(route = Route.Connections.path, inclusive = false)
                }
                if (uri != null && uri.toString().isNotEmpty()) {
                    context.sendResponseDeepLink(uri)
                } else {
                    composableScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Go back to your browser", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                toggleConfirmLoader(false)
                showError(navController, error, composableScope, context)
            })

    } catch (e: Throwable) {
        showError(navController, e, composableScope, context)
    }
}

private fun showError(navController: NavHostController, throwable: Throwable?, coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch(Dispatchers.Main) {
        if (throwable !is NoConnectivityException) {
            navController.popBackStack()
        }
        Toast.makeText(context, throwable?.message ?: "Session request error, please check your Internet connection", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun Request(sessionRequestUI: SessionRequestUI.Content, isError: Boolean) {
    Column(modifier = Modifier.height(400.dp)) {
        Content(title = "Transaction") {
            InnerContent {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp, top = 3.dp, end = 8.dp, bottom = 5.dp),
                        text = "Paying", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                    )
                    BlueLabelText("xx USDC") //TODO: GET from ERC20 decoding, how much to send
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            InnerContent {
                Text(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                    text = "Your balance:", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                )
                BlueLabelRow(listOf("xx USDC"))//todo: show balance
                if (!isError){
                    Text(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                        text = "Source of funds:", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                    )
                    val funding = WCDelegate.fulfilmentAvailable!!.funding.map { "${Transaction.hexToTokenAmount(it.amount, 6)!!.toPlainString()} ${it.symbol} from ${it.chainId}" }
                    BlueLabelRow(funding)
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            sessionRequestUI.chain?.let { chain ->
                InnerContent {
                    Text(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                        text = "Chain", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                    )
                    BlueLabelRow(listOf(sessionRequestUI.chain))
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            InnerContent {
                Text(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                    text = "Method", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                )
                BlueLabelRow(listOf(sessionRequestUI.method))
            }
        }
    }
}