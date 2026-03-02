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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.reown.sample.common.ui.theme.WCTheme

data class ChainItem(
    val chainId: String,
    val name: String,
    val namespace: String = "",
    val isRequired: Boolean = false,
)

@Composable
fun NetworkSelector(
    availableChains: List<ChainItem>,
    selectedChainIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius
    val accentColor = colors.bgAccentPrimary
    val rowHeight = spacing.spacing13 + spacing.spacing1
    val rowGap = spacing.spacing1 + spacing.spacing05
    val borderWidth = spacing.spacing05 / 2

    Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
        availableChains.forEach { chain ->
            val isSelected = selectedChainIds.contains(chain.chainId)
            val canToggle = !chain.isRequired || !isSelected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(borderRadius.shapeLarge)
                    .background(
                        if (isSelected) colors.foregroundAccentPrimary10
                        else colors.foregroundSecondary
                    )
                    .then(
                        if (isSelected) Modifier.border(borderWidth, colors.borderAccentPrimary, borderRadius.shapeLarge)
                        else Modifier
                    )
                    .then(
                        if (canToggle) {
                            Modifier.clickable {
                                if (isSelected) {
                                    onSelectionChange(selectedChainIds - chain.chainId)
                                } else {
                                    onSelectionChange(selectedChainIds + chain.chainId)
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = spacing.spacing5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
                ) {
                    ChainLogo(chainId = chain.chainId)
                    Text(
                        text = chain.name,
                        style = WCTheme.typography.bodyLgRegular.copy(
                            color = colors.textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Checkbox(checked = isSelected, locked = chain.isRequired && isSelected)
            }
        }
    }
}

@Composable
private fun ChainLogo(chainId: String) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val icon = getChainIcon(chainId)
    if (icon != null) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier
                .size(spacing.spacing8)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val (color, label) = chainInfo(chainId)
        Box(
            modifier = Modifier
                .size(spacing.spacing8)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = WCTheme.typography.bodySmMedium.copy(color = colors.textInvert)
            )
        }
    }
}

@Composable
private fun Checkbox(checked: Boolean, locked: Boolean = false) {
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing
    val borderRadius = WCTheme.borderRadius
    val accentColor = colors.bgAccentPrimary
    val lockedColor = accentColor.copy(alpha = 0.5f)
    val borderColor = colors.borderSecondary
    val borderWidth = spacing.spacing05 / 2
    val checkSize = spacing.spacing3 + spacing.spacing05
    val bgColor = if (locked) lockedColor else accentColor

    Box(
        modifier = Modifier
            .size(spacing.spacing6)
            .clip(borderRadius.shapeXSmall)
            .then(
                if (checked) {
                    Modifier
                        .background(bgColor)
                        .border(borderWidth, bgColor, borderRadius.shapeXSmall)
                } else {
                    Modifier.border(borderWidth, borderColor, borderRadius.shapeXSmall)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(checkSize),
                tint = colors.textInvert
            )
        }
    }
}
