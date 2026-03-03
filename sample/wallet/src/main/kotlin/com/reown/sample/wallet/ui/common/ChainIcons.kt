@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.KhTekaFontFamily

@Composable
fun ChainIcons(
    chainIds: List<String>,
    size: Int = 24,
    maxVisible: Int = 5,
) {
    val uniqueChainIds = chainIds.distinct()
    val visibleChainIds = uniqueChainIds.take(maxVisible)
    val remainingCount = (uniqueChainIds.size - maxVisible).coerceAtLeast(0)

    val borderColor = themedColor(darkColor = Color(0xFF252525), lightColor = Color(0xFFF3F3F3))

    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleChainIds.forEach { chainId ->
            val icon = getChainIcon(chainId)
            Box(
                modifier = Modifier
                    .size((size + 4).dp)
                    .clip(CircleShape)
                    .background(
                        themedColor(
                            darkColor = Color(0xFF363636),
                            lightColor = Color(0xFFD0D0D0)
                        )
                    )
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(size.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val (color, label) = chainInfo(chainId)
                    Box(
                        modifier = Modifier
                            .size(size.dp)
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
        }
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .size((size + 4).dp)
                    .widthIn(min = 36.dp)
                    .clip(CircleShape)
                    .background(
                        themedColor(
                            darkColor = Color(0xFF363636),
                            lightColor = Color(0xFFD0D0D0)
                        )
                    )
                    .border(2.dp, borderColor, CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    style = TextStyle(
                        fontFamily = KhTekaFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(
                            darkColor = Color(0xFFe3e7e7),
                            lightColor = Color(0xFF202020)
                        )
                    )
                )
            }
        }
    }
}
