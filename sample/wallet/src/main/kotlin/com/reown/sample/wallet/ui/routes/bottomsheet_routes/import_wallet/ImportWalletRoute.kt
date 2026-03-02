@file:JvmSynthetic

package com.reown.sample.wallet.ui.routes.bottomsheet_routes.import_wallet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

@Composable
fun ImportWalletRoute(navController: NavController, onImportSuccess: () -> Unit = {}) {
    val viewModel: ImportWalletViewModel = viewModel()
    val selectedChain by viewModel.selectedChain.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val context = LocalContext.current
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    LaunchedEffect(importResult) {
        when (val result = importResult) {
            is ImportResult.Success -> {
                Toast.makeText(context, "${selectedChain.label} wallet imported: ${result.address}", Toast.LENGTH_SHORT).show()
                onImportSuccess()
                navController.popBackStack()
            }

            is ImportResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.clearResult()
            }

            null -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .clip(RoundedCornerShape(topStart = borderRadius.radius6, topEnd = borderRadius.radius6))
            .background(color = colors.bgPrimary)
            .padding(spacing.spacing5)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.spacing10)
                    .clip(RoundedCornerShape(borderRadius.radius3))
                    .border(
                        width = spacing.spacing05 / 2,
                        color = colors.borderSecondary,
                        shape = RoundedCornerShape(borderRadius.radius3)
                    )
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x_close),
                    contentDescription = "Close",
                    modifier = Modifier.size(spacing.spacing5),
                    tint = colors.iconInvert
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing4))

        Text(
            text = "Import Wallet",
            style = WCTheme.typography.h6Medium.copy(color = colors.textPrimary),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(spacing.spacing4))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
        ) {
            ImportWalletChain.entries.forEach { chain ->
                ChainTab(
                    label = chain.label,
                    isSelected = chain == selectedChain,
                    onClick = { viewModel.selectChain(chain) }
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing4))

        OutlinedTextField(
            value = inputText,
            onValueChange = { viewModel.updateInput(it) },
            placeholder = {
                Text(
                    text = selectedChain.placeholder,
                    style = WCTheme.typography.bodySmRegular.copy(color = colors.textSecondary)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.spacing2 * 15),
            textStyle = WCTheme.typography.bodySmRegular.copy(color = colors.textPrimary),
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

        Spacer(modifier = Modifier.height(spacing.spacing4))

        val importEnabled = !isLoading && inputText.isNotBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.spacing4, bottom = spacing.spacing3),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(spacing.spacing11)
                    .clip(RoundedCornerShape(borderRadius.radius4))
                    .background(colors.bgPrimary)
                    .border(
                        width = spacing.spacing05,
                        color = colors.borderSecondary,
                        shape = RoundedCornerShape(borderRadius.radius4)
                    )
                    .then(
                        if (!isLoading) Modifier.clickable { navController.popBackStack() } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = WCTheme.typography.bodyLgRegular.copy(color = colors.textPrimary)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(spacing.spacing11)
                    .clip(RoundedCornerShape(borderRadius.radius4))
                    .background(
                        if (importEnabled) colors.bgAccentPrimary else colors.foregroundAccentPrimary60
                    )
                    .then(
                        if (importEnabled) Modifier.clickable { viewModel.importWallet() } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(spacing.spacing5),
                        strokeWidth = spacing.spacing05,
                        color = colors.textInvert
                    )
                } else {
                    Text(
                        text = "Import",
                        style = WCTheme.typography.bodyLgRegular.copy(color = colors.textInvert)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChainTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(borderRadius.radius5))
            .background(
                color = if (isSelected) colors.bgAccentPrimary else colors.foregroundPrimary
            )
            .clickable { onClick() }
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodySmMedium.copy(
                color = if (isSelected) colors.textInvert else colors.textPrimary
            )
        )
    }
}
