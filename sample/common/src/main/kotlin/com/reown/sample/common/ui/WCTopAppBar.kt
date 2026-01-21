package com.reown.sample.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.sample.common.R
import com.reown.sample.common.ui.theme.PreviewTheme
import com.reown.sample.common.ui.theme.UiModePreview


@Composable
fun WCTopAppBar(
    titleText: String,
    vararg actionImages: TopBarActionImage,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 45.dp, end = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = titleText, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight(700)))
        Spacer(modifier = Modifier.weight(1f))
        actionImages.forEachIndexed() { index, actionImage ->
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        onClick = actionImage.onClick
                    )
                    .padding(12.dp),
                painter = painterResource(id = actionImage.resource),
                contentDescription = null,
            )
        }
    }
}

data class TopBarActionImage(
    @DrawableRes val resource: Int,
    val onClick: () -> Unit,
)

@Composable
@UiModePreview
fun WCTopAppBarPreview() {
    PreviewTheme {
        WCTopAppBar(titleText = "Title", TopBarActionImage(R.drawable.ic_ethereum, {}))
    }
}