package com.reown.appkit.ui.components.button

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reown.appkit.ui.components.internal.commons.LoadingSpinner
import com.reown.appkit.ui.components.internal.commons.button.ButtonSize
import com.reown.appkit.ui.components.internal.commons.button.ButtonStyle
import com.reown.appkit.ui.components.internal.commons.button.StyledButton
import com.reown.appkit.ui.previews.ComponentPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.ProvideAppKitThemeComposition

@Composable
internal fun LoadingButton() {
    ProvideAppKitThemeComposition {
        StyledButton(style = ButtonStyle.ACCOUNT, size = ButtonSize.M, onClick = {}) {
            Box(
                modifier = Modifier.width(100.dp), contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(size = 16.dp, strokeWidth = 1.dp)
            }
        }
    }
}

@UiModePreview
@Composable
private fun LoadingButtonPreview() {
    ComponentPreview {
        LoadingButton()
    }
}
