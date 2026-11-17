package com.reown.modal.utils

import androidx.compose.ui.platform.UriHandler
import timber.log.Timber
import java.net.URLEncoder

private const val WC_URI_QUERY = "wc?uri="

fun UriHandler.openUri(uri: String, onError: (e: Throwable) -> Unit) {
    try {
        openUri(uri)
    } catch (e: Throwable) {
        onError(e)
    }
}

fun UriHandler.goToNativeWallet(uri: String, nativeLink: String?) {
    try {
        nativeLink?.let { openUri(formatNativeDeeplink(it, uri)) } ?: Timber.e("Invalid native link")
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun UriHandler.openMobileLink(
    uri: String,
    mobileLink: String?,
    onError: () -> Unit
) {
    try {
        mobileLink?.let { link -> openUri(formatNativeDeeplink(link, uri)) } ?: onError()
    } catch (e: Exception) {
        onError()
    }
}

fun UriHandler.openWebAppLink(uri: String, universalLink: String?) {
    try {
        universalLink?.let { openUri(formatUniversalLink(it, uri)) } ?: Timber.e("Invalid universal link")
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun UriHandler.goToPlayStore(playStoreLink: String) {
    try {
        openUri(playStoreLink)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

internal fun formatNativeDeeplink(nativeLink: String, uri: String): String {
    // Android intent-filter scheme matching is case-sensitive, so lowercase only the scheme
    var base = if (nativeLink.contains("://")) {
        val (scheme, rest) = nativeLink.split("://", limit = 2)
        scheme.lowercase() + "://" + rest
    } else {
        nativeLink.replace("/", "").replace(":", "").lowercase() + "://"
    }
    if (!base.endsWith('/')) base += "/"
    return base + WC_URI_QUERY + uri.encodeUri()
}

private fun formatUniversalLink(universalLink: String, uri: String): String {
    val plainAppUrl = if (universalLink.endsWith('/')) {
        universalLink.dropLast(1)
    } else {
        universalLink
    }
    return plainAppUrl + "/" + WC_URI_QUERY + uri.encodeUri()
}

private fun String.encodeUri() = URLEncoder.encode(this, "UTF-8")

fun UriHandler.openPlayStore(playStoreLink: String?) {
    playStoreLink?.let { openUri(it) } ?: Timber.e("Invalid play store link")
}
