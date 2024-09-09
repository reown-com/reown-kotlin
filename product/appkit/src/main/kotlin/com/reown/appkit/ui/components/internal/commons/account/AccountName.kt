package com.reown.appkit.ui.components.internal.commons.account

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.reown.modal.ui.components.common.roundedClickable
import com.reown.appkit.domain.model.AccountData
import com.reown.appkit.domain.model.Identity
import com.reown.appkit.ui.components.internal.commons.CopyIcon
import com.reown.appkit.ui.previews.ComponentPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.previews.accountDataPreview
import com.reown.appkit.ui.theme.AppKitTheme
import com.reown.appkit.utils.toVisibleAddress

@Composable
internal fun AccountName(accountData: AccountData) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        val name = accountData.identity?.name ?: accountData.address.toVisibleAddress()
        Text(text = name, style = AppKitTheme.typo.mediumTitle600)
        CopyIcon(
            modifier = Modifier
                .padding(10.dp)
                .size(16.dp)
                .roundedClickable { clipboardManager.setText(AnnotatedString(accountData.address)) }
        )
    }
}

@UiModePreview
@Composable
private fun AccountAddressPreview() {
    ComponentPreview {
        AccountName(accountDataPreview)
    }
}

@UiModePreview
@Composable
private fun AccountNamePreview() {
    ComponentPreview {
        AccountName(accountDataPreview.copy(identity = Identity(name = "testIdentity.eth")))
    }
}
