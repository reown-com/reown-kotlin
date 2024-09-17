package com.reown.appkit.ui.components.internal

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.AppKitTheme
import com.reown.appkit.ui.components.internal.commons.BackArrowIcon
import com.reown.appkit.ui.components.internal.commons.CloseIcon
import com.reown.appkit.ui.components.internal.commons.QuestionMarkIcon
import com.reown.appkit.ui.components.internal.commons.TestTags
import com.reown.appkit.ui.previews.MultipleComponentsPreview

@Composable
internal fun AppKitTopBar(
    title: String,
    startIcon: @Composable () -> Unit,
    onCloseIconClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            startIcon()
        }
        Text(
            text = title,
            style = AppKitTheme.typo.paragraph600.copy(
                color = AppKitTheme.colors.foreground.color100,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.weight(1f).testTag(TestTags.TITLE)
        )
        CloseIcon(onClick = onCloseIconClick)
    }
}

@Composable
@UiModePreview
private fun PreviewAppKitTopBar() {
    MultipleComponentsPreview(
        { AppKitTopBar(title = "WalletConnect", startIcon = { BackArrowIcon {} }, {}) },
        { AppKitTopBar(title = "WalletConnect", startIcon = { QuestionMarkIcon() }, {}) }
    )
}
