package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.BuildConfig
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.model.Currency
import kotlinx.coroutines.launch

private val CaretUpDown: ImageVector by lazy {
    ImageVector.Builder(
        name = "CaretUpDown",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // Down caret
            moveTo(14.4137f, 13.0875f)
            curveTo(14.5011f, 13.1746f, 14.5705f, 13.278f, 14.6178f, 13.392f)
            curveTo(14.6651f, 13.506f, 14.6895f, 13.6281f, 14.6895f, 13.7515f)
            curveTo(14.6895f, 13.8749f, 14.6651f, 13.9971f, 14.6178f, 14.111f)
            curveTo(14.5705f, 14.225f, 14.5011f, 14.3285f, 14.4137f, 14.4156f)
            lineTo(10.6637f, 18.1656f)
            curveTo(10.5766f, 18.253f, 10.4731f, 18.3223f, 10.3592f, 18.3696f)
            curveTo(10.2452f, 18.417f, 10.1231f, 18.4413f, 9.9997f, 18.4413f)
            curveTo(9.8763f, 18.4413f, 9.7541f, 18.417f, 9.6402f, 18.3696f)
            curveTo(9.5262f, 18.3223f, 9.4227f, 18.253f, 9.3356f, 18.1656f)
            lineTo(5.5856f, 14.4156f)
            curveTo(5.4095f, 14.2395f, 5.3106f, 14.0006f, 5.3106f, 13.7515f)
            curveTo(5.3106f, 13.5024f, 5.4095f, 13.2636f, 5.5856f, 13.0875f)
            curveTo(5.7617f, 12.9113f, 6.0006f, 12.8124f, 6.2497f, 12.8124f)
            curveTo(6.4987f, 12.8124f, 6.7376f, 12.9113f, 6.9137f, 13.0875f)
            lineTo(10.0005f, 16.1726f)
            lineTo(13.0872f, 13.0851f)
            curveTo(13.1744f, 12.9981f, 13.2779f, 12.9291f, 13.3918f, 12.8822f)
            curveTo(13.5057f, 12.8352f, 13.6277f, 12.8111f, 13.7509f, 12.8113f)
            curveTo(13.8741f, 12.8115f, 13.9961f, 12.836f, 14.1098f, 12.8834f)
            curveTo(14.2235f, 12.9308f, 14.3268f, 13.0001f, 14.4137f, 13.0875f)
            close()
            // Up caret
            moveTo(6.9137f, 6.9156f)
            lineTo(10.0005f, 3.8289f)
            lineTo(13.0872f, 6.9164f)
            curveTo(13.2633f, 7.0925f, 13.5022f, 7.1914f, 13.7512f, 7.1914f)
            curveTo(14.0003f, 7.1914f, 14.2392f, 7.0925f, 14.4153f, 6.9164f)
            curveTo(14.5914f, 6.7402f, 14.6904f, 6.5014f, 14.6904f, 6.2523f)
            curveTo(14.6904f, 6.0032f, 14.5914f, 5.7644f, 14.4153f, 5.5882f)
            lineTo(10.6653f, 1.8382f)
            curveTo(10.5782f, 1.7508f, 10.4747f, 1.6815f, 10.3608f, 1.6342f)
            curveTo(10.2468f, 1.5869f, 10.1246f, 1.5625f, 10.0012f, 1.5625f)
            curveTo(9.8779f, 1.5625f, 9.7557f, 1.5869f, 9.6417f, 1.6342f)
            curveTo(9.5278f, 1.6815f, 9.4243f, 1.7508f, 9.3372f, 1.8382f)
            lineTo(5.5872f, 5.5882f)
            curveTo(5.4111f, 5.7644f, 5.3121f, 6.0032f, 5.3121f, 6.2523f)
            curveTo(5.3121f, 6.5014f, 5.4111f, 6.7402f, 5.5872f, 6.9164f)
            curveTo(5.7633f, 7.0925f, 6.0022f, 7.1914f, 6.2512f, 7.1914f)
            curveTo(6.5003f, 7.1914f, 6.7392f, 7.0925f, 6.9153f, 6.9164f)
            lineTo(6.9137f, 6.9156f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
    viewModel: POSViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = WCTheme.spacing.spacing8, topEnd = WCTheme.spacing.spacing8),
        sheetBackgroundColor = WCTheme.colors.bgPrimary,
        sheetElevation = 4.dp,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetContent = {
            CurrencyBottomSheet(
                selectedCurrency = selectedCurrency,
                onSelect = { currency ->
                    viewModel.setCurrency(currency)
                    scope.launch { sheetState.hide() }
                },
                onDismiss = { scope.launch { sheetState.hide() } }
            )
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

            // Currency setting
            SettingsItem(
                label = "Currency",
                value = "${selectedCurrency.displayName} (${selectedCurrency.symbol})",
                showCaret = true,
                onClick = { scope.launch { sheetState.show() } },
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
        // Header: title centered, X button on right
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Currency",
                style = WCTheme.typography.h6Regular,
                color = WCTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(WCTheme.spacing.spacing3))
                    .border(
                        width = 1.dp,
                        color = WCTheme.colors.borderSecondary,
                        shape = RoundedCornerShape(WCTheme.spacing.spacing3)
                    )
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = WCTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        // Currency options
        Currency.entries.forEach { currency ->
            val isSelected = currency == selectedCurrency
            CurrencyOptionItem(
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
private fun CurrencyOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(WCTheme.borderRadius.radius4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.dp, WCTheme.colors.borderAccentPrimary, shape)
                        .background(WCTheme.colors.foregroundAccentPrimary10, shape)
                } else {
                    Modifier.background(WCTheme.colors.foregroundPrimary, shape)
                }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            // Radio button: blue circle border with filled inner circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.dp, WCTheme.colors.iconAccentPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(WCTheme.colors.iconAccentPrimary, CircleShape)
                )
            }
        }
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
                imageVector = CaretUpDown,
                contentDescription = null,
                tint = WCTheme.colors.iconInvert,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
