@file:OptIn(ExperimentalMaterialApi::class)

package com.reown.sample.dapp.ui.routes.host

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.navigation.BottomSheetNavigator
import com.reown.appkit.client.AppKit
import com.reown.sample.dapp.R
import com.reown.sample.dapp.ui.DappSampleEvents
import com.reown.sample.dapp.ui.DappSampleNavGraph
import com.reown.sample.dapp.ui.routes.Route
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DappSampleHost() {
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    var isOfflineState: Boolean? by remember { mutableStateOf(null) }
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    // Both must survive recomposition (e.g. a relay reconnect when the app comes
    // back to the foreground): a new navigator isn't the one the NavController
    // drives, and a new start destination makes NavHost replace the graph. Either
    // one, right as a session settles, can leave a blank screen instead of the
    // session route.
    val bottomSheetNavigator = remember(sheetState) { BottomSheetNavigator(sheetState) }
    val navController = rememberNavController(bottomSheetNavigator)
    val viewModel: DappSampleViewModel = viewModel()
    val startDestination = remember {
        if (AppKit.getAccount() != null) Route.Session.path else Route.ChainSelection.path
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            Timber.d("DappNav: destination ${destination.route}")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DappSampleEvents.ConnectionEvent -> {
                    Timber.d("DappNav: relay available=${event.isAvailable}")
                    isOfflineState = !event.isAvailable
                }
                is DappSampleEvents.Disconnect -> navController.navigate(Route.ChainSelection.path) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
                is DappSampleEvents.RequestError -> scaffoldState.snackbarHostState.showSnackbar(event.exceptionMsg)
                is DappSampleEvents.SessionExtend -> scaffoldState.snackbarHostState.showSnackbar("Session extended")
                else -> Unit
            }
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        // Exposes testTags as resource ids, so Maestro E2E flows can select them by `id`.
        modifier = Modifier.semantics { testTagsAsResourceId = true }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            DappSampleNavGraph(
                bottomSheetNavigator = bottomSheetNavigator,
                navController = navController,
                startDestination = startDestination,
            )

            if (isOfflineState != null) {
                if (isOfflineState == true) {
                    NoConnectionIndicator()
                } else {
                    RestoredConnectionIndicator()
                }
            }
        }
    }
}

@Composable
private fun NoConnectionIndicator() {
    var shouldShow by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        delay(3000)
        shouldShow = false
    }

    if (shouldShow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3496ff))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "No internet connection", color = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_offline),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(color = Color.White)
            )
        }
    }
}

@Composable
private fun RestoredConnectionIndicator() {
    var shouldShow by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        delay(3000)
        shouldShow = false
    }
    if (shouldShow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF93c47d))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "Network connection is OK", color = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_white_check),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(color = Color.White)
            )
        }
    }
}
