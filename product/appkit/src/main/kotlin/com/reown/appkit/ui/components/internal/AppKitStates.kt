package com.reown.appkit.ui.components.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reown.modal.ui.components.common.VerticalSpacer
import com.reown.appkit.ui.components.internal.commons.LoadingSpinner
import com.reown.appkit.ui.components.internal.commons.button.ButtonSize
import com.reown.appkit.ui.components.internal.commons.button.ButtonStyle
import com.reown.appkit.ui.components.internal.commons.button.TryAgainButton
import com.reown.appkit.ui.theme.AppKitTheme

@Composable
internal fun LoadingModalState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoadingSpinner()
    }
}

@Composable
internal fun ErrorModalState(retry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Something went wrong", style = AppKitTheme.typo.paragraph400)
        VerticalSpacer(height = 10.dp)
        TryAgainButton(
            size = ButtonSize.M,
            style = ButtonStyle.MAIN
        ) {
            retry()
        }
    }
}
