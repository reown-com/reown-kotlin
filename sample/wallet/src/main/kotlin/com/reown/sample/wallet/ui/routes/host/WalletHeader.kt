package com.reown.sample.wallet.ui.routes.host

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

@Composable
fun WalletHeader(navController: NavController) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WCTheme.spacing.spacing5)
            .padding(top = statusBarTop + WCTheme.spacing.spacing2, bottom = WCTheme.spacing.spacing2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Blue circle with WC brandmark
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(WCTheme.colors.bgAccentPrimary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_walletconnect_brandmark),
                contentDescription = "WalletConnect Logo",
                modifier = Modifier.size(width = 29.dp, height = 18.dp),
                colorFilter = ColorFilter.tint(WCTheme.colors.textInvert)
            )
        }

        // Scan button - inverted rounded rect with barcode icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(WCTheme.borderRadius.shapeMedium)
                .background(WCTheme.colors.bgInvert)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = { navController.navigate(Route.ScannerOptions.path) }
                )
                .testTag("button-scan"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_qr_code),
                contentDescription = "Scan",
                modifier = Modifier.size(18.dp),
                tint = WCTheme.colors.bgPrimary
            )
        }
    }
}
