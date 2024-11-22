package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.common.sendResponseDeepLink
import com.reown.sample.common.ui.theme.mismatch_color
import com.reown.sample.common.ui.theme.verified_color
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.R
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
import com.reown.sample.wallet.ui.common.generated.CancelButton
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
    val sessionRequestUI = chainAbstractionViewModel.sessionRequestUI
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isConfirmLoading by remember { mutableStateOf(false) }
    var isCancelLoading by remember { mutableStateOf(false) }
    var shouldShowErrorDialog by remember { mutableStateOf(false) }
    var shouldShowSuccessDialog by remember { mutableStateOf(true) }

    when {
        shouldShowSuccessDialog -> SuccessDialog(navController, chainAbstractionViewModel)
        shouldShowErrorDialog -> ErrorDialog(navController, chainAbstractionViewModel)
        else -> when (sessionRequestUI) {
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
                            onConfirm = {
                                confirmRequest(
                                    sessionRequestUI,
                                    navController,
                                    chainAbstractionViewModel,
                                    composableScope,
                                    context,
                                    toggleConfirmLoader = { isConfirmLoading = it },
                                    onSuccess = { hash ->
                                        chainAbstractionViewModel.txHash = hash
                                        shouldShowSuccessDialog = true
                                    },
                                    onError = { message: String ->
                                        chainAbstractionViewModel.errorMessage = message
                                        shouldShowErrorDialog = true
                                    }
                                )
                            },
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


}

@Composable
fun ErrorDialog(
    navController: NavHostController,
    chainAbstractionViewModel: ChainAbstractionViewModel
) {
    SemiTransparentDialog {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                text = "Something went wrong!",
                style = TextStyle(color = Color(0xFFFFFFFF), textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(modifier = Modifier.size(64.dp), painter = painterResource(R.drawable.ic_scam), contentDescription = null)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${chainAbstractionViewModel.errorMessage}",
                style = TextStyle(color = Color(0xFFFFFFFF), fontSize = 16.sp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
        ) {
            InnerContent {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp, top = 3.dp, end = 8.dp, bottom = 5.dp),
                        text = "Paying", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                    )
                    BlueLabelText("xx USDC") //TODO: GET from ERC20 decoding, how much to send, add chain with icon
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp, top = 3.dp, end = 8.dp, bottom = 5.dp),
                        text = "Source of funds:",
                        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                    )
                    val funding = WCDelegate.fulfilmentAvailable!!.funding.map { "${Transaction.hexToTokenAmount(it.amount, 6)!!.toPlainString()} ${it.symbol} from ${it.chainId}" }
                    Column {
                        funding.forEach {
                            BlueLabelText(it)
                        }
                    }
                }
            }
        }
        CancelButton(
            text = "Back to App",
            modifier = Modifier
                .padding(16.dp)
                .height(46.dp)
                .fillMaxWidth()
                .clickable {
                    navController.popBackStack(
                        route = Route.Connections.path,
                        inclusive = false
                    )
                },
            backgroundColor = Color(0xFFFFFFFF).copy(.25f)
        )
    }
}

@Composable
fun SuccessDialog(
    navController: NavHostController,
    chainAbstractionViewModel: ChainAbstractionViewModel
) {
    SemiTransparentDialog {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                text = "Transaction completed!",
                style = TextStyle(color = Color(0xFFFFFFFF), textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(modifier = Modifier.size(64.dp), painter = painterResource(R.drawable.ic_frame), contentDescription = null)
            Text(text = "You successfully sent USDC!", style = TextStyle(color = Color(0xFFFFFFFF), fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                InnerContent {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            modifier = Modifier.padding(start = 8.dp, top = 3.dp, end = 8.dp, bottom = 5.dp),
                            text = "Paying", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                        )
                        BlueLabelText("xx USDC") //TODO: GET from ERC20 decoding, how much to send, add chain with icon
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            modifier = Modifier.padding(start = 8.dp, top = 3.dp, end = 8.dp, bottom = 5.dp),
                            text = "Source of funds:",
                            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                        )
                        val funding = WCDelegate.fulfilmentAvailable!!.funding.map { "${Transaction.hexToTokenAmount(it.amount, 6)!!.toPlainString()} ${it.symbol} from ${it.chainId}" }
                        Column {
                            funding.forEach {
                                BlueLabelText(it)
                            }
                        }
                    }
                }
            }
            CancelButton(
                text = "Back to App",
                modifier = Modifier
                    .padding(16.dp)
                    .height(46.dp)
                    .fillMaxWidth()
                    .clickable {
                        navController.popBackStack(
                            route = Route.Connections.path,
                            inclusive = false
                        )
                    },
                backgroundColor = Color(0xFFFFFFFF).copy(.25f)
            )
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
    toggleConfirmLoader: (Boolean) -> Unit,
    onSuccess: (hash: String) -> Unit,
    onError: (message: String) -> Unit
) {
    toggleConfirmLoader(true)
    if (sessionRequestUI.peerUI.linkMode) {
        navController.popBackStack(route = Route.Connections.path, inclusive = false)
    }
    try {
        chainAbstractionViewModel.approve(
            onSuccess = { result ->
                toggleConfirmLoader(false)
                if (result.redirect != null && result.redirect.toString().isNotEmpty()) {
                    context.sendResponseDeepLink(result.redirect)
                } else {
                    onSuccess(result.hash)
                }
            },
            onError = { error ->
                toggleConfirmLoader(false)
                handleError(error, composableScope, context, onError)
            })

    } catch (e: Throwable) {
        handleError(e, composableScope, context, onError)
    }
}

private fun handleError(error: Throwable, composableScope: CoroutineScope, context: Context, onError: (message: String) -> Unit) {
    if (error is NoConnectivityException) {
        composableScope.launch(Dispatchers.Main) {
            Toast.makeText(context, error.message ?: "Session request error, please check your Internet connection", Toast.LENGTH_SHORT).show()
        }
    } else {
        onError(error.message ?: "Session request error, please check your Internet connection")
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
                if (!isError) {
                    Text(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                        text = "Source of funds:",
                        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
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