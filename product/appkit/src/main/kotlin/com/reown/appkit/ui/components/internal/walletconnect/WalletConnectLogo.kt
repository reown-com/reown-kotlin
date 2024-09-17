package com.reown.appkit.ui.components.internal.walletconnect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.reown.appkit.R
import com.reown.appkit.ui.components.internal.commons.ContentDescription
import com.reown.appkit.ui.previews.MultipleComponentsPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.AppKitTheme

@Composable
internal fun WalletConnectLogo(
    isEnabled: Boolean = true
) {
    val background: Color
    val border: Color
    val colorFilter: ColorFilter?
    if (isEnabled) {
        background = AppKitTheme.colors.accent100
        border = AppKitTheme.colors.grayGlass10
        colorFilter = null
    } else {
        background = AppKitTheme.colors.background.color300
        border = AppKitTheme.colors.grayGlass05
        colorFilter = ColorFilter.tint(AppKitTheme.colors.grayGlass30)
    }

    Image(
        modifier = Modifier
            .size(40.dp)
            .background(background, shape = RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(4.dp),
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_wallet_connect_logo),
        contentDescription = ContentDescription.WC_LOGO.description,

        colorFilter = colorFilter
    )
}

@UiModePreview
@Composable
private fun PreviewWalletConnectLogo() {
    MultipleComponentsPreview(
        { WalletConnectLogo() },
        { WalletConnectLogo(false) }
    )
}