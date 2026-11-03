package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

private const val PARAGRAPH_LINE_HEIGHT = 1.45f
private const val CARD_ASPECT_RATIO = 1.586f
private const val DISABLED_ALPHA = 0.4f

/** WCTypography styles have line height == font size; give multi-line copy room to breathe. */
internal fun TextStyle.paragraph(): TextStyle = copy(lineHeight = fontSize * PARAGRAPH_LINE_HEIGHT)

@Composable
internal fun VisaPayTitle(text: String) {
    Text(text = text, style = WCTheme.typography.h5Regular.copy(color = WCTheme.colors.textPrimary))
}

@Composable
internal fun VisaPayBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = WCTheme.typography.bodyLgRegular.paragraph().copy(color = WCTheme.colors.textTertiary)
    )
}

@Composable
internal fun VisaPayLabel(text: String) {
    Text(text = text, style = WCTheme.typography.bodyMdMedium.copy(color = WCTheme.colors.textPrimary))
}

@Composable
internal fun VisaPayPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = WCTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WCTheme.spacing.spacing12)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(colors.bgAccentPrimary)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textInvert))
    }
}

@Composable
internal fun VisaPayTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(WCTheme.borderRadius.shapeFull)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing3, vertical = WCTheme.spacing.spacing3),
        style = WCTheme.typography.bodyMdMedium.copy(color = WCTheme.colors.textAccentPrimary)
    )
}

@Composable
internal fun VisaPayTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    monospace: Boolean = false,
    isError: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = WCTheme.colors
    val baseStyle = WCTheme.typography.bodyLgRegular
    val textStyle = if (monospace) baseStyle.copy(fontFamily = FontFamily.Monospace) else baseStyle
    Column(verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)) {
        VisaPayLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            placeholder = { Text(text = placeholder, style = textStyle.copy(color = colors.textSecondary)) },
            trailingIcon = trailing,
            textStyle = textStyle.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = WCTheme.borderRadius.shapeMedium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = colors.bgPrimary,
                focusedBorderColor = colors.borderAccentPrimary,
                unfocusedBorderColor = colors.borderSecondary,
                errorBorderColor = colors.borderError,
                cursorColor = colors.iconAccentPrimary
            )
        )
    }
}

@Composable
internal fun VisaPaySelectField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    val colors = WCTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)) {
        VisaPayLabel(label)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WCTheme.spacing.spacing13 - WCTheme.spacing.spacing2)
                    .clip(WCTheme.borderRadius.shapeMedium)
                    .background(colors.bgPrimary)
                    .border(1.dp, colors.borderSecondary, WCTheme.borderRadius.shapeMedium)
                    .clickable { expanded = true }
                    .padding(horizontal = WCTheme.spacing.spacing4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { "Select a country" },
                    modifier = Modifier.weight(1f),
                    style = WCTheme.typography.bodyLgRegular.copy(
                        color = if (value.isBlank()) colors.textSecondary else colors.textPrimary
                    )
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.iconDefault,
                    modifier = Modifier.size(WCTheme.spacing.spacing4)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        onSelect(option)
                        expanded = false
                    }) {
                        Text(text = option, style = WCTheme.typography.bodyLgRegular.copy(color = colors.textPrimary))
                    }
                }
            }
        }
    }
}

/** Single-choice pill row used for document type, daily limit presets and network. */
@Composable
internal fun <T> VisaPayPillRow(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)) {
        items.forEach { item ->
            VisaPayPill(text = label(item), isSelected = item == selected, onClick = { onSelect(item) })
        }
    }
}

@Composable
private fun RowScope.VisaPayPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = WCTheme.colors
    Box(
        modifier = Modifier
            .weight(1f)
            .height(WCTheme.spacing.spacing11 - WCTheme.spacing.spacing1)
            .clip(WCTheme.borderRadius.shapeFull)
            .background(if (isSelected) colors.foregroundAccentPrimary10 else colors.bgPrimary)
            .border(
                1.dp,
                if (isSelected) colors.borderAccentPrimary else colors.borderSecondary,
                WCTheme.borderRadius.shapeFull
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = WCTheme.typography.bodyMdMedium.copy(
                color = if (isSelected) colors.textAccentPrimary else colors.textPrimary
            )
        )
    }
}

@Composable
internal fun VisaPaySurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(WCTheme.colors.foregroundPrimary)
            .border(1.dp, WCTheme.colors.borderPrimary, WCTheme.borderRadius.shapeLarge)
            .padding(horizontal = WCTheme.spacing.spacing4),
        content = content
    )
}

@Composable
internal fun VisaPayIconTile(@DrawableRes icon: Int, modifier: Modifier = Modifier, circle: Boolean = false) {
    Box(
        modifier = modifier
            .size(WCTheme.spacing.spacing11 - WCTheme.spacing.spacing1)
            .clip(if (circle) CircleShape else WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundAccentPrimary10Solid),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(WCTheme.spacing.spacing6 - WCTheme.spacing.spacing05),
            colorFilter = ColorFilter.tint(WCTheme.colors.iconAccentPrimary)
        )
    }
}

@Composable
internal fun VisaPayCheckIcon(color: Color = WCTheme.colors.iconSuccess) {
    Icon(
        painter = painterResource(R.drawable.ic_check),
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(WCTheme.spacing.spacing4 + WCTheme.spacing.spacing05)
    )
}

enum class VisaPayBadgeTone { ACCENT, SUCCESS, NEUTRAL }

@Composable
internal fun VisaPayBadge(text: String, tone: VisaPayBadgeTone) {
    val colors = WCTheme.colors
    val (background, foreground) = when (tone) {
        VisaPayBadgeTone.ACCENT -> colors.foregroundAccentPrimary10 to colors.textAccentPrimary
        VisaPayBadgeTone.SUCCESS -> colors.bgSuccess to colors.textSuccess
        VisaPayBadgeTone.NEUTRAL -> colors.foregroundSecondary to colors.textTertiary
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(WCTheme.borderRadius.shapeFull)
            .background(background)
            .padding(horizontal = WCTheme.spacing.spacing3, vertical = WCTheme.spacing.spacing1),
        style = WCTheme.typography.bodySmMedium.copy(color = foreground)
    )
}

/** The physical card visual shown on the approved and card home screens. */
@Composable
internal fun VisaPayCardVisual(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WCTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(colors.bgInvert)
            .padding(WCTheme.spacing.spacing5),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Sample Wallet",
                modifier = Modifier.weight(1f),
                style = WCTheme.typography.bodyLgMedium.copy(color = colors.textInvert)
            )
            Image(
                painter = painterResource(R.drawable.ic_walletconnect_brandmark),
                contentDescription = null,
                modifier = Modifier.size(width = WCTheme.spacing.spacing8, height = WCTheme.spacing.spacing5),
                colorFilter = ColorFilter.tint(colors.textInvert)
            )
        }
        content()
    }
}

@Composable
internal fun VisaPayCardFooter(left: String, right: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = left,
            modifier = Modifier.weight(1f),
            style = WCTheme.typography.bodyMdRegular.copy(color = WCTheme.colors.textInvert.copy(alpha = 0.8f))
        )
        Text(text = right, style = WCTheme.typography.h6Medium.copy(color = WCTheme.colors.textInvert))
    }
}

/** Three bouncing dots, matching the "In review" animation in the mockup. */
@Composable
internal fun VisaPayReviewDots() {
    val transition = rememberInfiniteTransition(label = "visapay_review_dots")
    val dotSize = WCTheme.spacing.spacing2 + WCTheme.spacing.spacing05
    Box(
        modifier = Modifier
            .size(WCTheme.spacing.spacing13 + WCTheme.spacing.spacing8)
            .clip(CircleShape)
            .background(WCTheme.colors.foregroundAccentPrimary10Solid),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)) {
            repeat(3) { index ->
                val offset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 140)
                    ),
                    label = "dot_$index"
                )
                Box(
                    modifier = Modifier
                        .offset(y = offset.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(WCTheme.colors.bgAccentPrimary)
                )
            }
        }
    }
}

@Composable
internal fun VisaPayDashedAction(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = WCTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(colors.bgPrimary)
            .dashedBorder(colors.borderSecondary, WCTheme.borderRadius.radius4)
            .clickable(onClick = onClick)
            .padding(WCTheme.spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing3)
    ) {
        VisaPayIconTile(icon)
        Column(verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing05)) {
            Text(text = title, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            Text(text = subtitle, style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
        }
    }
}

private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        )
    )
}

internal enum class TimelineState { DONE, ACTIVE, TODO }

@Composable
internal fun TimelineRow(timelineState: TimelineState, label: String, subtitle: String) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val dotSize = spacing.spacing6
    Row(
        modifier = Modifier.padding(vertical = spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
    ) {
        when (timelineState) {
            TimelineState.DONE -> Box(
                modifier = Modifier.size(dotSize).clip(CircleShape).background(colors.bgSuccess),
                contentAlignment = Alignment.Center
            ) { VisaPayCheckIcon(colors.iconSuccess) }

            TimelineState.ACTIVE -> Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(colors.foregroundAccentPrimary10)
                    .padding(spacing.spacing2)
                    .clip(CircleShape)
                    .background(colors.bgAccentPrimary)
            )

            TimelineState.TODO -> Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(colors.foregroundSecondary)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing05)) {
            Text(text = label, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            Text(text = subtitle, style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
        }
    }
}
