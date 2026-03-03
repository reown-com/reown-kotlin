@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.material.CircularProgressIndicator
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scanner_options.ModalCloseButton

private val accentBlue = Color(0xFF0988F0)

@Composable
fun RequestBottomSheet(
    peerUI: PeerUI,
    intention: String,
    isLinkMode: Boolean = false,
    approveLabel: String = "Connect",
    rejectLabel: String = "Cancel",
    approveEnabled: Boolean = true,
    isLoadingApprove: Boolean = false,
    isLoadingReject: Boolean = false,
    onApprove: () -> Unit = {},
    onReject: () -> Unit = {},
    onClose: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = themedColor(
                    darkColor = Color(0xFF1A1A1A),
                    lightColor = Color.White
                ),
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
            )
    ) {
        // Header
        ModalHeader(
            peerUI = peerUI,
            intention = intention,
            isLinkMode = isLinkMode,
            onClose = onClose
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }

        // Footer
        ModalFooter(
            approveLabel = approveLabel,
            rejectLabel = rejectLabel,
            approveEnabled = approveEnabled,
            isLoadingApprove = isLoadingApprove,
            isLoadingReject = isLoadingReject,
            onApprove = onApprove,
            onReject = onReject
        )
    }
}

@Composable
private fun ModalHeader(
    peerUI: PeerUI,
    intention: String,
    isLinkMode: Boolean,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button (top-right)
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                ModalCloseButton(onClick = onClose)
            }
        }

        // Link mode badge
        if (isLinkMode) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentBlue)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LINK MODE",
                    style = WCTheme.typography.bodySmMedium.copy(color = Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App icon
        val iconModifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(16.dp),
                color = themedColor(
                    darkColor = Color(0xFF3A3A3A),
                    lightColor = Color(0xFFD0D0D0)
                )
            )

        if (peerUI.peerIcon.isNotBlank() && peerUI.peerIcon != "null") {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(peerUI.peerIcon)
                    .size(64)
                    .crossfade(true)
                    .error(com.reown.sample.common.R.drawable.ic_walletconnect_circle_blue)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = "App icon",
                modifier = iconModifier,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        } else {
            Image(
                modifier = iconModifier.alpha(.7f),
                imageVector = ImageVector.vectorResource(id = R.drawable.sad_face),
                contentDescription = "No icon"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title: "{intention} {appName}"
        val appName = peerUI.peerName.takeIf { it.isNotBlank() } ?: "Unknown"
        Text(
            text = "$intention $appName",
            style = WCTheme.typography.h6Regular.copy(
                color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun ModalFooter(
    approveLabel: String,
    rejectLabel: String,
    approveEnabled: Boolean,
    isLoadingApprove: Boolean,
    isLoadingReject: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val buttonsDisabled = isLoadingApprove || isLoadingReject

        // Cancel button (secondary - outlined)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    themedColor(
                        darkColor = Color(0xFF202020),
                        lightColor = Color.White
                    )
                )
                .border(
                    width = 1.dp,
                    color = themedColor(
                        darkColor = Color(0xFF4F4F4F),
                        lightColor = Color(0xFFD0D0D0)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .then(
                    if (!buttonsDisabled) Modifier.clickable { onReject() } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingReject) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = themedColor(darkColor = Color.White, lightColor = Color.Black)
                )
            } else {
                Text(
                    text = rejectLabel,
                    style = WCTheme.typography.bodyLgRegular.copy(
                        color = themedColor(darkColor = 0xFFFFFFFF, lightColor = 0xFF202020)
                    )
                )
            }
        }

        // Approve button (primary - filled)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (approveEnabled) accentBlue else accentBlue.copy(alpha = 0.6f)
                )
                .then(
                    if (approveEnabled && !buttonsDisabled) {
                        Modifier.clickable { onApprove() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingApprove) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = approveLabel,
                    style = WCTheme.typography.bodyLgRegular.copy(
                        color = Color.White
                    )
                )
            }
        }
    }
}
