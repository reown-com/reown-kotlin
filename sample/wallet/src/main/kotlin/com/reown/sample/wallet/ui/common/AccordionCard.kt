@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.themedColor

private const val ANIMATION_DURATION = 250

@Composable
fun AccordionCard(
    headerContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit = {},
    isExpanded: Boolean,
    onPress: () -> Unit,
    hideExpand: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = themedColor(
                    darkColor = Color(0xFF252525),
                    lightColor = Color(0xFFF3F3F3)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!hideExpand) Modifier.clickable { onPress() } else Modifier)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                headerContent()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rightContent()
                if (!hideExpand) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        modifier = Modifier
                            .size(17.dp)
                            .rotate(if (isExpanded) 180f else 0f),
                        tint = themedColor(
                            darkColor = Color(0xFFe3e7e7),
                            lightColor = Color(0xFF202020)
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(ANIMATION_DURATION)),
            exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                content()
            }
        }
    }
}
