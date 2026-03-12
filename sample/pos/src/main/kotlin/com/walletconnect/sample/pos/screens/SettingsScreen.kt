package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.BuildConfig
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.components.BottomSheetHeader
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.components.SelectableOptionItem
import com.walletconnect.sample.pos.model.Currency
import com.walletconnect.sample.pos.model.ThemeMode
import kotlinx.coroutines.launch

private enum class ActiveSheet { THEME, CURRENCY }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
    viewModel: POSViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val selectedThemeMode by viewModel.selectedThemeMode.collectAsState()
    val printReceiptEnabled by viewModel.printReceiptEnabled.collectAsState()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    var activeSheet by remember { mutableStateOf(ActiveSheet.CURRENCY) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = WCTheme.spacing.spacing8, topEnd = WCTheme.spacing.spacing8),
        sheetBackgroundColor = WCTheme.colors.bgPrimary,
        sheetElevation = 4.dp,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetContent = {
            when (activeSheet) {
                ActiveSheet.THEME -> ThemeBottomSheet(
                    selectedThemeMode = selectedThemeMode,
                    onSelect = { mode ->
                        viewModel.setThemeMode(mode)
                        scope.launch { sheetState.hide() }
                    },
                    onDismiss = { scope.launch { sheetState.hide() } }
                )
                ActiveSheet.CURRENCY -> CurrencyBottomSheet(
                    selectedCurrency = selectedCurrency,
                    onSelect = { currency ->
                        viewModel.setCurrency(currency)
                        scope.launch { sheetState.hide() }
                    },
                    onDismiss = { scope.launch { sheetState.hide() } }
                )
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WCTheme.colors.bgPrimary)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PosHeader(onBack = onClose)

            Spacer(Modifier.height(WCTheme.spacing.spacing3))

            // Theme setting
            SettingsItem(
                label = "Theme",
                value = selectedThemeMode.displayName,
                showCaret = true,
                onClick = {
                    activeSheet = ActiveSheet.THEME
                    scope.launch { sheetState.show() }
                },
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // Currency setting
            SettingsItem(
                label = "Currency",
                value = "${selectedCurrency.displayName} (${selectedCurrency.symbol})",
                showCaret = true,
                onClick = {
                    activeSheet = ActiveSheet.CURRENCY
                    scope.launch { sheetState.show() }
                },
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // Print receipt toggle
            SettingsToggleItem(
                label = "Print Receipt",
                checked = printReceiptEnabled,
                onCheckedChange = { viewModel.setPrintReceiptEnabled(it) },
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // SDK Version
            SettingsItem(
                label = "SDK Version",
                value = BuildConfig.BOM_VERSION,
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.weight(1f))

            CloseButton(onClick = onClose)

            Spacer(Modifier.height(WCTheme.spacing.spacing5))
        }
    }
}

@Composable
private fun ThemeBottomSheet(
    selectedThemeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        BottomSheetHeader(title = "Theme", onDismiss = onDismiss)

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selectedThemeMode
            val iconRes = when (mode) {
                ThemeMode.SYSTEM -> R.drawable.ic_device_mobile
                ThemeMode.LIGHT -> R.drawable.ic_sun
                ThemeMode.DARK -> R.drawable.ic_moon
            }
            SelectableOptionItem(
                label = mode.displayName,
                isSelected = isSelected,
                onClick = { onSelect(mode) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = if (isSelected) WCTheme.colors.iconAccentPrimary else WCTheme.colors.iconDefault,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            if (mode != ThemeMode.entries.last()) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun CurrencyBottomSheet(
    selectedCurrency: Currency,
    onSelect: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        BottomSheetHeader(title = "Currency", onDismiss = onDismiss)

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        Currency.entries.forEach { currency ->
            val isSelected = currency == selectedCurrency
            SelectableOptionItem(
                label = "${currency.displayName} (${currency.symbol})",
                isSelected = isSelected,
                onClick = { onSelect(currency) }
            )
            if (currency != Currency.entries.last()) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String,
    showCaret: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundPrimary)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary
        )
        Spacer(Modifier.width(WCTheme.spacing.spacing2))
        Text(
            text = value,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textTertiary,
            modifier = Modifier.weight(1f)
        )
        if (showCaret) {
            Icon(
                painter = painterResource(R.drawable.ic_caret_up_down),
                contentDescription = null,
                tint = WCTheme.colors.iconInvert,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundPrimary)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WCTheme.colors.iconAccentPrimary,
                checkedTrackColor = WCTheme.colors.iconAccentPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = WCTheme.colors.iconDefault,
                uncheckedTrackColor = WCTheme.colors.iconDefault.copy(alpha = 0.3f)
            )
        )
    }
}
