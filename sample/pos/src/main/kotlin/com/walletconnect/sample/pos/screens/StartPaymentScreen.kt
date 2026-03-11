package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.components.BrandLogoRow

@Composable
fun StartPaymentScreen(
    viewModel: POSViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Logos row at top
        BrandLogoRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WCTheme.spacing.spacing5)
        )

        Spacer(Modifier.height(WCTheme.spacing.spacing5))

        // 3 action buttons filling remaining space
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing3)
        ) {
            ActionButton(
                iconRes = R.drawable.ic_plus,
                label = "New sale",
                onClick = { viewModel.navigateToAmountScreen() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            ActionButton(
                iconRes = R.drawable.ic_clock,
                label = "Activity",
                onClick = { viewModel.navigateToTransactionHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            ActionButton(
                iconRes = R.drawable.ic_settings,
                label = "Settings",
                onClick = { viewModel.navigateToSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(WCTheme.colors.foregroundPrimary)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = WCTheme.colors.iconDefault,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(WCTheme.spacing.spacing2))
        Text(
            text = label,
            style = WCTheme.typography.bodyXlRegular,
            color = WCTheme.colors.textPrimary
        )
    }
}
