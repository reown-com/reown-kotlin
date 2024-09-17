package com.reown.appkit.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.reown.appkit.ui.components.internal.commons.SelectNetworkIcon
import com.reown.appkit.ui.components.internal.commons.button.ButtonSize
import com.reown.appkit.ui.components.internal.commons.button.ButtonStyle
import com.reown.appkit.ui.components.internal.commons.button.ImageButton
import com.reown.appkit.ui.components.internal.commons.network.CircleNetworkImage
import com.reown.appkit.ui.theme.ProvideAppKitThemeComposition
import com.reown.appkit.utils.getImageData

@Composable
fun NetworkButton(
    state: AppKitState
) {
    val selectedChain by state.selectedChain.collectAsState(initial = null)
    val image: @Composable () -> Unit = selectedChain?.let { chain ->
        { CircleNetworkImage(data = chain.getImageData(), size = 24.dp) }
    } ?: {
        SelectNetworkIcon()
    }
    NetworkButton(
        text = selectedChain?.chainName ?: "Select Network",
        image = image,
        isEnabled = true,
        onClick = { state.openAppKit(true, selectedChain != null) }
    )
}

@Composable
internal fun NetworkButton(
    text: String,
    image: @Composable () -> Unit,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    ProvideAppKitThemeComposition {
        ImageButton(
            text = text,
            image = { image() },
            isEnabled = isEnabled,
            style = ButtonStyle.ACCOUNT,
            size = ButtonSize.ACCOUNT_M,
            onClick = onClick
        )
    }
}
