package com.reown.appkit.ui.routes.connect.scan_code

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.android.pulse.model.ConnectionMethod
import com.reown.modal.ui.components.qr.QrCodeType
import com.reown.modal.ui.components.qr.WalletConnectQRCode
import com.reown.appkit.client.Modal
import com.reown.appkit.domain.delegate.AppKitDelegate
import com.reown.appkit.ui.components.internal.OrientationBox
import com.reown.appkit.ui.components.internal.commons.entry.CopyActionEntry
import com.reown.appkit.ui.components.internal.snackbar.LocalSnackBarHandler
import com.reown.appkit.ui.previews.Landscape
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.previews.AppKitPreview
import com.reown.appkit.ui.routes.connect.ConnectViewModel
import com.reown.appkit.ui.theme.AppKitTheme
import kotlinx.coroutines.flow.filter

@Composable
internal fun ScanQRCodeRoute(connectViewModel: ConnectViewModel) {
    val snackBarHandler = LocalSnackBarHandler.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var uri by remember { mutableStateOf(connectViewModel.pairingUri) }

    LaunchedEffect(Unit) {
        AppKitDelegate
            .wcEventModels
            .filter { event -> event is Modal.Model.RejectedSession || event is Modal.Model.SessionAuthenticateResponse.Error }
            .collect {
                snackBarHandler.showErrorSnack("Declined")
                connectViewModel.connectWalletConnect(name = "WalletConnect", method = ConnectionMethod.QR_CODE, linkMode = null) { newUri -> uri = newUri }
            }
    }

    ScanQRCodeContent(
        uri = uri,
        onCopyLinkClick = {
            snackBarHandler.showSuccessSnack("Link copied")
            clipboardManager.setText(AnnotatedString(connectViewModel.pairingUri))
        }
    )
}

@Composable
private fun ScanQRCodeContent(
    uri: String, onCopyLinkClick: () -> Unit
) {
    OrientationBox(
        portrait = { PortraitContent(uri, onCopyLinkClick) },
        landscape = { LandscapeContent(uri, onCopyLinkClick) }
    )

}

@Composable
private fun LandscapeContent(
    uri: String,
    onCopyLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 5.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            QRCode(uri = uri)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScanQrCodeLabel()
            Spacer(modifier = Modifier.height(12.dp))
            CopyActionEntry(onClick = onCopyLinkClick)
        }
    }
}

@Composable
private fun PortraitContent(
    uri: String,
    onCopyLinkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QRCode(uri = uri)
        Spacer(modifier = Modifier.height(20.dp))
        ScanQrCodeLabel()
        Spacer(modifier = Modifier.height(12.dp))
        CopyActionEntry(onClick = onCopyLinkClick)
    }
}

@Composable
private fun ScanQrCodeLabel() {
    Text(
        text = "Scan this QR code with your phone",
        modifier = Modifier.fillMaxWidth(), style = AppKitTheme.typo.paragraph400, textAlign = TextAlign.Center
    )
}

@Composable
private fun QRCode(uri: String) {
    if (isSystemInDarkTheme()) {
        Box(
            modifier = Modifier
                .background(AppKitTheme.colors.inverse100, shape = RoundedCornerShape(36.dp))
                .padding(16.dp)
        ) {
            WalletConnectQRCode(
                qrData = uri,
                primaryColor = AppKitTheme.colors.inverse000,
                logoColor = AppKitTheme.colors.accent100,
                type = QrCodeType.W3M
            )
        }
    } else {
        WalletConnectQRCode(
            qrData = uri,
            primaryColor = AppKitTheme.colors.inverse000,
            logoColor = AppKitTheme.colors.accent100,
            type = QrCodeType.W3M
        )
    }
}

@UiModePreview
@Landscape
@Composable
private fun ScanQRCodePreview() {
    AppKitPreview("Mobile Wallets") {
        ScanQRCodeContent("47442c19ea7c6a7a836fa3e53af1ddd375438daaeea9acdbf595e989a731b73249a10a7cc0e343ca627e536609", {})
    }
}