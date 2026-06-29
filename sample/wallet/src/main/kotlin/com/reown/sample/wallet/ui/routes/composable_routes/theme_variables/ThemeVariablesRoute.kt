@file:JvmSynthetic

package com.reown.sample.wallet.ui.routes.composable_routes.theme_variables

import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.ThemeVariablesStore
import org.json.JSONObject

@Composable
fun ThemeVariablesRoute(navController: NavHostController) {
    val context = LocalContext.current
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    var input by remember { mutableStateOf(ThemeVariablesStore.themeVariables.value) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop)
                .padding(horizontal = spacing.spacing1, vertical = spacing.spacing2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = "Back",
                    tint = colors.iconDefault
                )
            }
            Text(
                text = "Pay form customization",
                style = WCTheme.typography.h6Medium.copy(color = colors.textPrimary)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3)
        ) {
            Text(
                text = "Paste the themeVariables value exported from the dashboard. Both " +
                    "\"themeVariables=<base64url>\" and the raw base64url value are accepted.",
                style = WCTheme.typography.bodyMdRegular.copy(color = colors.textSecondary)
            )

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = {
                    Text(
                        text = "themeVariables=eyJ...",
                        style = WCTheme.typography.bodyMdRegular.copy(color = colors.textSecondary)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.spacing2 * 15),
                textStyle = WCTheme.typography.bodyMdRegular.copy(color = colors.textPrimary),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = colors.foregroundPrimary,
                    focusedBorderColor = colors.borderAccentPrimary,
                    unfocusedBorderColor = colors.borderPrimary,
                    cursorColor = colors.iconAccentPrimary
                ),
                shape = RoundedCornerShape(borderRadius.radius3),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                maxLines = 6,
            )

            DecodedPreview(input = input)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
            ) {
                ActionButton(
                    text = "Clear",
                    textColor = colors.textPrimary,
                    backgroundColor = colors.bgPrimary,
                    bordered = true,
                    onClick = {
                        ThemeVariablesStore.clear()
                        input = ""
                        Toast.makeText(context, "Theme variables cleared", Toast.LENGTH_SHORT).show()
                    }
                )
                ActionButton(
                    text = "Save",
                    textColor = colors.textInvert,
                    backgroundColor = colors.bgAccentPrimary,
                    bordered = false,
                    onClick = {
                        ThemeVariablesStore.save(input)
                        Toast.makeText(context, "Theme variables saved", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))
        }
    }
}

@Composable
private fun RowScope.ActionButton(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    bordered: Boolean,
    onClick: () -> Unit,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Box(
        modifier = Modifier
            .weight(1f)
            .height(spacing.spacing11)
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(backgroundColor)
            .then(
                if (bordered) {
                    Modifier.border(
                        width = spacing.spacing05,
                        color = colors.borderSecondary,
                        shape = RoundedCornerShape(borderRadius.radius4)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = WCTheme.typography.bodyLgRegular.copy(color = textColor)
        )
    }
}

@Composable
private fun DecodedPreview(input: String) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    val decoded = remember(input) { decodeThemeVariables(input) }
    if (decoded == null && input.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius.radius4))
            .background(colors.foregroundSecondary)
            .padding(spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
    ) {
        Text(
            text = "Decoded",
            style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary)
        )
        Text(
            text = decoded ?: "Invalid themeVariables",
            style = WCTheme.typography.bodyMdRegular.copy(
                color = if (decoded != null) colors.textPrimary else colors.textSecondary
            )
        )
    }
}

private fun decodeThemeVariables(input: String): String? {
    val value = ThemeVariablesStore.parseInput(input)
    if (value.isBlank()) return null
    // Try URL-safe base64 first (the dashboard export format), then fall back to
    // standard base64. Decode bytes as UTF-8 explicitly to stay device-independent.
    val flags = listOf(
        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        Base64.DEFAULT
    )
    return flags.firstNotNullOfOrNull { flag ->
        runCatching {
            JSONObject(String(Base64.decode(value, flag), Charsets.UTF_8)).toString(2)
        }.getOrNull()
    }
}
