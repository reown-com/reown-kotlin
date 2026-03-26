@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import android.net.Uri
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.ui.common.peer.Validation

@Composable
fun AppInfoCard(
    url: String?,
    validation: Validation?,
    isScam: Boolean?,
    isExpanded: Boolean? = null,
    onPress: (() -> Unit)? = null,
) {
    val isControlled = isExpanded != null
    var internalIsExpanded by remember { mutableStateOf(false) }

    val expanded = if (isControlled) isExpanded!! else internalIsExpanded
    val onToggle = if (isControlled) (onPress ?: {}) else { -> internalIsExpanded = !internalIsExpanded }

    AccordionCard(
        headerContent = {
            val colors = WCTheme.colors
            Text(
                text = formatDomain(url),
                style = WCTheme.typography.bodyLgRegular.copy(
                    color = colors.textTertiary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        rightContent = {
            VerifiedBadge(validation = validation, isScam = isScam)
        },
        isExpanded = expanded,
        onPress = onToggle,
    ) {
        AppPermissions()
    }
}

internal fun formatDomain(url: String?): String {
    if (url.isNullOrBlank()) return "Unknown"
    return try {
        (Uri.parse(url).host ?: url).removePrefix("www.")
    } catch (_: Exception) {
        url
    }
}
