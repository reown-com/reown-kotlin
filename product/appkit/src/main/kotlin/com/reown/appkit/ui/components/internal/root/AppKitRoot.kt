@file:OptIn(ExperimentalComposeUiApi::class)

package com.reown.appkit.ui.components.internal.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.reown.modal.ui.components.common.roundedClickable
import com.reown.appkit.client.Modal
import com.reown.appkit.domain.delegate.AppKitDelegate
import com.reown.appkit.ui.components.internal.AppKitTopBar
import com.reown.appkit.ui.components.internal.commons.BackArrowIcon
import com.reown.appkit.ui.components.internal.commons.FullWidthDivider
import com.reown.appkit.ui.components.internal.commons.QuestionMarkIcon
import com.reown.appkit.ui.components.internal.snackbar.ModalSnackBarHost
import com.reown.appkit.ui.components.internal.snackbar.SnackBarState
import com.reown.appkit.ui.components.internal.snackbar.rememberSnackBarState
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.previews.MultipleComponentsPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.ProvideAppKitThemeComposition
import com.reown.appkit.ui.theme.AppKitTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach

@Composable
internal fun AppKitRoot(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    closeModal: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rootState = rememberAppKitRootState(coroutineScope = scope, navController = navController)
    val snackBarState = rememberSnackBarState(coroutineScope = scope)
    val title by rootState.title.collectAsState(null)

    LaunchedEffect(Unit) {
        AppKitDelegate
            .wcEventModels
            .filterIsInstance<Modal.Model.Error>()
            .onEach { event ->
                snackBarState.showErrorSnack(event.throwable.localizedMessage ?: "Something went wrong")
            }
            .collect()
    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
    ) {
        ProvideAppKitThemeComposition {
            AppKitRoot(rootState, snackBarState, title, closeModal, content)
        }
    }
}

@Composable
internal fun AppKitRoot(
    rootState: AppKitRootState,
    snackBarState: SnackBarState,
    title: String?,
    closeModal: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalSnackBarHost(snackBarState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppKitTheme.colors.background.color125)
        ) {
            title?.let { title ->
                AppKitTopBar(
                    title = title,
                    startIcon = { TopBarStartIcon(rootState) },
                    onCloseIconClick = closeModal
                )
                FullWidthDivider()
            }
            content()
        }
    }
}

@Composable
private fun TopBarStartIcon(
    rootState: AppKitRootState
) {
    if (rootState.currentDestinationRoute == Route.SIWE_FALLBACK.path) {
        questionMark(rootState)
    } else {
        if (rootState.canPopUp) {
            val keyboardController = LocalSoftwareKeyboardController.current
            BackArrowIcon(onClick = {
                keyboardController?.hide()
                rootState.popUp()
            })
        } else {
            when (rootState.currentDestinationRoute) {
                Route.CONNECT_YOUR_WALLET.path -> questionMark(rootState)
            }
        }
    }
}

@Composable
private fun questionMark(rootState: AppKitRootState) {
    QuestionMarkIcon(
        modifier = Modifier
            .size(36.dp)
            .roundedClickable(onClick = rootState::navigateToHelp)
            .padding(10.dp)
    )
}

@Composable
@UiModePreview
private fun PreviewAppKitRoot() {
    val content: @Composable () -> Unit = { Box(modifier = Modifier.size(200.dp)) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val rootState = rememberAppKitRootState(coroutineScope = scope, navController = navController)
    val snackBarState = rememberSnackBarState(coroutineScope = scope)

    MultipleComponentsPreview(
        { AppKitRoot(rootState, snackBarState, null, {}, { content() }) },
        { AppKitRoot(rootState, snackBarState, "Top Bar Title", {}, { content() }) }
    )
}

