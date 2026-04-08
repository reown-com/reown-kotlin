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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScannerOptionsRoute(
    navController: NavController,
    onPair: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .background(
                color = WCTheme.colors.bgPrimary,
                shape = RoundedCornerShape(topStart = WCTheme.borderRadius.radius8, topEnd = WCTheme.borderRadius.radius8)
            )
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Close button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ModalCloseButton(onClick = { navController.popBackStack() })
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing5))

        // Scan QR code option
        OptionCard(
            label = "Scan QR code",
            iconRes = R.drawable.ic_scan_qr,
            onClick = {
                navController.popBackStack()
                navController.navigate(Route.ScanUri.path)
            }
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))

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

        // Test mode: manual URL input for Maestro E2E tests (enabled via ENABLE_TEST_MODE env var)
        if (BuildConfig.ENABLE_TEST_MODE) {
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))

            var urlText by remember { mutableStateOf("") }

            TextField(
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = {
                    Text(
                        text = "Paste URL here...",
                        style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textTertiary)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input-paste-url"),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = WCTheme.colors.foregroundPrimary,
                    textColor = WCTheme.colors.textPrimary,
                    cursorColor = WCTheme.colors.bgAccentPrimary,
                    focusedIndicatorColor = WCTheme.colors.bgAccentPrimary,
                    unfocusedIndicatorColor = WCTheme.colors.borderPrimary
                ),
                textStyle = WCTheme.typography.bodyLgRegular,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )

            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(WCTheme.borderRadius.shapeLarge)
                    .background(WCTheme.colors.bgAccentPrimary)
                    .clickable {
                        val text = urlText.trim()
                        if (text.isNotEmpty()) {
                            navController.popBackStack()
                            onPair(text)
                        }
                    }
                    .testTag("button-submit-url"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Submit URL",
                    style = WCTheme.typography.bodyLgRegular.copy(color = androidx.compose.ui.graphics.Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
fun ModalCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(WCTheme.borderRadius.shapeMedium)
            .border(
                width = 1.dp,
                color = WCTheme.colors.borderPrimary,
                shape = WCTheme.borderRadius.shapeMedium
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x_close),
            contentDescription = "Close",
            modifier = Modifier.size(20.dp),
            tint = WCTheme.colors.textPrimary
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
            .clip(WCTheme.borderRadius.shapeXLarge)
            .background(color = WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular.copy(
                color = WCTheme.colors.textPrimary
            )
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = WCTheme.colors.textPrimary
        )
    }
}
