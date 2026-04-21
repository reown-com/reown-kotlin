package com.walletconnect.sample.pos.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.ui.theme.WCTheme
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.model.LocalPosVariant

@Composable
fun BrandLogoRow(
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter = ColorFilter.tint(WCTheme.colors.textPrimary)
) {
    val variant = LocalPosVariant.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_wcpay_logo),
            contentDescription = "WCPay",
            modifier = Modifier
                .width(60.dp)
                .height(18.dp),
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter
        )
        variant.partnerLogoRes?.let { logoRes ->
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
            Image(
                painter = painterResource(R.drawable.ic_plus_header),
                contentDescription = "+",
                modifier = Modifier.size(20.dp),
                colorFilter = colorFilter
            )
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
            Image(
                painter = painterResource(logoRes),
                contentDescription = variant.displayName,
                modifier = Modifier
                    .width(variant.partnerLogoWidthDp.dp)
                    .height(variant.partnerLogoHeightDp.dp),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter
            )
        }
    }
}
