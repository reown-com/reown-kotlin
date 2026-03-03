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
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

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
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(borderRadius.shapeLarge)
            .background(color = colors.foregroundPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!hideExpand) Modifier.clickable { onPress() } else Modifier)
                .padding(spacing.spacing5),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                headerContent()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
            ) {
                rightContent()
                if (!hideExpand) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_caret_up_down),
                        contentDescription = "Toggle",
                        modifier = Modifier.size(spacing.spacing5),
                        tint = colors.iconInvert
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
                    .padding(start = spacing.spacing5, end = spacing.spacing5, bottom = spacing.spacing5)
            ) {
                content()
            }
        }
    }
}
