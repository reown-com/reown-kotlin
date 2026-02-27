package com.reown.sample.wallet.ui.routes.host

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

@Composable
fun WalletHeader(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 45.dp, end = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_walletconnect_logo),
            contentDescription = "WalletConnect Logo",
            modifier = Modifier.size(width = 36.dp, height = 25.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground)
        )

        Row {
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        onClick = { navController.navigate(Route.PasteUri.path) }
                    )
                    .padding(12.dp),
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "Paste URI",
            )
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        onClick = { navController.navigate(Route.ScanUri.path) }
                    )
                    .padding(12.dp),
                painter = painterResource(id = R.drawable.ic_qr_code),
                contentDescription = "Scan QR Code",
            )
        }
    }
}
