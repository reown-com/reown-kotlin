package com.reown.appkit.ui.components.internal.commons.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.reown.appkit.R
import com.reown.appkit.ui.components.internal.commons.CopyIcon
import com.reown.appkit.ui.components.internal.commons.HorizontalSpacer
import com.reown.appkit.ui.components.internal.commons.button.ButtonSize
import com.reown.appkit.ui.components.internal.commons.button.LinkButton
import com.reown.appkit.ui.previews.MultipleComponentsPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.AppKitTheme

@Composable
internal fun ActionEntry(
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable() ((Color) -> Unit)? = null,
    isEnabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: () -> Unit
) {
    BaseEntry(
        isEnabled = isEnabled,
        contentPadding = contentPadding
    ) { colors ->
        Row(
            modifier = modifier
                .clickable { onClick() }
                .background(colors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                it(colors.textColor)
                HorizontalSpacer(width = 8.dp)
            }
            Text(text = text, style = AppKitTheme.typo.paragraph500.copy(color = colors.textColor))
        }
    }
}

@Composable
internal fun CopyActionEntry(isEnabled: Boolean = true, onClick: () -> Unit) {
    LinkButton(
        text = "Copy link",
        startIcon = {
            CopyIcon(tint = it, modifier = Modifier.size(12.dp))
        },
        isEnabled = isEnabled,
        size = ButtonSize.S,
        onClick = onClick
    )
}

@UiModePreview
@Composable
private fun ActionEntryPreview() {
    MultipleComponentsPreview(
        { ActionEntry(text = "Action without icon") {} },
        { ActionEntry(text = "Action without icon", isEnabled = false) {} },
        { ActionEntry(icon = { Image(imageVector = ImageVector.vectorResource(R.drawable.ic_check), contentDescription = null, colorFilter = ColorFilter.tint(it)) }, text = "Action with icon") {} },
        { ActionEntry(isEnabled = false, icon = { Image(imageVector = ImageVector.vectorResource(R.drawable.ic_check), contentDescription = null, colorFilter = ColorFilter.tint(it)) }, text = "Action with icon") {} },
    )
}

@UiModePreview
@Composable
private fun CopyActionPreview() {
    MultipleComponentsPreview(
        { CopyActionEntry {} },
        { CopyActionEntry(isEnabled = false) {} }
    )
}
