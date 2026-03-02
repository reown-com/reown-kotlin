@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.reown.android.internal.common.exception.NoConnectivityException
import com.reown.sample.common.sendResponseDeepLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun showError(
    navController: NavHostController,
    throwable: Throwable?,
    coroutineScope: CoroutineScope,
    context: Context,
    defaultMessage: String = "Error, please check your Internet connection"
) {
    coroutineScope.launch(Dispatchers.Main) {
        if (throwable !is NoConnectivityException) {
            navController.popBackStack()
        }
        Toast.makeText(context, throwable?.message ?: defaultMessage, Toast.LENGTH_SHORT).show()
    }
}

internal fun handleRedirect(
    redirect: String?,
    navController: NavHostController,
    composableScope: CoroutineScope,
    context: Context,
) {
    composableScope.launch(Dispatchers.Main) {
        navController.popBackStack()
    }
    if (!redirect.isNullOrEmpty()) {
        context.sendResponseDeepLink(redirect.toUri())
    } else {
        composableScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Go back to your browser", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun handleRedirect(
    uri: Uri?,
    navController: NavHostController,
    composableScope: CoroutineScope,
    context: Context,
) {
    handleRedirect(uri?.toString(), navController, composableScope, context)
}
