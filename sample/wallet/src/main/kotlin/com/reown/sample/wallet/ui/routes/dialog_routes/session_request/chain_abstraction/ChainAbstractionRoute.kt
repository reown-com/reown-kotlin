package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.chain_abstraction

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.reown.sample.common.ui.theme.verified_color
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.domain.WCDelegate
import com.reown.sample.wallet.ui.common.Buttons
import com.reown.sample.wallet.ui.common.Content
import com.reown.sample.wallet.ui.common.InnerContent
import com.reown.sample.wallet.ui.common.SemiTransparentDialog
import com.reown.sample.wallet.ui.common.blue.BlueLabelRow
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
    when (sessionRequestUI) {
        is SessionRequestUI.Content -> {
            val allowButtonColor = getColor(sessionRequestUI.peerContextUI)
            WCDelegate.currentId = sessionRequestUI.requestId
            SemiTransparentDialog {
                Spacer(modifier = Modifier.height(24.dp))
                Peer(peerUI = sessionRequestUI.peerUI, "sends a request", sessionRequestUI.peerContextUI)
                Spacer(modifier = Modifier.height(16.dp))
                Request(sessionRequestUI = sessionRequestUI)
                Spacer(modifier = Modifier.height(16.dp))
                Buttons(
                    allowButtonColor,
                    onConfirm = { confirmRequest(sessionRequestUI, navController, chainAbstractionViewModel, composableScope, context) { isConfirmLoading = it } },
                    onCancel = { cancelRequest(sessionRequestUI, navController, chainAbstractionViewModel, composableScope, context) { isCancelLoading = it } },
                    isLoadingConfirm = isConfirmLoading,
                    isLoadingCancel = isCancelLoading
                )
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
fun Request(sessionRequestUI: SessionRequestUI.Content) {
    Column(modifier = Modifier.height(400.dp)) {
        Content(title = "Request") {
            InnerContent {
                Text(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 13.dp),
                    text = "Params", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, top = 0.dp, end = 5.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            themedColor(
                                darkColor = Color(0xFFE4E4E7).copy(alpha = .12f),
                                lightColor = Color(0xFF505059).copy(.1f)
                            )
                        )
                        .padding(start = 8.dp, top = 5.dp, end = 8.dp, bottom = 5.dp),
                    text = sessionRequestUI.param,
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = themedColor(darkColor = Color(0xFF9ea9a9), lightColor = Color(0xFF788686)))
                )
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