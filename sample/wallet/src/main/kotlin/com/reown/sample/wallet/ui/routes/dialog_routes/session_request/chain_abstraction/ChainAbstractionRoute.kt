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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.reown.sample.common.ui.theme.verified_color
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.domain.getErrorMessage
import com.reown.sample.wallet.domain.model.NetworkUtils
import com.reown.sample.wallet.domain.model.Transaction
import com.reown.sample.wallet.ui.common.Buttons
import com.reown.sample.wallet.ui.common.ButtonsVertical
import com.reown.sample.wallet.ui.common.InnerContent
import com.reown.sample.wallet.ui.common.SemiTransparentDialog
import com.reown.sample.wallet.ui.common.generated.ButtonWithLoader
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
    val sessionRequestUI = chainAbstractionViewModel.sessionRequestUI
    val composableScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isConfirmLoading by remember { mutableStateOf(false) }
    var isCancelLoading by remember { mutableStateOf(false) }
    var shouldShowErrorDialog by remember { mutableStateOf(false) }
    var shouldShowSuccessDialog by remember { mutableStateOf(false) }

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
                    Request(chainAbstractionViewModel, isError)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isError) {
                        ButtonWithLoader(
                            buttonColor = Color(0xFF363636),
                            loaderColor = Color(0xFFFFFFFF),
                            modifier = Modifier
                                .padding(8.dp)
                                .height(60.dp)
                                .clickable { navController.popBackStack() },
                            isLoading = false,
                            content = {
                                Text(
                                    text = "Back to App",
                                    style = TextStyle(
                                        fontSize = 20.0.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFFFFF),
                                    ),
                                    modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically)
                                )
                            }
                        )
                    } else {
                        ButtonsVertical(
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
                text = "Something went wrong",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(500),
                    color = Color(0xFFFFFFFF),
                    textAlign = TextAlign.Center,
                )
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(modifier = Modifier.size(64.dp), painter = painterResource(R.drawable.ic_ca_error), contentDescription = null)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(8.dp),
                text = "${chainAbstractionViewModel.errorMessage}",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF9A9A9A),
                    textAlign = TextAlign.Center,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(10.dp)
                .clip(shape = RoundedCornerShape(25.dp))
                .fillMaxWidth()
                .background(themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFF505059).copy(.1f)))
                .verticalScroll(rememberScrollState())
        ) {
            InnerContent {
                Row(
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Paying",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight(400),
                            color = Color(0xFF9A9A9A),
                        )
                    )
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                        Text(
                            text = chainAbstractionViewModel.getTransferAmount(),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFFFFFFFF),
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                            Text(
                                text = "on ${NetworkUtils.getNameByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: "")}",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFF9A9A9A),
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Image(
                                modifier = Modifier.size(12.dp).clip(CircleShape),
                                painter = painterResource(id = NetworkUtils.getIconByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: "")),
                                contentDescription = "image description"
                            )
                        }
                    }
                }
                WCDelegate.fulfilmentAvailable?.funding?.forEach { funding ->
                    Row(
                        modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Bridging",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${Transaction.hexToTokenAmount(funding.amount, 6)?.toPlainString()}${funding.symbol}",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFFFFFFFF),
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                Text(
                                    text = "from ${NetworkUtils.getNameByChainId(funding.chainId)}",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF9A9A9A),
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Image(
                                    modifier = Modifier.size(12.dp).clip(CircleShape),
                                    painter = painterResource(id = NetworkUtils.getIconByChainId(funding.chainId)),
                                    contentDescription = "image description"
                                )
                            }
                        }
                    }
                }
            }
        }
        ButtonWithLoader(
            buttonColor = Color(0xFF363636),
            loaderColor = Color(0xFFFFFFFF),
            modifier = Modifier
                .padding(8.dp)
                .height(60.dp)
                .clickable { navController.popBackStack() },
            isLoading = false,
            content = {
                Text(
                    text = "Back to App",
                    style = TextStyle(
                        fontSize = 20.0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                    ),
                    modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
        )
    }
}

@Composable
fun SuccessDialog(
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
                text = "Transaction Completed",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(500),
                    color = Color(0xFFFFFFFF),
                    textAlign = TextAlign.Center,
                )
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(modifier = Modifier.size(64.dp), painter = painterResource(R.drawable.ic_frame), contentDescription = null)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(8.dp),
                text = "You successfully send ${WCDelegate.fulfilmentAvailable?.initialTransactionMetadata?.symbol ?: ""}",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF9A9A9A),
                    textAlign = TextAlign.Center,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(10.dp)
                .clip(shape = RoundedCornerShape(25.dp))
                .fillMaxWidth()
                .background(themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFF505059).copy(.1f)))
                .verticalScroll(rememberScrollState())
        ) {
            InnerContent {
                Row(
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Paying",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight(400),
                            color = Color(0xFF9A9A9A),
                        )
                    )
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                        Text(
                            text = chainAbstractionViewModel.getTransferAmount(),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFFFFFFFF),
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                            Text(
                                text = "on ${NetworkUtils.getNameByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: "")}",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFF9A9A9A),
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Image(
                                modifier = Modifier.size(12.dp).clip(CircleShape),
                                painter = painterResource(id = NetworkUtils.getIconByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: "")),
                                contentDescription = "image description"
                            )
                        }
                    }
                }
                WCDelegate.fulfilmentAvailable?.funding?.forEach { funding ->
                    Row(
                        modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Bridging",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${Transaction.hexToTokenAmount(funding.amount, 6)?.toPlainString()}${funding.symbol}",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFFFFFFFF),
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                Text(
                                    text = "from ${NetworkUtils.getNameByChainId(funding.chainId)}",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF9A9A9A),
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Image(
                                    modifier = Modifier.size(12.dp).clip(CircleShape),
                                    painter = painterResource(id = NetworkUtils.getIconByChainId(funding.chainId)),
                                    contentDescription = "image description"
                                )
                            }
                        }
                    }
                }
            }
        }
        ButtonWithLoader(
            buttonColor = Color(0xFF363636),
            loaderColor = Color(0xFFFFFFFF),
            modifier = Modifier
                .padding(8.dp)
                .height(60.dp)
                .clickable { navController.popBackStack() },
            isLoading = false,
            content = {
                Text(
                    text = "Back to App",
                    style = TextStyle(
                        fontSize = 20.0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                    ),
                    modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
        )
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
fun Request(viewModel: ChainAbstractionViewModel, isError: Boolean) {
    Column(modifier = Modifier.height(450.dp)) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .clip(shape = RoundedCornerShape(25.dp))
                .fillMaxWidth()
                .background(themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFF505059).copy(.1f)))
                .verticalScroll(rememberScrollState())
        ) {
            if (!isError) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Paying",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight(400),
                            color = Color(0xFF9A9A9A),
                        )
                    )
                    Text(
                        text = viewModel.getTransferAmount(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight(400),
                            color = Color(0xFFFFFFFF),
                        )
                    )
                }
            }
            //Content
            InnerContent {
                Text(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                    text = "Source of funds",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight(400),
                        color = Color(0xFF9A9A9A),
                    )
                )

                Row(
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            modifier = Modifier.size(24.dp).clip(CircleShape),
                            painter = painterResource(id = NetworkUtils.getIconByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: "")),
                            contentDescription = "Network"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Balance",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                    }
                    Column {
                        Text(
                            text = Transaction.convertTokenAmount(viewModel.getERC20Balance().toBigInteger(), 6)?.toPlainString() ?: "-.--",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFFFFFFFF),
                            )
                        )
                        if (isError) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = getErrorMessage(),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight(500),
                                    color = Color(0xFFDF4A34),
                                    textAlign = TextAlign.Right,
                                )
                            )
                        }
                    }

                }

                if (!isError) {
                    WCDelegate.fulfilmentAvailable?.funding?.forEach { funding ->
                        Row(
                            modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 38.dp)
                            ) {
                                Image(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(id = R.drawable.ic_bridge),
                                    contentDescription = "Bridge"
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Bridging",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF9A9A9A),
                                    )
                                )
                            }
                            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${Transaction.hexToTokenAmount(funding.amount, 6)?.toPlainString()}${funding.symbol}",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFFFFFFFF),
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                    Text(
                                        text = "from ${NetworkUtils.getNameByChainId(funding.chainId)}",
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp,
                                            fontWeight = FontWeight(400),
                                            color = Color(0xFF9A9A9A),
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Image(
                                        modifier = Modifier.size(12.dp).clip(CircleShape),
                                        painter = painterResource(id = NetworkUtils.getIconByChainId(funding.chainId)),
                                        contentDescription = "image description"
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (!isError) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .clip(shape = RoundedCornerShape(25.dp))
                    .fillMaxWidth()
                    .background(themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFF505059).copy(.1f)))
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Network",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight(400),
                            color = Color(0xFF9A9A9A),
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            modifier = Modifier.size(18.dp).clip(CircleShape),
                            painter = painterResource(id = NetworkUtils.getIconByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: "")),
                            contentDescription = "Network",
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = NetworkUtils.getNameByChainId(WCDelegate.fulfilmentAvailable?.initialTransaction?.chainId ?: ""),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFFFFFFFF),
                            )
                        )
                    }
                }

                InnerContent {
                    Row(
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Estimated Fees",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                        Text(
                            text = "${WCDelegate.fulfilmentDetails?.localTotal?.formattedAlt} ${WCDelegate.fulfilmentDetails?.localTotal?.symbol}",
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFFFFFFFF),
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Bridge",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                        Text(
                            text = viewModel.calculateBridgeFee(),
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Purchase",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                        Text(
                            text = "${WCDelegate.fulfilmentDetails?.initialDetails?.transactionFee?.localFee?.formattedAlt} ${WCDelegate.fulfilmentDetails?.initialDetails?.transactionFee?.localFee?.symbol}",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight(400),
                                color = Color(0xFF9A9A9A),
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
    }
}