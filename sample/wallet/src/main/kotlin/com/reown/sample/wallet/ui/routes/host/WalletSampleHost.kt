@file:OptIn(ExperimentalMaterialNavigationApi::class)

package com.reown.sample.wallet.ui.routes.host

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.pandulapeter.beagle.DebugMenuView
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.Web3WalletNavGraph
import com.reown.sample.wallet.ui.Web3WalletViewModel
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.state.ConnectionState
import com.reown.sample.wallet.ui.state.PairingEvent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun WalletSampleHost(
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    web3walletViewModel: Web3WalletViewModel,
    connectionsViewModel: ConnectionsViewModel,
) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    val connectionState by web3walletViewModel.connectionState.collectAsState()
    val bottomBarState = rememberBottomBarMutableState()
    val isLoader by web3walletViewModel.isLoadingFlow.collectAsState(false)
    val isRequestLoader by web3walletViewModel.isRequestLoadingFlow.collectAsState(false)
    val fallbackDimColor = WCTheme.colors.bgInvert.copy(alpha = 0.48f)
    val sheetState = remember(bottomSheetNavigator) { bottomSheetNavigator.navigatorSheetState }
    val isSheetVisible =
        sheetState.currentValue != ModalBottomSheetValue.Hidden || sheetState.targetValue != ModalBottomSheetValue.Hidden
    var shouldDimBackground by remember { mutableStateOf(true) }
    val dimProgress by animateFloatAsState(
        targetValue = if (isSheetVisible && shouldDimBackground) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "bottom_sheet_dim_alpha"
    )

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val route = destination.route ?: ""
            val shouldHideBottomBar = route.startsWith(Route.SnackbarMessage.path)
            bottomBarState.value = bottomBarState.value.copy(isDisplayed = !shouldHideBottomBar)
            shouldDimBackground = !route.startsWith(Route.SnackbarMessage.path)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    LaunchedEffect(Unit) {
        web3walletViewModel.eventsSharedFlow.collect {
            when (it) {
                is PairingEvent.Error -> {
                    if (navController.currentDestination?.route != Route.Wallets.path) {
                        navController.popBackStack(route = Route.Wallets.path, inclusive = false)
                    }
                    Toast.makeText(navController.context, it.message, Toast.LENGTH_SHORT).show()
                }

                is PairingEvent.ProposalExpired -> {
                    Toast.makeText(navController.context, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(topStart = WCTheme.borderRadius.radius6, topEnd = WCTheme.borderRadius.radius6),
        sheetBackgroundColor = Color.Transparent,
        sheetElevation = 0.dp,
        scrimColor = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                scaffoldState = scaffoldState,
                drawerGesturesEnabled = true,
                drawerContent = { BeagleDrawer() },
                bottomBar = {
                    if (bottomBarState.value.isDisplayed) {
                        BottomBar(navController, bottomBarState.value)
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    Web3WalletNavGraph(
                        bottomSheetNavigator = bottomSheetNavigator,
                        navController = navController,
                        web3walletViewModel = web3walletViewModel,
                        connectionsViewModel = connectionsViewModel,
                    )

                    if (connectionState is ConnectionState.Error) {
                        Banner(
                            message = "Network connection lost: ${(connectionState as ConnectionState.Error).message}",
                            backgroundColor = WCTheme.colors.textError,
                            iconResId = R.drawable.invalid_domain,
                            durationMs = 5000
                        )
                    } else if (connectionState is ConnectionState.Ok) {
                        Banner(
                            message = "Network connection is OK",
                            backgroundColor = WCTheme.colors.textSuccess,
                            iconResId = R.drawable.ic_check_white,
                            durationMs = 2000
                        )
                    }

                    if (isLoader) {
                        Loader(initMessage = "WalletConnect is pairing...", updateMessage = "Pairing is taking longer than usual, please try again...")
                    }

                    if (isRequestLoader) {
                        Loader(initMessage = "Awaiting a request...", updateMessage = "It is taking longer than usual..")
                    }

                    Timer(web3walletViewModel)
                }
            }

            if (dimProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallbackDimColor.copy(alpha = fallbackDimColor.alpha * dimProgress))
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Timer(web3walletViewModel: Web3WalletViewModel) {
    val timer by web3walletViewModel.timerFlow.collectAsState()
    Text(
        modifier = Modifier
            .align(Alignment.BottomStart),
        text = timer,
        maxLines = 1,
        style = WCTheme.typography.bodySmMedium.copy(
            color = WCTheme.colors.textTertiary
        ),
    )
}

@Composable
private fun BeagleDrawer() {
    AndroidView(factory = { DebugMenuView(it) }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun BoxScope.Loader(initMessage: String, updateMessage: String) {
    var shouldChangeText by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        delay(15000)
        shouldChangeText = true
    }

    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(34.dp))
            .background(themedColor(Color(0xFF242425).copy(alpha = .95f), Color(0xFFF2F2F7).copy(alpha = .95f)))
            .padding(WCTheme.spacing.spacing6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            strokeWidth = 8.dp,
            modifier = Modifier
                .size(75.dp), color = Color(0xFFB8F53D)
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
        Text(
            textAlign = TextAlign.Center,
            text = if (shouldChangeText) updateMessage else initMessage,
            maxLines = 2,
            style = WCTheme.typography.h6Medium.copy(
                color = WCTheme.colors.textTertiary
            ),
        )
    }
}

@Composable
private fun Banner(message: String, backgroundColor: Color, iconResId: Int, durationMs: Long) {
    var shouldShow by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        delay(durationMs)
        shouldShow = false
    }

    if (shouldShow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = WCTheme.spacing.spacing4, vertical = WCTheme.spacing.spacing2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                imageVector = ImageVector.vectorResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(color = WCTheme.colors.textInvert)
            )
            Spacer(modifier = Modifier.width(WCTheme.spacing.spacing1))
            Text(text = message, style = WCTheme.typography.bodyMdRegular.copy(color = WCTheme.colors.textInvert))
        }
    }
}

@Preview
@Composable
private fun PreviewPairingLoader() {
    Box() {
        Loader("", "")
    }
}
