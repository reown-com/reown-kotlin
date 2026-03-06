@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

@Composable
fun AppConnectionRow(
    iconUrl: String?,
    name: String,
    uri: String?,
    chainIds: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius
    val iconSize = spacing.spacing10 + spacing.spacing1
    val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
    val borderWidth = spacing.spacing05 / 2

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        val iconModifier = Modifier
            .size(iconSize)
            .clip(borderRadius.shapeMedium)
            .border(
                width = borderWidth,
                shape = borderRadius.shapeMedium,
                color = colors.foregroundSecondary
            )

        if (iconUrl?.isNotBlank() == true) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(iconUrl)
                    .size(iconSizePx)
                    .crossfade(true)
                    .error(com.reown.sample.common.R.drawable.ic_walletconnect_circle_blue)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = "$name icon",
                modifier = iconModifier,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        } else {
            Icon(
                modifier = iconModifier.alpha(.7f),
                imageVector = ImageVector.vectorResource(id = R.drawable.sad_face),
                contentDescription = "No icon"
            )
        }

        Spacer(modifier = Modifier.width(spacing.spacing3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = WCTheme.typography.bodyLgMedium.copy(
                    color = colors.textPrimary
                )
            )
            Text(
                text = formatDomain(uri),
                style = WCTheme.typography.bodySmRegular.copy(
                    color = colors.textSecondary
                )
            )
        }

        ChainIcons(chainIds = chainIds, size = spacing.spacing5)
    }
}
