package com.reown.appkit.ui.components.internal.commons.inputs

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.reown.appkit.R
import com.reown.appkit.ui.components.internal.commons.ContentDescription
import com.reown.appkit.ui.previews.MultipleComponentsPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.AppKitTheme

@Composable
internal fun InputCancel(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val background: Color
    val tint: Color

    if (isEnabled) {
        tint = AppKitTheme.colors.background.color100
        background = AppKitTheme.colors.grayGlass20
    } else {
        tint = AppKitTheme.colors.background.color100
        background = AppKitTheme.colors.grayGlass10
    }

    Surface(
        color = background,
        modifier = Modifier.size(18.dp).clickable { onClick() }.then(modifier),
        shape = RoundedCornerShape(6.dp)
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_close),
            contentDescription = ContentDescription.CLEAR.description,
            modifier = Modifier.size(10.dp).padding(4.dp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@UiModePreview
@Composable
private fun PreviewInputCancel() {
    MultipleComponentsPreview(
        { InputCancel() {} },
        { InputCancel(isEnabled = false) {} },
    )
}
