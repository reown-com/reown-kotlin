package com.walletconnect.sample.pos.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.R

@Composable
fun PosHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WCTheme.spacing.spacing5, vertical = WCTheme.spacing.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = WCTheme.colors.textPrimary,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onBack)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_wcpay_logo),
                contentDescription = "WCPay",
                modifier = Modifier
                    .width(60.dp)
                    .height(18.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
            Image(
                painter = painterResource(R.drawable.ic_plus_header),
                contentDescription = "+",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
            Image(
                painter = painterResource(R.drawable.ic_ingenico_logo),
                contentDescription = "Ingenico",
                modifier = Modifier
                    .width(78.dp)
                    .height(22.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
