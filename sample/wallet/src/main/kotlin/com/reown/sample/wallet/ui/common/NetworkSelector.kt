@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.KhTekaFontFamily
import com.reown.sample.common.ui.theme.WCTheme

data class ChainItem(
    val chainId: String,
    val name: String,
    val namespace: String = "",
)

@Composable
fun NetworkSelector(
    availableChains: List<ChainItem>,
    selectedChainIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        availableChains.forEach { chain ->
            val isSelected = selectedChainIds.contains(chain.chainId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isSelected) {
                            onSelectionChange(selectedChainIds - chain.chainId)
                        } else {
                            onSelectionChange(selectedChainIds + chain.chainId)
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChainLogo(chainId = chain.chainId)
                    Text(
                        text = chain.name,
                        style = WCTheme.typography.bodyMdRegular.copy(
                            color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Checkbox(checked = isSelected)
            }
        }
    }
}

@Composable
private fun ChainLogo(chainId: String) {
    val icon = getChainIcon(chainId)
    if (icon != null) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val (color, label) = chainInfo(chainId)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = KhTekaFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun Checkbox(checked: Boolean) {
    val accentColor = Color(0xFF0988F0)
    val borderColor = themedColor(darkColor = Color(0xFF4F4F4F), lightColor = Color(0xFFD0D0D0))

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (checked) {
                    Modifier
                        .background(accentColor)
                        .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
        }
    }
}
