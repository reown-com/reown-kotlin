package com.walletconnect.sample.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.R
import com.walletconnect.sample.pos.ui.theme.WCBorderRadius
import com.walletconnect.sample.pos.ui.theme.WCTheme

private val SearchShape = RoundedCornerShape(WCBorderRadius.radius4)

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 35,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(SearchShape)
            .background(WCTheme.colors.foregroundPrimary)
            .padding(horizontal = WCTheme.spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = WCTheme.colors.iconDefault,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(WCTheme.spacing.spacing2))
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    if (new.length <= maxLength) onValueChange(new)
                },
                singleLine = true,
                cursorBrush = SolidColor(WCTheme.colors.textPrimary),
                textStyle = LocalTextStyle.current.copy(
                    color = WCTheme.colors.textPrimary,
                    fontFamily = WCTheme.typography.bodyLgRegular.fontFamily,
                    fontSize = WCTheme.typography.bodyLgRegular.fontSize,
                    fontWeight = WCTheme.typography.bodyLgRegular.fontWeight,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = WCTheme.typography.bodyLgRegular,
                    color = WCTheme.colors.textSecondary,
                )
            }
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = WCTheme.colors.iconDefault,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
