package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.sample.wallet.ui.common.generated.ButtonWithLoader
import com.reown.sample.wallet.ui.common.generated.ButtonWithoutLoader


@Composable
fun Button(modifier: Modifier = Modifier, onClick: () -> Unit = {}, text: String, textColor: Color = Color(0xFF000000)) {
    ButtonWithoutLoader(
        buttonColor = Color(0x2A2A2A),
        modifier = Modifier
            .height(46.dp)
            .clickable { onClick() },
        content = {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 16.0.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                ),
                modifier = modifier.wrapContentHeight(align = Alignment.CenterVertically).wrapContentWidth(align = Alignment.CenterHorizontally)
            )
        }
    )
}

@Composable
fun ButtonsVertical(
    allowButtonColor: Color,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
    isLoadingConfirm: Boolean,
    isLoadingCancel: Boolean
) {
    Column(modifier = modifier) {
        ButtonWithLoader(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF732BCC), Color(0xFF076CF1)),
                start = Offset(0f, 0f),
                end = Offset(1000f, 0f)
            ),
            buttonColor = Color(0xFFFFFFFF),
            loaderColor = Color(0xFFFFFFFF),
            modifier = Modifier
                .padding(8.dp)
                .height(60.dp)
                .clickable { onConfirm() },
            isLoading = isLoadingConfirm,
            content = {
                Text(
                    text = "Confirm",
                    style = TextStyle(
                        fontSize = 20.0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                    ),
                    modifier = modifier.wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
        )
        ButtonWithLoader(
            buttonColor = Color(0xFF363636),
            loaderColor = Color(0xFFFFFFFF),
            modifier = Modifier
                .padding(8.dp)
                .height(60.dp)
                .clickable { onCancel() },
            isLoading = isLoadingCancel,
            content = {
                Text(
                    text = "Cancel",
                    style = TextStyle(
                        fontSize = 20.0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFFFF),
                    ),
                    modifier = modifier.wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun Buttons(
    allowButtonColor: Color,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
    isLoadingConfirm: Boolean,
    isLoadingCancel: Boolean
) {
    Row(modifier = modifier) {
        Spacer(modifier = Modifier.width(18.dp))
        ButtonWithLoader(
            buttonColor = Color(0xFFD6D6D6),
            loaderColor = Color(0xFF000000),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clickable { onCancel() },
            isLoading = isLoadingCancel,
            content = {
                Text(
                    text = "Cancel",
                    style = TextStyle(
                        fontSize = 20.0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF000000),
                    ),
                    modifier = modifier.wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        ButtonWithLoader(
            buttonColor = allowButtonColor,
            loaderColor = Color(0xFFFFFFFF),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clickable { onConfirm() },
            isLoadingConfirm,
            content = {
                Text(
                    text = "Confirm",
                    style = TextStyle(
                        fontSize = 20.0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(
                            alpha = 255,
                            red = 255,
                            green = 255,
                            blue = 255
                        ),
                    ),
                    modifier = modifier.wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
        )
        Spacer(modifier = Modifier.width(20.dp))
    }
}