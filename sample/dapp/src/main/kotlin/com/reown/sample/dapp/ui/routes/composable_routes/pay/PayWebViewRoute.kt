package com.reown.sample.dapp.ui.routes.composable_routes.pay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.commons.BlueButton
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.dapp.BuildConfig
import com.reown.sample.dapp.DAPP_RETURN_DEEP_LINK
import org.json.JSONObject

/**
 * Embeds the WalletConnect Pay checkout portal inside a native WebView.
 *
 * Ported from the React Native reference (reown-com/react-native-examples PRs #570/#576) and the
 * "Buyer Experience — WebView Integration" doc. The hosted UI drives wallet selection, payment option
 * display, compliance data collection, and signing orchestration; this screen only:
 *  - loads the [gatewayUrl] with `returnUrl` + `preferUniversalLinks=1` appended,
 *  - forwards `?uri=wc:` wallet deeplinks to the OS (WebViews can't route them natively),
 *  - listens for `PAY_SUCCESS` / `PAY_FAILURE` bridge messages via `window.ReactNativeWebView`.
 *
 * The app's registered deep link ([DAPP_RETURN_DEEP_LINK]) is passed as the return destination so the wallet
 * can route the user back after signing. [DappSampleActivity] is `singleTask`, so the return simply
 * foregrounds the existing task — the WebView survives and the checkout resumes.
 */
@Composable
fun PayWebViewRoute(
    navController: NavHostController,
    gatewayUrl: String,
) {
    var result by remember { mutableStateOf<PayResult>(PayResult.InProgress) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .statusBarsPadding()
    ) {
        when (val current = result) {
            is PayResult.InProgress -> PayWebViewContent(
                gatewayUrl = gatewayUrl,
                onSuccess = { message -> result = PayResult.Success(message) },
                onFailure = { error -> result = PayResult.Failure(error) },
            )

            is PayResult.Success -> PayResultContent(
                isSuccess = true,
                title = current.message ?: "Payment confirmed",
                actionLabel = "Done",
                onAction = { navController.popBackStack() },
            )

            is PayResult.Failure -> PayResultContent(
                isSuccess = false,
                title = current.error ?: "Payment failed",
                actionLabel = "Close",
                onAction = { navController.popBackStack() },
            )
        }
    }
}

private sealed interface PayResult {
    data object InProgress : PayResult
    data class Success(val message: String?) : PayResult
    data class Failure(val error: String?) : PayResult
}

@Composable
private fun PayWebViewContent(
    gatewayUrl: String,
    onSuccess: (String?) -> Unit,
    onFailure: (String?) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }

    // Wrap the callbacks so the WebView's JS bridge (registered once in `factory`) always invokes the
    // latest lambdas rather than the ones captured at first composition.
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnFailure by rememberUpdatedState(onFailure)

    // Hold a direct reference to the WebView so `onRelease` can tear it down deterministically,
    // rather than fishing it out of the FrameLayout by child index. Remembered so it survives
    // recompositions (e.g. the `isLoading` toggle) — `onRelease` runs on a later composition.
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // Wrap the WebView in a FrameLayout to isolate it from Compose's rendering (matches
                // the wallet sample's PaymentRoute and avoids input/scroll glitches).
                FrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )

                    val webView = WebView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )

                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false

                        addJavascriptInterface(
                            PayJsBridge(
                                onSuccess = { message -> currentOnSuccess(message) },
                                onFailure = { error -> currentOnFailure(error) },
                            ),
                            // The hosted checkout calls window.ReactNativeWebView.postMessage(json)
                            // on every platform; the bridge name must match exactly.
                            "ReactNativeWebView"
                        )

                        webViewClient = object : WebViewClient() {
                            // API 24+ overload.
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = handleWalletDeeplink(context, request?.url?.toString())

                            // Deprecated pre-API-24 overload — still the only one called on API 23
                            // (the repo's minSdk), so without it deeplink interception silently
                            // no-ops there and `wc:` links would load inside the WebView.
                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean = handleWalletDeeplink(context, url)

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }

                        loadUrl(buildPayUrl(gatewayUrl, DAPP_RETURN_DEEP_LINK))
                    }

                    webViewRef.value = webView
                    addView(webView)
                }
            },
            onRelease = {
                webViewRef.value?.apply {
                    removeJavascriptInterface("ReactNativeWebView")
                    stopLoading()
                    webViewClient = WebViewClient()
                    loadUrl("about:blank")
                    (parent as? android.view.ViewGroup)?.removeView(this)
                    destroy()
                }
                webViewRef.value = null
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WCTheme.colors.bgPrimary),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WCTheme.colors.bgAccentPrimary)
            }
        }
    }
}

@Composable
private fun PayResultContent(
    isSuccess: Boolean,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSuccess) WCTheme.colors.iconSuccess else WCTheme.colors.textError),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSuccess) "✓" else "!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing5))

        Text(
            text = title,
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        // NOTE: In production, treat PAY_SUCCESS only as a trigger to confirm the payment
        // server-side via GET /v1/payments/{paymentId}/status — never as proof on its own.
        BlueButton(
            text = actionLabel,
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        )
    }
}

/** WebView -> native bridge. Methods run on a background thread, so hop to the main thread. */
private class PayJsBridge(
    private val onSuccess: (String?) -> Unit,
    private val onFailure: (String?) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(json: String) {
        val msg = try {
            JSONObject(json)
        } catch (_: Exception) {
            return
        }
        val type = msg.optString("type")
        val hasSuccess = msg.has("success")
        val success = msg.optBoolean("success", false)
        mainHandler.post {
            when {
                type == "PAY_SUCCESS" || (hasSuccess && success) ->
                    onSuccess(msg.optString("message").ifEmpty { null })

                type == "PAY_FAILURE" || (hasSuccess && !success) ->
                    onFailure(msg.optString("error").ifEmpty { null })
            }
        }
    }
}

/**
 * Appends the two required WebView params to the API-provided gateway URL. Never construct the base
 * URL manually — start from the `gatewayUrl` the Merchant API returns.
 */
internal fun buildPayUrl(gatewayUrl: String, appDeepLink: String): String =
    Uri.parse(gatewayUrl).buildUpon()
        .appendQueryParameter("returnUrl", appDeepLink)
        .appendQueryParameter("preferUniversalLinks", "1")
        .build()
        .toString()

/** A navigation is a wallet deeplink when it carries a `?uri=wc:` query parameter. */
internal fun isWalletDeeplink(url: String): Boolean = try {
    Uri.parse(url).getQueryParameter("uri")?.startsWith("wc:") == true
} catch (_: Exception) {
    false
}

/**
 * Shared interception logic for both `shouldOverrideUrlLoading` overloads. Forwards `wc:` wallet
 * deeplinks to the OS and returns `true` (handled); returns `false` for everything else so the
 * WebView loads it in-place. Only `wc:` links are forwarded — other non-https schemes are never
 * routed to the OS.
 */
private fun handleWalletDeeplink(context: Context, url: String?): Boolean {
    if (url == null) return false
    if (isWalletDeeplink(url)) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // No app can handle the wallet deeplink (wallet not installed).
        }
        return true
    }
    return false
}
