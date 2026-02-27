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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.sample.common.ui.themedColor
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

@Composable
fun WalletHeader(navController: NavController) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeTop = if (statusBarTop > 0.dp) statusBarTop + 8.dp else 45.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = safeTop, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Blue circle with WC brandmark
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF0988F0)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_walletconnect_logo),
                contentDescription = "WalletConnect Logo",
                modifier = Modifier.size(width = 28.dp, height = 18.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }

        // Scan button - dark rounded rect with barcode icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    themedColor(
                        darkColor = Color(0xFFFFFFFF),
                        lightColor = Color(0xFF202020)
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = { navController.navigate(Route.ScannerOptions.path) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_qr_code),
                contentDescription = "Scan",
                modifier = Modifier.size(18.dp),
                tint = themedColor(
                    darkColor = Color(0xFF202020),
                    lightColor = Color(0xFFFFFFFF)
                )
            )
        }
    }
}
