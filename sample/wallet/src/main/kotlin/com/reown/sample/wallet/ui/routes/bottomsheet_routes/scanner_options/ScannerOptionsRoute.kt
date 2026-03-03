package com.reown.sample.wallet.ui.routes.bottomsheet_routes.scanner_options

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

@Composable
fun ScannerOptionsRoute(
    navController: NavController,
    onPair: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = themedColor(
                    darkColor = Color(0xFF1A1A1A),
                    lightColor = Color(0xFFFFFFFF)
                ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(20.dp)
    ) {
        // Close button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ModalCloseButton(onClick = { navController.popBackStack() })
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scan QR code option
        OptionCard(
            label = "Scan QR code",
            iconRes = R.drawable.ic_scan_qr,
            onClick = {
                navController.popBackStack()
                navController.navigate(Route.ScanUri.path)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Paste a URL option
        OptionCard(
            label = "Paste a URL",
            iconRes = R.drawable.ic_paste_url,
            onClick = {
                val text = clipboardManager.getText()?.text?.trim()
                if (text.isNullOrEmpty()) {
                    Toast.makeText(context, "No URL found in clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    navController.popBackStack()
                    onPair(text)
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ModalCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = themedColor(
                    darkColor = Color(0xFF3A3A3A),
                    lightColor = Color(0xFFD0D0D0)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x_close),
            contentDescription = "Close",
            modifier = Modifier.size(20.dp),
            tint = themedColor(
                darkColor = Color(0xFFe3e7e7),
                lightColor = Color(0xFF202020)
            )
        )
    }
}

@Composable
private fun OptionCard(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFF3F3F3)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
            )
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = themedColor(
                darkColor = Color(0xFFe3e7e7),
                lightColor = Color(0xFF202020)
            )
        )
    }
}
