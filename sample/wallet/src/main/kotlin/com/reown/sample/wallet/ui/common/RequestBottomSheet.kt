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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.material.CircularProgressIndicator
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.common.peer.PeerUI
import com.reown.sample.wallet.ui.routes.bottomsheet_routes.scanner_options.ModalCloseButton

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
                color = WCTheme.colors.bgPrimary,
                shape = RoundedCornerShape(topStart = WCTheme.borderRadius.radius8, topEnd = WCTheme.borderRadius.radius8)
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
            .padding(horizontal = WCTheme.spacing.spacing4, vertical = WCTheme.spacing.spacing4),
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
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing1))
            Box(
                modifier = Modifier
                    .clip(WCTheme.borderRadius.shapeXSmall)
                    .background(WCTheme.colors.bgAccentPrimary)
                    .padding(horizontal = WCTheme.spacing.spacing3, vertical = WCTheme.spacing.spacing1),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LINK MODE",
                    style = WCTheme.typography.bodySmMedium.copy(color = WCTheme.colors.textInvert)
                )
            }
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))

        // App icon
        val iconModifier = Modifier
            .size(WCTheme.spacing.spacing13)
            .clip(WCTheme.borderRadius.shapeLarge)
            .border(
                width = 1.dp,
                shape = WCTheme.borderRadius.shapeLarge,
                color = WCTheme.colors.borderPrimary
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

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))

        // Title: "{intention} {appName}"
        val appName = peerUI.peerName.takeIf { it.isNotBlank() } ?: "Unknown"
        Text(
            text = "$intention $appName",
            style = WCTheme.typography.h6Regular.copy(
                color = WCTheme.colors.textPrimary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = WCTheme.spacing.spacing1, bottom = WCTheme.spacing.spacing3)
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
            .padding(horizontal = WCTheme.spacing.spacing4)
            .padding(top = WCTheme.spacing.spacing4, bottom = WCTheme.spacing.spacing8),
        horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
    ) {
        val buttonsDisabled = isLoadingApprove || isLoadingReject

        // Cancel button (secondary - outlined)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(WCTheme.spacing.spacing11)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.bgPrimary)
                .border(
                    width = 1.dp,
                    color = WCTheme.colors.borderSecondary,
                    shape = WCTheme.borderRadius.shapeLarge
                )
                .then(
                    if (!buttonsDisabled) Modifier.clickable { onReject() } else Modifier
                )
                .testTag("wallet-request-reject"),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingReject) {
                CircularProgressIndicator(
                    modifier = Modifier.size(WCTheme.spacing.spacing5),
                    strokeWidth = WCTheme.spacing.spacing05,
                    color = WCTheme.colors.textPrimary
                )
            } else {
                Text(
                    text = rejectLabel,
                    style = WCTheme.typography.bodyLgRegular.copy(
                        color = WCTheme.colors.textPrimary
                    )
                )
            }
        }

        // Approve button (primary - filled)
        val accentColor = WCTheme.colors.bgAccentPrimary
        Box(
            modifier = Modifier
                .weight(1f)
                .height(WCTheme.spacing.spacing11)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(
                    if (approveEnabled) accentColor else accentColor.copy(alpha = 0.6f)
                )
                .then(
                    if (approveEnabled && !buttonsDisabled) {
                        Modifier.clickable { onApprove() }
                    } else {
                        Modifier
                    }
                )
                .testTag("wallet-request-approve"),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingApprove) {
                CircularProgressIndicator(
                    modifier = Modifier.size(WCTheme.spacing.spacing5),
                    strokeWidth = WCTheme.spacing.spacing05,
                    color = WCTheme.colors.textInvert
                )
            } else {
                Text(
                    text = approveLabel,
                    style = WCTheme.typography.bodyLgRegular.copy(
                        color = WCTheme.colors.textInvert
                    )
                )
            }
        }
    }
}
