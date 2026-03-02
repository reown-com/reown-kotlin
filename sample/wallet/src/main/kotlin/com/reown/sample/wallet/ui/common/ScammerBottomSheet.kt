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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.sample.common.ui.themedColor
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
                color = themedColor(darkColor = Color(0xFF1A1A1A), lightColor = Color.White),
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
            )
            .background(mismatch_color.copy(alpha = .15f))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Image(
            modifier = Modifier.size(72.dp),
            painter = painterResource(R.drawable.ic_scam),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Website flagged",
            style = TextStyle(
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = formatDomain(origin),
            style = TextStyle(color = Color(0xFFC9C9C9))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "The website you're trying to connect with is flagged as malicious by multiple security providers. Approving may lead to loss of funds.",
            style = TextStyle(color = Color.White, textAlign = TextAlign.Center)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Proceed anyway",
            modifier = Modifier.clickable { onProceed() },
            style = TextStyle(color = mismatch_color, fontSize = 16.sp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        CancelButton(
            modifier = Modifier
                .height(46.dp)
                .fillMaxWidth()
                .clickable { onClose() },
            backgroundColor = Color.White.copy(0.25f)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
