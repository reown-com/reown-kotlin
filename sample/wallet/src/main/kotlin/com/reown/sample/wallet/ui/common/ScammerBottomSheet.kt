@file:JvmSynthetic

package com.reown.sample.wallet.ui.common


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.common.ui.theme.mismatch_color
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.common.generated.CancelButton

@Composable
fun ScammerBottomSheet(
    origin: String,
    onProceed: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WCTheme.colors.bgPrimary,
                shape = RoundedCornerShape(topStart = WCTheme.borderRadius.radius8, topEnd = WCTheme.borderRadius.radius8)
            )
            .background(mismatch_color.copy(alpha = .15f))
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing3))
        Image(
            modifier = Modifier.size(72.dp),
            painter = painterResource(R.drawable.ic_scam),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
        Text(
            text = "Website flagged",
            style = WCTheme.typography.h5Medium.copy(color = WCTheme.colors.textInvert)
        )
        Text(
            text = formatDomain(origin),
            style = WCTheme.typography.bodyMdRegular.copy(color = WCTheme.colors.textSecondary)
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))
        Text(
            modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing4),
            text = "The website you're trying to connect with is flagged as malicious by multiple security providers. Approving may lead to loss of funds.",
            style = WCTheme.typography.bodyMdRegular.copy(color = WCTheme.colors.textInvert, textAlign = TextAlign.Center)
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))
        Text(
            text = "Proceed anyway",
            modifier = Modifier.clickable { onProceed() },
            style = WCTheme.typography.bodyLgRegular.copy(color = mismatch_color)
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
        CancelButton(
            modifier = Modifier
                .height(46.dp)
                .fillMaxWidth()
                .clickable { onClose() },
            backgroundColor = Color.White.copy(0.25f)
        )
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
    }
}
